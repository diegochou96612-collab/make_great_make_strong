package com.petmed.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.petmed.app.data.database.AppDatabase
import com.petmed.app.data.model.Pet
import com.petmed.app.data.model.VaccineRecord
import com.petmed.app.interview.AiAssist
import com.petmed.app.interview.DateUtil
import com.petmed.app.interview.DoseRecord
import com.petmed.app.interview.GuideSpecies
import com.petmed.app.interview.GuideStatus
import com.petmed.app.interview.VaccineGuide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HandbookChatLine(val question: String, val answer: String)

@OptIn(ExperimentalCoroutinesApi::class)
class VaccineViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val vaccineDao = db.vaccineDao()
    private val petDao = db.petDao()

    val pets: StateFlow<List<Pet>> = petDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val selectedId = MutableStateFlow<Int?>(null)

    /** 目前選的寵物；沒選過就用第一隻 */
    val selectedPet: StateFlow<Pet?> = combine(pets, selectedId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val records: StateFlow<List<VaccineRecord>> = selectedPet
        .flatMapLatest { p -> if (p == null) flowOf(emptyList()) else vaccineDao.observeByPet(p.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 種類欄位不是狗或貓時，讓飼主手動指定（只存在這次使用中，不改寵物資料） */
    private val speciesPick = MutableStateFlow<Map<Int, GuideSpecies>>(emptyMap())

    val species: StateFlow<GuideSpecies?> = combine(selectedPet, speciesPick) { p, picks ->
        p?.let { VaccineGuide.speciesOf(it.species) ?: picks[it.id] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectPet(id: Int) { selectedId.value = id }

    fun pickSpecies(petId: Int, s: GuideSpecies) { speciesPick.value = speciesPick.value + (petId to s) }

    fun addRecord(petId: Int, itemKey: String, date: String, note: String = "") {
        viewModelScope.launch { vaccineDao.insert(VaccineRecord(petId = petId, itemKey = itemKey, doneDate = date, note = note)) }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch { vaccineDao.deleteById(id) }
    }

    // ───── 首頁提示（第一隻寵物） ─────
    val homeNotice: StateFlow<String?> = petDao.getFirstPet()
        .flatMapLatest { pet ->
            if (pet == null) flowOf<String?>(null)
            else vaccineDao.observeByPet(pet.id).map { recs -> noticeFor(pet, recs) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun noticeFor(pet: Pet, recs: List<VaccineRecord>): String? {
        val sp = VaccineGuide.speciesOf(pet.species) ?: return null
        val birth = DateUtil.parse(pet.birthDate) ?: return null
        val today = DateUtil.today()
        val states = VaccineGuide.evaluateAll(sp, birth, today, recs.toDoseRecords())
        val list = VaccineGuide.attention(states)
        if (list.isEmpty()) return "${pet.name}：目前沒有需要安排的疫苗或檢測"
        val first = list.first()
        val verb = if (first.status == GuideStatus.DUE) "可以安排" else "快到了"
        val more = if (list.size > 1) "（還有 ${list.size - 1} 項）" else ""
        return "${pet.name}：${first.item.title}$verb$more"
    }

    // ───── 問 AI ─────
    private val _chat = MutableStateFlow<List<HandbookChatLine>>(emptyList())
    val chat: StateFlow<List<HandbookChatLine>> = _chat
    val busy = MutableStateFlow(false)
    val chatError = MutableStateFlow<String?>(null)

    fun clearChat() { _chat.value = emptyList(); chatError.value = null }

    fun ask(question: String) {
        val q = question.trim()
        val pet = selectedPet.value ?: return
        val sp = species.value ?: return
        if (q.isEmpty() || busy.value) return
        busy.value = true
        chatError.value = null
        val birth = DateUtil.parse(pet.birthDate)
        val age = birth?.let { VaccineGuide.ageText(it, DateUtil.today()) }
        val recText = records.value.map { r ->
            (VaccineGuide.items.firstOrNull { it.key == r.itemKey }?.title ?: r.itemKey) to r.doneDate
        }
        val header = VaccineGuide.petHeader(pet.name, sp, age, recText)
        val knowledge = VaccineGuide.knowledgeText(sp)
        val history = _chat.value.map { it.question to it.answer }
        viewModelScope.launch {
            val answer = withContext(Dispatchers.IO) { AiAssist.handbookAnswer(header, knowledge, history, q) }
            if (answer == null) {
                chatError.value = AiAssist.lastError ?: "AI 暫時無法回答。手冊上的內容仍然可以看。"
            } else {
                _chat.value = _chat.value + HandbookChatLine(q, answer)
            }
            busy.value = false
        }
    }
}

fun List<VaccineRecord>.toDoseRecords(): List<DoseRecord> =
    mapNotNull { r -> DateUtil.parse(r.doneDate)?.let { DoseRecord(r.itemKey, it) } }
