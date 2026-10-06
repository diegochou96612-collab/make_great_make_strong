package com.petmed.app.interview

import com.petmed.app.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * AI 只負責兩件事，而且**失敗時一律回傳 null，問診流程照常進行**：
 * 1. 把飼主「自己打的文字」對應到選項（mapAnswer）
 * 2. 把已填欄位整理成客觀摘要（narrative）
 *
 * AI 不出題、不診斷、不推薦產品或藥物。題目與流程完全由 App 控制（InterviewBank）。
 * 摘要若含診斷或用藥用語，直接丟棄。供應商與金鑰可在 App 內切換（AiKeyStore）。
 */
object AiAssist {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    data class Mapped(val matched: List<String>, val other: String)

    /** 目前使用的設定。預設為建置時內建的 Groq 金鑰；可由 AiKeyStore 在 App 內改。 */
    @Volatile var config: AiConfig = AiConfig(
        AiProvider.GROQ, BuildConfig.GROQ_API_KEY, AiProvider.GROQ.defaultModel, AiProvider.GROQ.defaultUrl
    )

    /** 最近一次失敗的原因（給飼主看的白話文）；成功時清除。 */
    @Volatile var lastError: String? = null

    fun explainError(httpCode: Int, body: String): String =
        AiProtocol.explainError(config.provider, httpCode, body)

    /** 呼叫模型，成功回傳文字內容，任何失敗回傳 null（原因記錄在 lastError）。 */
    private fun chat(system: String, user: String, maxTokens: Int): String? {
        val cfg = config
        if (!cfg.ready) {
            lastError = "還沒有設定 AI 金鑰。請到「AI 設定」貼上金鑰。"
            return null
        }
        return try {
            val builder = Request.Builder()
                .url(cfg.url)
                .post(AiProtocol.requestBody(cfg, system, user, maxTokens).toRequestBody("application/json".toMediaType()))
            AiProtocol.headers(cfg).forEach { (k, v) -> builder.addHeader(k, v) }
            client.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    lastError = AiProtocol.explainError(cfg.provider, response.code, text)
                    return null
                }
                val result = AiProtocol.parseText(cfg.provider, text)
                lastError = if (result == null) "AI 這次沒有回傳內容，請再試一次。" else null
                result
            }
        } catch (e: Exception) {
            lastError = "網路連線有問題，請確認網路後再試。"
            null
        }
    }

    /** 把飼主打的字對應到選項。對應不到就回傳 null，由呼叫端把原文存成答案。 */
    fun mapAnswer(question: Question, userText: String): Mapped? {
        if (question.options.isEmpty()) return null
        val system = "你是問卷選項對照員。只輸出 JSON，格式：{\"matched\":[選項原文],\"other\":\"無法對應的補充文字\"}。" +
            "規則：matched 只能從提供的選項原文中挑選，不可自行創造或改寫選項；" +
            "單選題 matched 最多一個；無法確定就讓 matched 為空陣列，並把使用者原話放進 other。" +
            "不要解釋、不要診斷、不要加任何其他文字。"
        val user = "問題：${question.text}\n題型：${if (question.type == QType.MULTI) "複選" else "單選"}\n" +
            "選項：${question.options.joinToString("｜")}\n使用者回答：$userText"
        val raw = chat(system, user, 800) ?: return null
        return AiProtocol.parseMapped(raw, question)
    }

    private val forbidden = listOf(
        "可能是", "疑似", "診斷為", "診斷是", "應該是", "建議使用", "建議服用", "建議給",
        "劑量", "毫克", "mg", "炎", "症候群", "中化", "滴爾易", "舒爾利", "力停疼"
    )

    /**
     * 把欄位整理成 2–4 句客觀摘要。若內容含診斷、推測、用藥或產品用語，視為不合格並丟棄（回傳 null）。
     * 「炎」一律擋：寧可丟掉摘要，也不要讓 AI 輸出疾病名稱。
     */
    fun narrative(header: String, lines: List<Pair<String, String>>): String? {
        if (lines.isEmpty()) return null
        val system = "你是動物醫院就診前的「紀錄整理員」。依下列飼主填寫的欄位，用繁體中文寫一段 2 到 4 句的客觀摘要，方便獸醫快速閱讀。" +
            "嚴格規則：只能重述欄位中已有的資訊；不可做診斷、不可推測原因、不可提到任何疾病名稱；" +
            "不可建議用藥、劑量、產品或處置；不可評論嚴重程度；沒有提供的資訊不要猜測；" +
            "不要使用「可能是」「疑似」「應該」等推測用語。只輸出摘要本文，不要標題。"
        val user = header + "\n" + lines.joinToString("\n") { "${it.first}：${it.second}" }
        val text = chat(system, user, 1200) ?: return null
        val cleaned = text.trim()
        if (cleaned.length < 10) {
            lastError = "AI 回覆太短，沒有採用。可以再試一次。"
            return null
        }
        if (forbidden.any { cleaned.contains(it, ignoreCase = true) }) {
            lastError = "AI 的摘要含有推測或診斷用語，為了安全已丟棄。可以再試一次。"
            return null
        }
        return cleaned
    }

    /** AI 是否可用（有金鑰、模型與網址）。沒設定時，對話一律退回固定流程。 */
    val available: Boolean get() = config.ready

    /**
     * 自由對話的一個回合。回傳驗證過的結果；AI 不可用或輸出壞掉時回傳 null（原因在 lastError），
     * 呼叫端要退回固定流程，確保飼主說的話不會遺失。
     */
    fun agent(
        petHeader: String,
        pending: Question?,
        recorded: List<Pair<String, String>>,
        catalog: List<Question>,
        history: List<String>,
        userText: String
    ): AgentProtocol.Result? {
        val prompt = AgentProtocol.userPrompt(petHeader, pending, recorded, catalog, history, userText)
        val raw = chat(AgentProtocol.SYSTEM_PROMPT, prompt, 1500) ?: return null
        val parsed = AgentProtocol.parse(raw, catalog)
        if (parsed == null) lastError = "AI 的回覆格式有問題，這句話改用固定流程記錄。"
        return parsed
    }

    /**
     * 疫苗與檢測手冊的問答。AI 只能依手冊內容回答；回覆經 VaccineGuide.filterAnswer 檢查。
     * AI 不可用或失敗時回傳 null（原因在 lastError）。
     */
    fun handbookAnswer(
        header: String, knowledge: String, history: List<Pair<String, String>>, question: String
    ): String? {
        val raw = chat(VaccineGuide.SYSTEM_PROMPT, VaccineGuide.userPrompt(header, knowledge, history, question), 1200)
            ?: return null
        return VaccineGuide.filterAnswer(raw)
    }

    /** 測試連線：回傳給飼主看的結果說明。 */
    fun ping(): String {
        val r = chat("你是測試用助手。", "請只回覆：好", 600)
        return if (r != null) "連線成功，AI 可以使用（${config.provider.label}）。" else (lastError ?: "連線失敗。")
    }
}
