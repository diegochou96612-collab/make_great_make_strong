package com.petmed.app.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.petmed.app.BuildConfig
import com.petmed.app.data.database.AppDatabase
import com.petmed.app.data.model.ChatMessage
import com.petmed.app.data.model.ChatMessageEntity
import com.petmed.app.data.model.ChatSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AIChatViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).chatHistoryDao()

    val messages = mutableStateListOf<ChatMessage>().apply {
        add(ChatMessage(id = 0, content = "您好！我是動物飼養助手，專門提供寵物用藥諮詢。請問您的寵物有什麼問題嗎？", isFromUser = false))
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 所有歷史對話清單（給 Drawer 用）
    val allSessions = dao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 當前正在查看的歷史對話（null = 目前對話）
    private val _viewingSession = MutableStateFlow<List<ChatMessage>?>(null)
    val viewingSession: StateFlow<List<ChatMessage>?> = _viewingSession

    private var currentSessionId: Long? = null
    private val chatHistory = mutableListOf<Pair<String, String>>()

    private val knowledgeChunks: List<String> by lazy {
        try {
            getApplication<Application>().assets
                .open("動物保健依據(全).txt")
                .bufferedReader(Charsets.UTF_8)
                .readText()
                .split(Regex("\\n{2,}"))
                .map { it.trim() }
                .filter { it.length > 20 }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun retrieveRelevantContext(query: String): String {
        if (knowledgeChunks.isEmpty()) return ""
        val queryWords = query.filter { it.isLetterOrDigit() || it == ' ' }
            .split(" ").filter { it.isNotBlank() }
            .flatMap { it.chunked(1) }.toSet()
        val scored = knowledgeChunks.map { chunk ->
            chunk to queryWords.count { chunk.contains(it) }
        }
        val relevant = scored.filter { it.second > 0 }
            .sortedByDescending { it.second }.map { it.first }
            .ifEmpty { knowledgeChunks.take(10) }
        val result = StringBuilder()
        for (chunk in relevant) {
            if (result.length + chunk.length > 5000) break
            result.append(chunk).append("\n\n")
        }
        return result.toString().trim()
    }

    private val emergencyKeywords = listOf(
        "中毒", "誤食", "呼吸困難", "喘不過氣", "無法站立", "昏迷", "意識不清",
        "大量出血", "劇烈嘔吐", "血便", "血尿", "抽搐", "癲癇發作", "骨折", "急救"
    )

    private val emergencyReply =
        "⚠️ 您描述的症狀屬於緊急狀況，請立即停止自行處理，馬上帶寵物前往最近的動物醫院或急診。\n\n" +
        "途中請保持寵物安靜、平躺，避免過度移動。\n\n" +
        "如誤食有毒物質，請記錄誤食物品名稱與時間，一併告知獸醫師。"

    private val baseSystemPrompt = """
你是中化動藥的動物健康助手，提供寵物用藥與健康的衛教資訊。
請以繁體中文回答，語氣親切專業，回答簡潔清楚。

【合規規則——必須遵守】
1. 只提供衛教資訊與可能方向，不做確定性診斷，不使用「一定」「保證有效」「絕對」等字眼。
2. 所有回答結尾視情況加上「建議諮詢獸醫師確認」，讓飼主自行決定是否就醫。
3. 不蒐集、不詢問飼主個人資料（姓名、地址、電話等）。
4. 推薦藥品時，說明適應症，並補充「請確認是否適合您的寵物後再使用」。

【中化動藥產品——遇到相關症狀時主動推薦】
抗生素：
- 滴爾易：犬貓耳炎、皮膚感染
- 舒爾利軟膏：犬貓鴿子結膜炎、角膜炎、術後預防感染

潔耳劑：
- 舒聰敏：耳道清潔保養

非類固醇消炎止痛藥：
- 力停疼75毫克：犬隻疼痛及發炎（僅限犬）

外用液劑：
- 輕鬆洗外用液劑：犬貓皮膚炎、黴菌（小芽孢菌、髮癬菌）感染

疫苗：
- 日生研狂犬病疫苗：預防犬貓狂犬病（需由獸醫師施打）

營養保健品：
- 汪好腸：犬用腸道健康保健品
- 綠谷草本膏：犬貓皮膚護理
- 透優彩：視網膜健康
- 透優希：犬貓水晶體健康
- 透優明：犬貓水晶體健康
- 歐諾飲：幼犬及老年犬照護
- 富樂-B液：幼犬營養補充

【可追溯原則】
回答時，如果是依據中化產品資料，請說明「依據中化動藥產品資訊」；如果是依據一般動物保健知識，請說明「依據動物保健知識庫」。
    """.trimIndent()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun startNewChat() {
        messages.clear()
        messages.add(ChatMessage(id = 0, content = "您好！我是動物飼養助手，專門提供寵物用藥諮詢。請問您的寵物有什麼問題嗎？", isFromUser = false))
        chatHistory.clear()
        currentSessionId = null
        _viewingSession.value = null
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val msgs = withContext(Dispatchers.IO) { dao.getMessagesForSession(sessionId) }
            _viewingSession.value = msgs.mapIndexed { i, m ->
                ChatMessage(id = i, content = m.content, isFromUser = m.isFromUser)
            }
        }
    }

    fun clearViewingSession() {
        _viewingSession.value = null
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteMessagesForSession(sessionId)
            dao.deleteSession(sessionId)
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        messages.add(ChatMessage(id = messages.size, content = userText, isFromUser = true))

        if (emergencyKeywords.any { userText.contains(it) }) {
            chatHistory.add("user" to userText)
            chatHistory.add("assistant" to emergencyReply)
            messages.add(ChatMessage(id = messages.size, content = emergencyReply, isFromUser = false))
            saveToHistory(userText, emergencyReply)
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val context = withContext(Dispatchers.IO) { retrieveRelevantContext(userText) }
                val reply = withContext(Dispatchers.IO) { callGroq(userText, context) }
                chatHistory.add("user" to userText)
                chatHistory.add("assistant" to reply)
                messages.add(ChatMessage(id = messages.size, content = reply, isFromUser = false))
                saveToHistory(userText, reply)
            } catch (e: Exception) {
                messages.add(ChatMessage(id = messages.size, content = "連線失敗：${e.message}", isFromUser = false))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun saveToHistory(userText: String, reply: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (currentSessionId == null) {
                val title = if (userText.length > 20) userText.take(20) + "…" else userText
                currentSessionId = dao.insertSession(ChatSessionEntity(title = title))
            }
            val sid = currentSessionId ?: return@launch
            dao.insertMessage(ChatMessageEntity(sessionId = sid, content = userText, isFromUser = true))
            dao.insertMessage(ChatMessageEntity(sessionId = sid, content = reply, isFromUser = false))
        }
    }

    private fun callGroq(userText: String, context: String): String {
        val systemContent = buildString {
            append(baseSystemPrompt)
            if (context.isNotEmpty()) {
                append("\n=== 相關動物保健資料 ===\n")
                append(context)
            }
        }
        val messagesArray = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemContent))
            chatHistory.forEach { (role, content) ->
                put(JSONObject().put("role", role).put("content", content))
            }
            put(JSONObject().put("role", "user").put("content", userText))
        }
        val body = JSONObject()
            .put("model", "openai/gpt-oss-20b")
            .put("messages", messagesArray)
            .put("max_tokens", 1024)
            .toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
            .post(body)
            .build()
        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("空回應")
        if (!response.isSuccessful) throw Exception(responseBody)
        return JSONObject(responseBody)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }
}
