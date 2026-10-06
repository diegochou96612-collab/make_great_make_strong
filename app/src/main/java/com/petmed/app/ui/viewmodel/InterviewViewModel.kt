package com.petmed.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.petmed.app.data.database.AppDatabase
import com.petmed.app.data.model.CaseEvent
import com.petmed.app.data.model.CaseField
import com.petmed.app.data.model.CasePhoto
import com.petmed.app.data.model.DailyLog
import com.petmed.app.data.model.HealthCase
import com.petmed.app.data.model.Pet
import com.petmed.app.interview.AgentProtocol
import com.petmed.app.interview.AiAssist
import com.petmed.app.interview.AiKeyStore
import com.petmed.app.interview.InterviewBank
import com.petmed.app.interview.LogUtil
import com.petmed.app.interview.MULTI_SEP
import com.petmed.app.interview.PhotoStore
import java.io.File
import com.petmed.app.interview.QType
import com.petmed.app.interview.Question
import com.petmed.app.interview.RedFlag
import com.petmed.app.interview.RedFlagDetector
import com.petmed.app.interview.ReportBuilder
import com.petmed.app.interview.Species
import com.petmed.app.interview.Transcript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Msg {
    data class Bot(val text: String) : Msg()
    data class User(val text: String) : Msg()
    data class Alert(val flag: RedFlag) : Msg()
}

data class InterviewUi(
    val messages: List<Msg> = emptyList(),
    val current: Question? = null,
    val picked: Set<String> = emptySet(),
    val busy: Boolean = false,
    val finished: Boolean = false,
    val caseId: Long? = null,
    val answered: Int = 0,
    val total: Int = 0,
    val canFinishEarly: Boolean = false,
    val resumeOffer: ResumeOffer? = null,
    /** 已完成的紀錄重新開啟：只有自由對話（補充、更正），沒有固定題目。 */
    val chatMode: Boolean = false
)

/** 偵測到上次沒問完的紀錄時，詢問飼主要不要接續。 */
data class ResumeOffer(val caseId: Long, val title: String, val answered: Int, val updatedAt: Long)

/**
 * 問診流程＝「固定題庫當骨架」＋「自由對話」。
 *
 * - 題目、順序、結束條件由 App 控制（InterviewBank）。
 * - 飼主隨時可以自由說話：AI 會把一句話拆填到多個欄位（已回答的題目不再重問）、
 *   更正先前的說法、把想問獸醫的問題記下來。AI 不診斷、不建議用藥或產品。
 * - AI 不可用或輸出壞掉時，退回固定流程，飼主說的話不會遺失。
 * - 問診結束後（或重新開啟已完成的紀錄）仍可繼續對話補充、更正，報告與摘要會跟著更新。
 * - 緊急紅旗由 App 用確定性規則偵測，不依賴 AI。
 */
class InterviewViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.caseDao()

    private val _ui = MutableStateFlow(InterviewUi())
    val ui: StateFlow<InterviewUi> = _ui.asStateFlow()

    val cases: StateFlow<List<HealthCase>> = dao.observeAllCases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private enum class Stage { PET_PICK, SPECIES_PICK, BANK }

    // 啟動前的特殊題（不屬於題庫，也不存為欄位）
    private val petPickQ = { names: List<String> ->
        Question(
            id = "pet_pick", module = "INTRO", section = InterviewBank.SEC_COMPLAINT, label = "寵物",
            text = "這次是要記錄哪一隻？", type = QType.SINGLE, options = names, source = "App", scanRedFlag = false
        )
    }
    private val speciesPickQ = Question(
        id = "species_pick", module = "INTRO", section = InterviewBank.SEC_COMPLAINT, label = "物種",
        text = "牠是狗還是貓？", type = QType.SINGLE, options = listOf("狗", "貓", "其他"), source = "App", scanRedFlag = false
    )

    private var stage = Stage.BANK
    private var pets: List<Pet> = emptyList()
    private var pet: Pet? = null
    private var speciesText = ""
    private var species = Species.OTHER
    private var caseId: Long? = null
    private val answers = linkedMapOf<String, String>()
    private val states = mutableMapOf<String, String>()
    private val shownFlags = mutableSetOf<RedFlag>()
    private val pendingChat = mutableListOf<Pair<String, String>>()
    private var extraSeq = 0
    private var entered = false

    init { AiKeyStore.load(application) }

    /** 畫面第一次出現時呼叫一次：帶 openCaseId 代表要開啟既有紀錄（補充或更正）。 */
    fun enter(openCaseId: Long?) {
        if (entered) return
        entered = true
        if (openCaseId != null && openCaseId > 0) restore(openCaseId) else start()
    }

    private fun resetState() {
        answers.clear(); states.clear(); shownFlags.clear(); pendingChat.clear()
        caseId = null; pet = null; speciesText = ""; species = Species.OTHER; extraSeq = 0
    }

    fun start(allowOffer: Boolean = true) {
        resetState()
        _ui.value = InterviewUi(
            messages = listOf(
                Msg.Bot(
                    "你好，我是AI 記錄助理。我會陪你把牠的狀況整理成一份給獸醫看的紀錄。\n\n" +
                        "你可以照我的問題回答，也可以直接用自己的話說，想到什麼就說什麼，我會幫你整理進紀錄。" +
                        "我不會診斷，也不會建議用藥。如果牠現在有緊急狀況，請先聯絡獸醫或前往急診。"
                )
            ),
            busy = true
        )
        viewModelScope.launch {
            pets = withContext(Dispatchers.IO) { db.petDao().getAllOnce() }
            val unfinished = if (allowOffer) withContext(Dispatchers.IO) { dao.getLatestUnfinished() } else null
            if (unfinished != null) {
                val n = withContext(Dispatchers.IO) { dao.countAnswered(unfinished.id) }
                if (n > 0) {
                    _ui.value = _ui.value.copy(
                        busy = false,
                        resumeOffer = ResumeOffer(unfinished.id, unfinished.title, n, unfinished.updatedAt)
                    )
                    return@launch
                }
            }
            beginFlow()
        }
    }

    private fun beginFlow() {
        when {
            pets.size == 1 -> { pet = pets[0]; applyPetSpecies(pets[0].species); beginBank() }
            pets.size > 1 -> { stage = Stage.PET_PICK; ask(petPickQ(pets.map { it.name })) }
            else -> { stage = Stage.SPECIES_PICK; ask(speciesPickQ) }
        }
    }

    /** 不接續：舊紀錄標為「已放棄」（之後不再主動詢問，但仍可從選單接續），改開新的。 */
    fun declineResume() {
        val offer = _ui.value.resumeOffer ?: return
        _ui.value = _ui.value.copy(resumeOffer = null, busy = true)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.getCase(offer.caseId)?.let { dao.updateCase(it.copy(status = HealthCase.STATUS_ABANDONED)) }
            }
            beginFlow()
        }
    }

    fun resume(id: Long) = restore(id)

    /**
     * 還原一份紀錄與先前的對話。
     * - 還沒問完：從下一題繼續。
     * - 已完成：進入「補充與修改」對話模式。
     */
    private fun restore(id: Long) {
        _ui.value = InterviewUi(messages = listOf(Msg.Bot("正在載入紀錄……")), busy = true)
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val c = dao.getCase(id)
                Triple(c, if (c != null) dao.getFields(id) else emptyList(), if (c != null) dao.getEvents(id) else emptyList())
            }
            val c = loaded.first
            if (c == null) { start(allowOffer = false); return@launch }
            resetState()
            pets = withContext(Dispatchers.IO) { db.petDao().getAllOnce() }
            pet = pets.firstOrNull { it.id == c.petId }
            applyPetSpecies(pet?.species?.ifBlank { null } ?: c.species)
            loaded.second.filter { it.state == CaseField.STATE_ANSWERED || it.state == CaseField.STATE_UNKNOWN }.forEach {
                answers[it.fieldKey] = it.value
                states[it.fieldKey] = it.state
            }
            extraSeq = loaded.second.count { it.fieldKey.startsWith("note_") || it.fieldKey.startsWith("vet_q_") }
            caseId = id
            stage = Stage.BANK
            val done = c.status == HealthCase.STATUS_DONE
            if (!done) {
                withContext(Dispatchers.IO) { dao.updateCase(c.copy(status = HealthCase.STATUS_INTERVIEWING, updatedAt = System.currentTimeMillis())) }
            }

            val msgs = mutableListOf<Msg>(
                Msg.Bot(
                    if (done) "這份紀錄已經整理好了。你可以在這裡繼續補充、更正，也可以問我怎麼記錄。以下是先前的對話。"
                    else "接續上次沒問完的紀錄「${c.title.ifBlank { "（未命名）" }}」。以下是先前的對話。"
                )
            )
            loaded.third.forEach { e ->
                when (e.kind) {
                    CaseEvent.ANSWERED -> Transcript.parseAnswered(e.detail)?.let { t ->
                        if (InterviewBank.get(t.questionId) != null) { msgs += Msg.Bot(t.question); msgs += Msg.User(t.answer) }
                    }
                    CaseEvent.CHAT -> Transcript.parseChat(e.detail)?.let { msgs += if (it.fromUser) Msg.User(it.text) else Msg.Bot(it.text) }
                    CaseEvent.RED_FLAG -> RedFlag.values().firstOrNull { it.name == e.detail.substringBefore("｜") }?.let {
                        msgs += Msg.Alert(it); shownFlags += it
                    }
                }
            }
            if (done) {
                _ui.value = InterviewUi(
                    messages = msgs + Msg.Bot("想補充或更正什麼嗎？例如：「其實是兩天前才開始吐的」。"),
                    caseId = id, finished = true, chatMode = true
                )
            } else {
                _ui.value = InterviewUi(messages = msgs, caseId = id, busy = false)
                askNext()
            }
        }
    }

    private fun applyPetSpecies(text: String) {
        speciesText = text
        species = when {
            text.contains("狗") || text.contains("犬") -> Species.DOG
            text.contains("貓") -> Species.CAT
            else -> Species.OTHER
        }
    }

    private fun beginBank() {
        stage = Stage.BANK
        val name = pet?.name?.takeIf { it.isNotBlank() }
        if (name != null) append(Msg.Bot("好的，這次記錄「$name」的狀況。"))
        if (species == Species.OTHER) {
            append(Msg.Bot("提醒：目前的題目主要依據犬貓的資料整理，其他物種的題目還沒有經過查證，請以你的描述為主。"))
        }
        askNext()
    }

    private fun append(m: Msg) { _ui.value = _ui.value.copy(messages = _ui.value.messages + m) }

    private fun ask(q: Question) {
        _ui.value = _ui.value.copy(
            messages = _ui.value.messages + Msg.Bot(q.text),
            current = q, picked = emptySet(), busy = false
        )
    }

    private fun currentPlan() = InterviewBank.plan(answers, species)

    private fun refreshProgress() {
        val plan = currentPlan()
        _ui.value = _ui.value.copy(
            answered = answers.size,
            total = answers.size + plan.count { it.id !in answers },
            canFinishEarly = answers.containsKey("categories")
        )
    }

    private fun askNext() {
        val next = currentPlan().firstOrNull { it.id !in answers }
        if (next == null) { finish(); return }
        refreshProgress()
        ask(next)
    }

    // ───────── 使用者操作 ─────────

    fun toggle(option: String) {
        val q = _ui.value.current ?: return
        if (q.type != QType.MULTI) return
        val exclusive = q.options.lastOrNull { it.startsWith("都沒有") || it.startsWith("沒有") }
        val cur = _ui.value.picked
        val next = when {
            option in cur -> cur - option
            option == exclusive -> setOf(option)
            else -> (cur - (exclusive ?: "")) + option
        }
        _ui.value = _ui.value.copy(picked = next)
    }

    /** 單選題點選，或複選題按「確定」 */
    fun confirmChoice(option: String? = null) {
        val q = _ui.value.current ?: return
        val chosen: List<String> = if (q.type == QType.MULTI) {
            q.options.filter { it in _ui.value.picked }
        } else listOfNotNull(option)
        if (chosen.isEmpty()) return
        submit(q, chosen.joinToString(MULTI_SEP), CaseField.STATE_ANSWERED, chosen.joinToString(MULTI_SEP))
    }

    fun answerUnknown() {
        val q = _ui.value.current ?: return
        submit(q, "不確定", CaseField.STATE_UNKNOWN, "不確定")
    }

    /**
     * 飼主自己打字。AI 可用時走自由對話（一句話可填多個欄位、可更正、可記下想問獸醫的問題）；
     * AI 不可用或失敗時，退回固定流程，把這句話當成目前題目的答案。
     */
    fun answerText(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || _ui.value.busy) return
        val q = _ui.value.current
        if (q != null && (q.id == "pet_pick" || q.id == "species_pick")) { legacyAnswer(q, text); return }
        if (q == null && !_ui.value.finished) return
        if (AiAssist.available) { agentTurn(text, q); return }
        if (q != null) { legacyAnswer(q, text); return }
        // 已結束、但 AI 沒有設定
        append(Msg.User(text))
        append(Msg.Bot("AI 還沒有設定，所以沒辦法用對話補充。你可以到右上角「AI 設定」貼上金鑰，或在報告頁直接點欄位修改。"))
    }

    /** 固定流程的作法：文字題直接存；選擇題先本地比對，再請 AI 對應，最後存原文。 */
    private fun legacyAnswer(q: Question, text: String) {
        if (q.type == QType.TEXT || q.options.isEmpty()) {
            submit(q, text, CaseField.STATE_ANSWERED, text); return
        }
        val local = matchLocally(q, text)
        if (local != null) { submit(q, local.joinToString(MULTI_SEP), CaseField.STATE_ANSWERED, text); return }
        _ui.value = _ui.value.copy(busy = true, messages = _ui.value.messages + Msg.User(text))
        viewModelScope.launch {
            val mapped = withContext(Dispatchers.IO) { AiAssist.mapAnswer(q, text) }
            val value = when {
                mapped != null && mapped.matched.isNotEmpty() ->
                    (mapped.matched.joinToString(MULTI_SEP) +
                        if (mapped.other.isNotBlank()) "（補充：${mapped.other}）" else "")
                else -> text
            }
            submit(q, value, CaseField.STATE_ANSWERED, text, echo = false)
        }
    }

    private fun matchLocally(q: Question, text: String): List<String>? {
        val exact = q.options.firstOrNull { it == text }
        if (exact != null) return listOf(exact)
        if (q.type == QType.SINGLE) {
            val hits = q.options.filter { text.contains(it) }
            return if (hits.size == 1) hits else null
        }
        val hits = q.options.filter { text.contains(it) }
        return hits.ifEmpty { null }
    }

    fun finishEarly() {
        if (!_ui.value.canFinishEarly) return
        finish()
    }

    /** 問診中加入照片：存進這次狀況的相簿，並在對話中確認。 */
    fun addPhoto(file: File, note: String, topic: String) {
        val id = caseId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPhoto(CasePhoto(caseId = id, filePath = file.absolutePath, note = note.trim(), topic = topic))
            dao.insertEvent(CaseEvent(caseId = id, kind = CaseEvent.ANSWERED, detail = "加入照片｜$topic｜${note.trim()}"))
        }
        append(Msg.Bot("已把照片放進這次紀錄的相簿（$topic）。"))
    }

    // ───────── 自由對話 ─────────

    private fun petHeader(): String {
        val p = pet
        val name = p?.name?.takeIf { it.isNotBlank() } ?: "未提供"
        val sp = (p?.species?.takeIf { it.isNotBlank() } ?: speciesText).ifBlank { "未提供" }
        return "名字：$name，物種：$sp，品種：${p?.breed?.ifBlank { null } ?: "未提供"}，生日：${p?.birthDate?.ifBlank { null } ?: "未提供"}"
    }

    private fun recentHistory(): List<String> =
        _ui.value.messages.dropLast(1).filter { it is Msg.Bot || it is Msg.User }.takeLast(8).map {
            when (it) { is Msg.Bot -> "助理：${it.text}"; is Msg.User -> "飼主：${it.text}"; else -> "" }
        }

    private fun logChat(id: Long?, role: String, text: String) {
        if (id == null) { pendingChat += role to text; return }
        viewModelScope.launch(Dispatchers.IO) { dao.insertEvent(CaseEvent(caseId = id, kind = CaseEvent.CHAT, detail = "$role｜$text")) }
    }

    /** 自由對話的一個回合。pending 為 null 代表問診已結束（補充與修改）。 */
    private fun agentTurn(text: String, pending: Question?) {
        _ui.value = _ui.value.copy(busy = true, messages = _ui.value.messages + Msg.User(text))
        viewModelScope.launch {
            val catalog = AgentProtocol.catalogFor(species)
            val recorded = answers.entries.mapNotNull { (k, v) -> InterviewBank.get(k)?.let { it.label to v } }
            val history = recentHistory()
            val result = withContext(Dispatchers.IO) { AiAssist.agent(petHeader(), pending, recorded, catalog, history, text) }

            // 1) 緊急偵測：確定性規則，永遠執行，不依賴 AI
            val flags = linkedSetOf<RedFlag>().apply { addAll(RedFlagDetector.detect(text)) }

            if (result == null) {
                // AI 失敗 → 退回固定流程，確保這句話不會遺失
                append(Msg.Bot("（AI 暫時沒辦法對話：${AiAssist.lastError ?: "原因不明"}）"))
                if (flags.isNotEmpty() || pending != null) ensureCase(text)
                recordFlags(caseId, flags, "對話")
                if (pending != null) {
                    submitAfterEcho(pending, text)
                } else {
                    append(Msg.Bot("這句話這次沒有被記錄。你可以到報告頁直接修改欄位，或到右上角「AI 設定」檢查金鑰。"))
                    _ui.value = _ui.value.copy(busy = false)
                }
                return@launch
            }

            // 2) AI 沒有挑出任何可記錄的內容，但目前在問文字題、且不像在發問 → 當作這題的答案，避免遺失
            var updates = result.updates
            val nothing = updates.isEmpty() && result.notes.isEmpty() && result.vetQuestions.isEmpty()
            if (nothing && pending != null && pending.type == QType.TEXT && !AgentProtocol.looksLikeQuestion(text)) {
                updates = listOf(AgentProtocol.Update(pending, text))
            }

            val needCase = updates.isNotEmpty() || result.notes.isNotEmpty() || result.vetQuestions.isNotEmpty() || flags.isNotEmpty()
            val titleSource = updates.firstOrNull { it.question.id == "complaint" }?.value ?: text
            val id = if (needCase) ensureCase(titleSource) else caseId

            // 3) 寫入欄位（新增或更正）
            val changes = mutableListOf<Triple<String, String?, String>>()
            if (id != null) {
                withContext(Dispatchers.IO) {
                    for (u in updates) {
                        val q = u.question
                        val old = answers[q.id]
                        if (old == u.value) continue
                        val isEdit = old != null
                        dao.upsertField(
                            CaseField(
                                caseId = id, fieldKey = q.id, section = q.section, label = q.label, value = u.value,
                                state = CaseField.STATE_ANSWERED,
                                origin = if (isEdit) CaseField.ORIGIN_EDITED else CaseField.ORIGIN_OWNER,
                                sourceRef = "對話（AI 整理）：" + if (q.sourced) q.source else "待獸醫審閱：${q.source}",
                                sortOrder = InterviewBank.sortOrderOf(q)
                            )
                        )
                        dao.insertEvent(
                            if (isEdit) CaseEvent(caseId = id, kind = CaseEvent.EDITED, detail = "${q.label}｜$old → ${u.value}")
                            else CaseEvent(caseId = id, kind = CaseEvent.FILLED, detail = "${q.id}｜${q.label}｜${u.value}｜來源：對話")
                        )
                        answers[q.id] = u.value
                        states[q.id] = CaseField.STATE_ANSWERED
                        changes += Triple(q.label, old, u.value)
                        q.flagOptions.forEach { (opt, flag) -> if (u.value.split(MULTI_SEP).contains(opt)) flags += flag }
                    }
                    result.notes.forEach { n -> saveExtra(id, "note_", "飼主補充", n) }
                    result.vetQuestions.forEach { n -> saveExtra(id, "vet_q_", "想問獸醫的問題", n) }
                    dao.getCase(id)?.let { dao.updateCase(it.copy(updatedAt = System.currentTimeMillis())) }
                }
            }
            recordFlags(id, flags, "對話")

            // 4) 回覆（AI 的簡短回應）＋ App 產生的「記了什麼」確認
            append(Msg.Bot(result.reply))
            AgentProtocol.confirmation(changes, result.notes.size, result.vetQuestions.size)?.let { append(Msg.Bot(it)) }
            logChat(id, "U", text)
            logChat(id, "B", result.reply)

            val changed = changes.isNotEmpty() || result.notes.isNotEmpty() || result.vetQuestions.isNotEmpty()
            if (pending != null) {
                // 5a) 問診進行中：問下一個還沒回答的題目
                val next = currentPlan().firstOrNull { it.id !in answers }
                if (next == null) { finish(); return@launch }
                refreshProgress()
                if (next.id == pending.id) {
                    append(Msg.Bot("回到剛才的問題：${pending.text}"))
                    _ui.value = _ui.value.copy(busy = false, current = pending, picked = emptySet())
                } else ask(next)
            } else {
                // 5b) 問診已結束：更新報告與摘要
                _ui.value = _ui.value.copy(busy = false)
                if (changed && id != null) refreshNarrative(id)
            }
        }
    }

    private suspend fun saveExtra(id: Long, prefix: String, label: String, text: String) {
        val seq = extraSeq++
        dao.upsertField(
            CaseField(
                caseId = id, fieldKey = "$prefix${System.currentTimeMillis()}_$seq", section = InterviewBank.SEC_OWNER,
                label = label, value = text, state = CaseField.STATE_ANSWERED, origin = CaseField.ORIGIN_OWNER,
                sourceRef = "對話（AI 整理）", sortOrder = 6500 + seq
            )
        )
    }

    /** 緊急提示：同一種提示在同一次對話只顯示一次。 */
    private suspend fun recordFlags(id: Long?, flags: Set<RedFlag>, via: String) {
        for (flag in flags) {
            if (!shownFlags.add(flag)) continue
            if (id != null) {
                withContext(Dispatchers.IO) {
                    dao.insertEvent(CaseEvent(caseId = id, kind = CaseEvent.RED_FLAG, detail = "${flag.name}｜$via｜${flag.source}"))
                }
            }
            append(Msg.Alert(flag))
        }
    }

    /** AI 失敗後的退路：把飼主的話當成目前題目的答案（訊息已經顯示過，不重複顯示）。 */
    private fun submitAfterEcho(q: Question, text: String) {
        val v = if (q.type != QType.TEXT && q.options.isNotEmpty()) (matchLocally(q, text)?.joinToString(MULTI_SEP) ?: text) else text
        submit(q, v, CaseField.STATE_ANSWERED, text, echo = false)
    }

    /** 已完成的紀錄被更新後，重新整理摘要（失敗不影響已存的內容）。 */
    private fun refreshNarrative(id: Long) {
        viewModelScope.launch {
            append(Msg.Bot("報告已更新，我再重新整理一次摘要……"))
            _ui.value = _ui.value.copy(busy = true)
            val narrative = generateNarrative(id)
            withContext(Dispatchers.IO) {
                dao.getCase(id)?.let { dao.updateCase(it.copy(narrative = narrative ?: "", updatedAt = System.currentTimeMillis())) }
                dao.insertEvent(CaseEvent(caseId = id, kind = CaseEvent.AI_SUMMARY, detail = if (narrative != null) "對話更新後重新產生摘要" else "對話更新後未產生摘要"))
            }
            append(Msg.Bot(
                if (narrative != null) "摘要也更新好了。可以按下面的按鈕查看報告。"
                else "摘要這次沒有重新整理：${AiAssist.lastError ?: "原因不明"}。內容都已存好，可以到報告頁再試一次。"
            ))
            _ui.value = _ui.value.copy(busy = false)
        }
    }

    private suspend fun generateNarrative(id: Long): String? = withContext(Dispatchers.IO) {
        val c = dao.getCase(id) ?: return@withContext null
        val fields = dao.getFields(id)
        val header = "寵物：${c.petName.ifBlank { "未提供" }}，物種：${c.species.ifBlank { "未提供" }}，品種：${c.breed.ifBlank { "未提供" }}"
        AiAssist.narrative(header, fields.filter { it.state == CaseField.STATE_ANSWERED }.map { it.label to it.value })
    }

    // ───────── 固定流程內部 ─────────

    private fun submit(q: Question, value: String, state: String, display: String, echo: Boolean = true) {
        if (echo) append(Msg.User(display))
        // 啟動前的特殊題
        when (q.id) {
            "pet_pick" -> {
                pet = pets.firstOrNull { it.name == value }
                applyPetSpecies(pet?.species ?: "")
                beginBank(); return
            }
            "species_pick" -> { applyPetSpecies(value); beginBank(); return }
        }
        viewModelScope.launch {
            val id = ensureCase(value)
            val old = answers[q.id]
            answers[q.id] = value
            states[q.id] = state
            withContext(Dispatchers.IO) {
                dao.upsertField(
                    CaseField(
                        caseId = id, fieldKey = q.id, section = q.section, label = q.label, value = value,
                        state = state, origin = if (old != null) CaseField.ORIGIN_EDITED else CaseField.ORIGIN_OWNER,
                        sourceRef = if (q.sourced) q.source else "待獸醫審閱：${q.source}",
                        sortOrder = InterviewBank.sortOrderOf(q)
                    )
                )
                dao.insertEvent(CaseEvent(caseId = id, kind = CaseEvent.ANSWERED, detail = "${q.id}｜${q.text}｜$value｜出處：${q.source}"))
                dao.updateCase(dao.getCase(id)!!.copy(updatedAt = System.currentTimeMillis()))
            }
            checkRedFlags(id, q, value, state)
            askNext()
        }
    }

    private suspend fun ensureCase(firstValue: String): Long {
        caseId?.let { return it }
        val p = pet
        val species = p?.species?.ifBlank { speciesText } ?: speciesText
        val c = HealthCase(
            petId = p?.id ?: 0, petName = p?.name ?: "", species = species,
            breed = p?.breed ?: "", birthDate = p?.birthDate ?: "",
            title = firstValue.take(20) + if (firstValue.length > 20) "…" else ""
        )
        val id = withContext(Dispatchers.IO) { dao.insertCase(c) }
        caseId = id
        _ui.value = _ui.value.copy(caseId = id)
        // 案例建立前的對話，補寫進紀錄
        if (pendingChat.isNotEmpty()) {
            val lines = pendingChat.toList(); pendingChat.clear()
            withContext(Dispatchers.IO) {
                lines.forEach { (r, t) -> dao.insertEvent(CaseEvent(caseId = id, kind = CaseEvent.CHAT, detail = "$r｜$t")) }
            }
        }
        return id
    }

    private suspend fun checkRedFlags(id: Long, q: Question, value: String, state: String) {
        if (state != CaseField.STATE_ANSWERED) return
        val found = linkedSetOf<RedFlag>()
        if (q.scanRedFlag && (q.type == QType.TEXT || q.options.none { it == value })) {
            found += RedFlagDetector.detect(value)
        }
        value.split(MULTI_SEP).forEach { opt -> q.flagOptions[opt.trim()]?.let { found += it } }
        recordFlags(id, found, q.id)
    }

    private fun finish() {
        val id = caseId
        _ui.value = _ui.value.copy(busy = true, current = null)
        viewModelScope.launch {
            if (id == null) { _ui.value = _ui.value.copy(busy = false); return@launch }
            val plan = currentPlan()
            withContext(Dispatchers.IO) {
                // 沒回答的題目也記錄為「未提供」，報告上看得出缺什麼
                plan.filter { it.id !in answers && it.id != "categories" && it.id != "complaint" }.forEach { q ->
                    dao.upsertField(
                        CaseField(
                            caseId = id, fieldKey = q.id, section = q.section, label = q.label, value = "",
                            state = CaseField.STATE_NOT_PROVIDED, origin = CaseField.ORIGIN_AUTO,
                            sourceRef = q.source, sortOrder = InterviewBank.sortOrderOf(q)
                        )
                    )
                }
            }
            append(Msg.Bot("好的，資料我都記下來了。我正在整理成一份摘要……"))
            val narrative = generateNarrative(id)
            withContext(Dispatchers.IO) {
                dao.getCase(id)?.let { dao.updateCase(it.copy(status = HealthCase.STATUS_DONE, narrative = narrative ?: "", updatedAt = System.currentTimeMillis())) }
                dao.insertEvent(CaseEvent(caseId = id, kind = CaseEvent.AI_SUMMARY, detail = if (narrative != null) "已產生摘要" else "未產生摘要（網路或內容檢查未通過）"))
            }
            if (narrative == null) {
                append(Msg.Bot("這次沒有產生 AI 摘要：${AiAssist.lastError ?: "原因不明"}\n其他內容都已經存好，之後可以在報告頁再試一次。"))
            }
            append(Msg.Bot("整理好了。你可以檢視、修改每一項內容，也可以匯出 PDF 給獸醫。\n如果還想補充或更正，直接在下面跟我說就好。"))
            _ui.value = _ui.value.copy(finished = true, busy = false, caseId = id, chatMode = true)
        }
    }
}

