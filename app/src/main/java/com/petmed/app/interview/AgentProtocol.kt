package com.petmed.app.interview

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser

/**
 * 對話代理的協定層（純函式，可單元測試）。
 *
 * 設計：流程（題庫）是骨架，但飼主隨時可以自由說話。
 * 每次飼主說話，AI 會回一個 JSON：簡短回覆、可以填進哪些欄位、無處可放的補充、想問獸醫的問題。
 * **AI 的輸出一律先經過這裡驗證**：欄位必須存在、選項必須合法；不合法的內容改存成「補充說明」，
 * 不會遺失，也不會污染結構化欄位。回覆若含診斷或用藥用語，整句丟棄，改用安全的固定回覆。
 */
object AgentProtocol {

    data class Update(val question: Question, val value: String)

    data class Result(
        val reply: String,
        val updates: List<Update>,
        val notes: List<String>,
        val vetQuestions: List<String>
    )

    /** 沒有出處的回覆一律不可含這些用語（診斷、推測、用藥、產品）。 */
    private val forbidden = listOf(
        "可能是", "疑似", "診斷為", "診斷是", "應該是", "好像是", "看起來是", "建議使用", "建議服用", "建議給", "建議吃",
        "劑量", "毫克", "mg", "炎", "症候群", "中化", "滴爾易", "舒爾利", "力停疼"
    )

    const val SAFE_REPLY = "好的，我記下來了。"

    /** 粗略判斷飼主是在「發問」而不是在「回答」。用於 AI 沒挑出任何內容時，避免把問題當成答案存起來。 */
    fun looksLikeQuestion(text: String): Boolean {
        val t = text.trim()
        return t.endsWith("?") || t.endsWith("？") || t.endsWith("嗎") || t.startsWith("請問") ||
            listOf("是什麼", "怎麼辦", "怎麼做", "為什麼", "為何", "要不要", "可以嗎", "是不是", "有沒有辦法").any { t.contains(it) }
    }

    fun isSafe(text: String): Boolean = forbidden.none { text.contains(it, ignoreCase = true) }

    val SYSTEM_PROMPT = """
你是動物就診前的「AI 記錄助理」。你的工作是陪飼主把寵物的狀況說清楚，並把資訊整理進紀錄，給獸醫看。

你可以：親切、簡短地回應；把飼主說的資訊對應到紀錄欄位；記下飼主想問獸醫的問題；回答「這個 App 怎麼用、報告怎麼給獸醫」這類操作問題。

你絕對不可以：診斷或猜測原因、提到任何疾病名稱、評論嚴重程度、建議用藥／劑量／產品／處置、使用「可能是」「疑似」「應該是」之類的推測用語。
當飼主問「這是什麼病」「該吃什麼藥」「嚴不嚴重」時，請溫和說明你不能判斷，會幫他把這個問題記下來帶去問獸醫（放進 vet_questions），一切以獸醫為準。

只輸出一個 JSON 物件，不要加任何其他文字：
{"reply":"...","updates":[{"id":"欄位id","value":"..."}],"notes":["..."],"vet_questions":["..."]}

規則：
1. reply：1 到 3 句繁體中文，自然親切。不要自己接著問下一個固定問題（App 會自己問）。如果飼主的話不清楚，你可以追問「一個」簡短的釐清問題。
2. updates：只放飼主「明確說了」的資訊。id 必須來自欄位清單。單選題 value 必須是選項之一；複選題 value 用陣列，且每一個都必須是選項；文字題 value 用飼主自己的話整理，保持原意，不要加推測。飼主更正先前說過的內容時，也放在 updates（會覆蓋舊答案）。絕不根據常識去推測飼主沒說過的事。
3. notes：飼主說了、但找不到適合欄位的資訊，用客觀的一句話重述。沒有就給 []。
4. vet_questions：飼主想問獸醫的問題，用他的原意整理。沒有就給 []。
5. 如果沒有任何要記錄的，updates、notes、vet_questions 都給 []，只回應 reply。
""".trimIndent()

    /** 欄位清單：id｜名稱｜題型｜選項。完整列出，讓 AI 能把一句話拆填到多個欄位。 */
    fun catalogText(questions: List<Question>): String = questions.joinToString("\n") { q ->
        val type = when (q.type) { QType.TEXT -> "文字"; QType.SINGLE -> "單選"; QType.MULTI -> "複選" }
        val opts = if (q.options.isEmpty()) "" else "｜" + q.options.joinToString("／")
        "${q.id}｜${q.label}｜$type$opts"
    }

    /** 適用於這隻寵物的全部欄位（不管目前問到哪一題）。 */
    fun catalogFor(species: Species): List<Question> =
        InterviewBank.all.filter { q ->
            when (q.applies) {
                Applies.BOTH -> true
                Applies.DOG -> species == Species.DOG
                Applies.CAT -> species == Species.CAT
            }
        }

