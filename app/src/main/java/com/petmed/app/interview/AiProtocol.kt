package com.petmed.app.interview

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * AI 供應商。金鑰過期、模型被下架時，可以在 App 內切換，不必重新 Build。
 *
 * 規格查證（2026-10-02）：
 * - Claude：POST https://api.anthropic.com/v1/messages，標頭 x-api-key、anthropic-version: 2023-06-01；
 *   回應 content 是區塊陣列，文字在 type == "text" 的區塊；Sonnet 5.5 預設啟用 adaptive thinking，
 *   會先回 thinking 區塊，且思考 token 計入 max_tokens；model 填 claude-sonnet-5-5
 *   （官方承諾不早於 2027-09-28 退役；Haiku 4.5 最早 2026-10-15 就可能退役，所以不選）。
 * - Gemini：Google 官方的 OpenAI 相容端點（base URL .../v1beta/openai/，路徑 /chat/completions，標頭 Bearer），
 *   官方定價頁列出多個有免費額度的模型（含 gemini-2.5-flash）；2.5 系列會思考，所以也要預留輸出長度。
 * - Groq：OpenAI 相容格式。免費金鑰會過期（2026-10-02 實測 expired_api_key）。
 * - 自訂：任何 OpenAI 相容的端點（例如 OpenAI、Gemini 的 OpenAI 相容端點）。
 */
enum class AiProvider(val label: String, val defaultModel: String, val defaultUrl: String, val keyPrefix: String) {
    ANTHROPIC("Claude（Anthropic，付費）", "claude-sonnet-5-5", "https://api.anthropic.com/v1/messages", "sk-ant-"),
    GEMINI("Gemini（Google，有免費額度）", "gemini-2.5-flash", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", ""),
    GROQ("Groq（金鑰可能過期）", "openai/gpt-oss-20b", "https://api.groq.com/openai/v1/chat/completions", "gsk_"),
    CUSTOM("自訂（OpenAI 相容）", "", "", "")
}

data class AiConfig(val provider: AiProvider, val key: String, val model: String, val url: String) {
    val ready: Boolean get() = key.isNotBlank() && model.isNotBlank() && url.isNotBlank()
}

object AiProtocol {

    const val ANTHROPIC_VERSION = "2023-06-01"

    /** Claude 會先思考再回答，思考 token 計入 max_tokens，所以要給足額度。 */
    private const val ANTHROPIC_MAX_TOKENS = 4096

    fun headers(cfg: AiConfig): Map<String, String> = when (cfg.provider) {
        AiProvider.ANTHROPIC -> mapOf(
            "x-api-key" to cfg.key,
            "anthropic-version" to ANTHROPIC_VERSION,
            "content-type" to "application/json"
        )
        else -> mapOf(
            "Authorization" to "Bearer ${cfg.key}",
            "content-type" to "application/json"
        )
    }

    fun requestBody(cfg: AiConfig, system: String, user: String, maxTokens: Int): String {
        val root = JsonObject()
        root.addProperty("model", cfg.model)
        when (cfg.provider) {
            AiProvider.ANTHROPIC -> {
                root.addProperty("max_tokens", maxOf(maxTokens, ANTHROPIC_MAX_TOKENS))
                root.addProperty("system", system)
                root.add("messages", JsonArray().apply {
                    add(JsonObject().apply { addProperty("role", "user"); addProperty("content", user) })
                })
            }
            else -> {
                root.add("messages", JsonArray().apply {
                    add(JsonObject().apply { addProperty("role", "system"); addProperty("content", system) })
                    add(JsonObject().apply { addProperty("role", "user"); addProperty("content", user) })
                })
                if (cfg.provider == AiProvider.GROQ) {
                    root.addProperty("max_completion_tokens", maxTokens)
                    root.addProperty("reasoning_effort", "low")
                    root.addProperty("temperature", 0.2)
                } else {
                    // Gemini 2.5 等模型會先思考，思考 token 也計入上限，所以至少給 4096
                    root.addProperty("max_tokens", maxOf(maxTokens, 4096))
                    root.addProperty("temperature", 0.2)
                }
            }
        }
        return root.toString()
    }

    /** 從回應取出文字。Claude 只取 type == "text" 的區塊（跳過 thinking）。取不到回傳 null。 */
    fun parseText(provider: AiProvider, body: String): String? = try {
        val root = JsonParser.parseString(body).asJsonObject
        val text = if (provider == AiProvider.ANTHROPIC) {
            root.getAsJsonArray("content")
                ?.filter { it.isJsonObject && it.asJsonObject.get("type")?.asString == "text" }
                ?.joinToString("") { it.asJsonObject.get("text")?.asString ?: "" }
        } else {
            root.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject
                ?.getAsJsonObject("message")?.get("content")?.takeIf { !it.isJsonNull }?.asString
        }
        text?.trim()?.ifEmpty { null }
    } catch (e: Exception) {
        null
    }

    /** 把 API 錯誤轉成給飼主看的白話文。 */
    fun explainError(provider: AiProvider, httpCode: Int, body: String): String {
        val b = body.lowercase()
        return when {
            "expired_api_key" in b -> "AI 金鑰已過期。請重新建立一把金鑰，再貼到「AI 設定」。"
            "limit: 0" in b -> "這個模型在你的帳號沒有免費額度（limit: 0）。請在「AI 設定」換一個模型，例如 gemini-2.5-flash，或改用其他供應商。"
            "credit balance" in b || "insufficient" in b || "billing" in b || httpCode == 402 ->
                "AI 帳戶餘額不足。請到供應商網站儲值後再試。"
            "authentication_error" in b || "invalid_api_key" in b || "invalid x-api-key" in b || httpCode == 401 ->
                "AI 金鑰無效。請確認金鑰有複製完整，並且選對供應商，再貼到「AI 設定」。"
            httpCode == 403 || "permission" in b -> "這把金鑰沒有權限使用這個模型。"
            "model_not_found" in b || "not_found_error" in b || "decommissioned" in b || httpCode == 404 ->
                "AI 模型目前無法使用（被停用、名稱錯誤，或沒有權限）。可以在「AI 設定」改模型名稱。"
            httpCode == 429 || "rate_limit" in b -> "AI 用量達到上限，請稍後再試。"
            httpCode == 529 || "overloaded" in b -> "AI 服務目前太忙，請稍後再試。"
            httpCode >= 500 -> "AI 服務暫時有問題，請稍後再試。"
            else -> "AI 呼叫失敗（${provider.label}，代碼 $httpCode）。"
        }
    }

    /** 解析「選項對應」的 JSON 輸出。matched 必須全部來自選項，否則視為無效。 */
    fun parseMapped(raw: String, q: Question): AiAssist.Mapped? = try {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) null else {
            val json = JsonParser.parseString(raw.substring(start, end + 1)).asJsonObject
            val matched = json.getAsJsonArray("matched")?.map { it.asString } ?: emptyList()
            if (matched.any { it !in q.options }) null else {
                val limited = if (q.type == QType.SINGLE) matched.take(1) else matched
                AiAssist.Mapped(limited, json.get("other")?.takeIf { !it.isJsonNull }?.asString ?: "")
            }
        }
    } catch (e: Exception) {
        null
    }
}