// ───────── 報告頁 ─────────

class CaseReportViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).caseDao()

    fun observeCase(id: Long) = dao.observeCase(id)
    fun observeFields(id: Long) = dao.observeFields(id)

    private val _events = MutableStateFlow<List<CaseEvent>>(emptyList())
    val events: StateFlow<List<CaseEvent>> = _events.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun loadEvents(id: Long) {
        viewModelScope.launch { _events.value = withContext(Dispatchers.IO) { dao.getEvents(id) } }
    }

    /** 修改欄位：標記為「飼主修改」，留下修改前後紀錄，並清除（已過期的）AI 摘要。 */
    fun editField(field: CaseField, newValue: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val value = newValue.trim()
                dao.upsertField(
                    field.copy(
                        value = value,
                        state = if (value.isBlank()) CaseField.STATE_NOT_PROVIDED else CaseField.STATE_ANSWERED,
                        origin = CaseField.ORIGIN_EDITED, updatedAt = System.currentTimeMillis()
                    )
                )
                dao.insertEvent(CaseEvent(caseId = field.caseId, kind = CaseEvent.EDITED, detail = "${field.label}｜${ReportBuilder.displayValue(field)} → ${value.ifBlank { "未提供" }}"))
                dao.getCase(field.caseId)?.let { dao.updateCase(it.copy(narrative = "", updatedAt = System.currentTimeMillis())) }
            }
            loadEvents(field.caseId)
        }
    }

    fun regenerateNarrative(id: Long) {
        viewModelScope.launch {
            _busy.value = true
            val result = withContext(Dispatchers.IO) {
                val c = dao.getCase(id) ?: return@withContext null
                val fields = dao.getFields(id)
                val header = "寵物：${c.petName.ifBlank { "未提供" }}，物種：${c.species.ifBlank { "未提供" }}，品種：${c.breed.ifBlank { "未提供" }}"
                val narrative = AiAssist.narrative(header, fields.filter { it.state == CaseField.STATE_ANSWERED }.map { it.label to it.value })
                dao.updateCase(c.copy(narrative = narrative ?: "", updatedAt = System.currentTimeMillis()))
                dao.insertEvent(CaseEvent(caseId = id, kind = CaseEvent.AI_SUMMARY, detail = if (narrative != null) "重新產生摘要" else "重新產生失敗"))
                narrative
            }
            _busy.value = false
            loadEvents(id)
            lastMessage.value = if (result == null) "這次沒有產生摘要：${AiAssist.lastError ?: "原因不明"}" else null
        }
    }

    val lastMessage = MutableStateFlow<String?>(null)

    init { AiKeyStore.load(application) }

    fun recordExport(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertEvent(CaseEvent(caseId = id, kind = CaseEvent.EXPORTED, detail = "匯出 PDF"))
        }
    }

    suspend fun snapshot(id: Long): Triple<HealthCase, List<CaseField>, List<CaseEvent>>? = withContext(Dispatchers.IO) {
        val c = dao.getCase(id) ?: return@withContext null
        Triple(c, dao.getFields(id), dao.getEvents(id))
    }

    // ───── 每日紀錄 ─────

    fun observeLogs(id: Long) = dao.observeLogs(id)

    /** 同一天只留一筆（unique index + REPLACE）。內容全空就當作刪除。 */
    fun saveLog(log: DailyLog) {
        viewModelScope.launch(Dispatchers.IO) {
            if (LogUtil.isEmpty(log)) {
                dao.getLogs(log.caseId).firstOrNull { it.date == log.date }?.let { dao.deleteLog(it.id) }
            } else {
                val existing = dao.getLogs(log.caseId).firstOrNull { it.date == log.date }
                dao.upsertLog(log.copy(id = existing?.id ?: log.id, updatedAt = System.currentTimeMillis()))
            }
            dao.getCase(log.caseId)?.let { dao.updateCase(it.copy(updatedAt = System.currentTimeMillis())) }
        }
    }

    fun deleteLog(log: DailyLog) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteLog(log.id) }
    }

    // ───── 照片 ─────

    fun observePhotos(id: Long) = dao.observePhotos(id)

    fun addPhoto(caseId: Long, file: File, note: String, topic: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPhoto(CasePhoto(caseId = caseId, filePath = file.absolutePath, note = note.trim(), topic = topic))
            dao.insertEvent(CaseEvent(caseId = caseId, kind = CaseEvent.ANSWERED, detail = "加入照片｜$topic｜${note.trim()}"))
            dao.getCase(caseId)?.let { dao.updateCase(it.copy(updatedAt = System.currentTimeMillis())) }
        }
    }

    fun updatePhoto(photo: CasePhoto, note: String, topic: String) {
        viewModelScope.launch(Dispatchers.IO) { dao.updatePhoto(photo.copy(note = note.trim(), topic = topic)) }
    }

    fun deletePhoto(photo: CasePhoto) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePhoto(photo.id)
            PhotoStore.deleteFile(photo.filePath)
        }
    }

    suspend fun exportData(id: Long): ExportData? = withContext(Dispatchers.IO) {
        val c = dao.getCase(id) ?: return@withContext null
        ExportData(c, dao.getFields(id), dao.getEvents(id), dao.getLogs(id), dao.getPhotos(id))
    }

    fun delete(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteFields(id); dao.deleteEvents(id); dao.deleteLogs(id); dao.deletePhotos(id)
                dao.deleteCase(id)
                PhotoStore.deleteAlbum(getApplication(), id)
            }
            onDone()
        }
    }
}

data class ExportData(
    val case: HealthCase,
    val fields: List<CaseField>,
    val events: List<CaseEvent>,
    val logs: List<DailyLog>,
    val photos: List<CasePhoto>
)