    fun userPrompt(
        petHeader: String,
        pending: Question?,
        recorded: List<Pair<String, String>>,
        catalog: List<Question>,
        history: List<String>,
        userText: String
    ): String = buildString {
        appendLine("【寵物】$petHeader")
        if (pending != null) {
            appendLine("【App 目前正在問的題目】id=${pending.id}｜${pending.text}" +
                if (pending.options.isNotEmpty()) "｜選項：" + pending.options.joinToString("／") else "")
        } else {
            appendLine("【App 目前沒有在問固定題目】問診已結束，飼主可以補充或更正。")
        }
        appendLine("【已記錄的內容】")
        if (recorded.isEmpty()) appendLine("（還沒有）") else recorded.forEach { appendLine("${it.first}：${it.second}") }
        appendLine("【欄位清單（id｜名稱｜題型｜選項）】")
        appendLine(catalogText(catalog))
        appendLine("【最近的對話】")
        if (history.isEmpty()) appendLine("（剛開始）") else history.forEach { appendLine(it) }
        appendLine("【飼主剛說的話】")
        append(userText)
    }

    private fun asStringList(e: JsonElement?): List<String> {
        if (e == null || e.isJsonNull) return emptyList()
        return if (e.isJsonArray) (e as JsonArray).mapNotNull { if (it.isJsonPrimitive) it.asString.trim().ifEmpty { null } else null }
        else if (e.isJsonPrimitive) listOfNotNull(e.asString.trim().ifEmpty { null }) else emptyList()
    }

    /**
     * 解析並驗證 AI 的輸出。壞掉的 JSON 回傳 null。
     * - updates：欄位必須在 catalog 內；單選取一個合法選項、複選只留合法選項、文字題限制長度。
     *   不合法的值不會丟掉，會變成一條 notes（「欄位名稱：值」）。
     * - reply 含診斷或用藥用語時，改用安全的固定回覆。
     */
    fun parse(raw: String, catalog: List<Question>): Result? {
        val byId = catalog.associateBy { it.id }
        return try {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            val root = JsonParser.parseString(raw.substring(start, end + 1)).asJsonObject

            val notes = asStringList(root.get("notes")).toMutableList()
            val updates = mutableListOf<Update>()
            root.getAsJsonArray("updates")?.forEach { el ->
                if (!el.isJsonObject) return@forEach
                val o = el.asJsonObject
                val id = o.get("id")?.takeIf { it.isJsonPrimitive }?.asString ?: return@forEach
                val q = byId[id] ?: return@forEach
                val values = asStringList(o.get("value"))
                if (values.isEmpty()) return@forEach
                when (q.type) {
                    QType.TEXT -> updates += Update(q, values.joinToString("，").take(300))
                    QType.SINGLE -> {
                        val v = values.first()
                        if (v in q.options) updates += Update(q, v) else notes += "${q.label}：$v"
                    }
                    QType.MULTI -> {
                        val ok = values.filter { it in q.options }.distinct()
                        val bad = values.filter { it !in q.options }
                        if (ok.isNotEmpty()) updates += Update(q, ok.joinToString(MULTI_SEP))
                        bad.forEach { notes += "${q.label}：$it" }
                    }
                }
            }
            // 同一欄位只留最後一次
            val dedup = updates.associateBy { it.question.id }.values.toList()

            val reply = root.get("reply")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
            val safeReply = if (reply.isNotEmpty() && isSafe(reply)) reply else SAFE_REPLY

            Result(
                reply = safeReply,
                updates = dedup,
                notes = notes.map { it.take(200) }.distinct().take(5),
                vetQuestions = asStringList(root.get("vet_questions")).map { it.take(200) }.distinct().take(5)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 由 App（不是 AI）產生的確認文字，讓飼主看到到底記下了什麼。
     * changes：欄位名稱、舊值（沒有就是 null）、新值。
     */
    fun confirmation(changes: List<Triple<String, String?, String>>, notes: Int, vetQuestions: Int): String? {
        val parts = mutableListOf<String>()
        val added = changes.filter { it.second == null }
        val edited = changes.filter { it.second != null }
        if (added.isNotEmpty()) parts += "已記下：" + added.joinToString("；") { "${it.first}＝${it.third}" }
        if (edited.isNotEmpty()) parts += "已更正：" + edited.joinToString("；") { "${it.first}：${it.second} → ${it.third}" }
        if (notes > 0) parts += "另外記了 $notes 則補充說明"
        if (vetQuestions > 0) parts += "記了 $vetQuestions 個要問獸醫的問題"
        return if (parts.isEmpty()) null else parts.joinToString("\n")
    }
}
