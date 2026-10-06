package com.petmed.app.interview

import android.content.Context
import com.petmed.app.BuildConfig

/**
 * 在 App 內儲存 AI 供應商、金鑰與模型，讓金鑰過期、模型被下架時不用重新 Build。
 * 存在這支手機的 App 私有設定中；僅供個人示範使用，正式上線應改由後端保管金鑰。
 * 每個供應商各自存一把金鑰，避免貼錯。
 */
object AiKeyStore {
    private const val PREFS = "ai_prefs"
    private const val K_PROVIDER = "provider"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun savedProvider(c: Context): AiProvider =
        runCatching { AiProvider.valueOf(prefs(c).getString(K_PROVIDER, null) ?: "") }.getOrDefault(AiProvider.GROQ)

    fun savedKey(c: Context, p: AiProvider): String = prefs(c).getString("key_${p.name}", "") ?: ""
    fun savedModel(c: Context, p: AiProvider): String = prefs(c).getString("model_${p.name}", "")?.ifBlank { null } ?: p.defaultModel
    fun savedUrl(c: Context, p: AiProvider): String = prefs(c).getString("url_${p.name}", "")?.ifBlank { null } ?: p.defaultUrl

    /** App 啟動時呼叫：把儲存的設定套用到 AiAssist。沒有儲存的 Groq 金鑰就退回建置時內建的。 */
    fun load(c: Context) {
        val p = savedProvider(c)
        val key = savedKey(c, p).ifBlank { if (p == AiProvider.GROQ) BuildConfig.GROQ_API_KEY else "" }
        AiAssist.config = AiConfig(p, key, savedModel(c, p), savedUrl(c, p))
    }

    fun save(c: Context, provider: AiProvider, key: String, model: String, url: String) {
        prefs(c).edit()
            .putString(K_PROVIDER, provider.name)
            .putString("key_${provider.name}", key.trim())
            .putString("model_${provider.name}", model.trim())
            .putString("url_${provider.name}", url.trim())
            .apply()
        load(c)
        AiAssist.lastError = null
    }

    /** 是否有在 App 內貼過目前這個供應商的金鑰 */
    fun hasOverride(c: Context): Boolean = savedKey(c, savedProvider(c)).isNotBlank()

    /** 金鑰格式的基本檢查（不呼叫網路）。回傳 null 代表格式沒問題。 */
    fun formatProblem(key: String, provider: AiProvider = AiProvider.GROQ): String? {
        val k = key.trim()
        if (k.isEmpty()) return null
        return when {
            k.any { it.isWhitespace() } -> "金鑰中間有空白或換行，請重新複製。"
            k.startsWith("sk-or-") -> "這是 OpenRouter 的金鑰，本 App 不使用 OpenRouter。"
            provider == AiProvider.ANTHROPIC && !k.startsWith("sk-ant-") -> "Claude 的金鑰是 sk-ant- 開頭，請確認有沒有複製完整，並確認供應商選對了。"
            provider == AiProvider.GROQ && !k.startsWith("gsk_") -> "Groq 的金鑰是 gsk_ 開頭，請確認有沒有複製完整，並確認供應商選對了。"
            k.length < 30 && provider != AiProvider.CUSTOM -> "金鑰看起來太短，可能沒複製完整。"
            else -> null
        }
    }
}
