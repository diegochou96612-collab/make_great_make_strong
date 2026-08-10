package com.petmed.app.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.petmed.app.BuildConfig
import com.petmed.app.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    val messages = mutableStateListOf<ChatMessage>().apply {
        add(ChatMessage(
            id = 0,
            content = "您好！我是動物飼養助手，專門提供寵物用藥諮詢。請問您的寵物有什麼問題嗎？",
            isFromUser = false
        ))
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 把整份知識庫切成段落，載入一次即可
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

    // 根據使用者問題，從知識庫找相關段落（限制約 5000 字）
    private fun retrieveRelevantContext(query: String): String {
        if (knowledgeChunks.isEmpty()) return ""

        val queryWords = query.filter { it.isLetterOrDigit() || it == ' ' }
            .split(" ")
            .filter { it.isNotBlank() }
            .flatMap { it.chunked(1) } // 中文一字一字拆
            .toSet()

        val scored = knowledgeChunks.map { chunk ->
            val score = queryWords.count { word -> chunk.contains(word) }
            chunk to score
        }

        val relevant = scored
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .ifEmpty { knowledgeChunks.take(10) } // 若完全沒命中，取前10段

        val result = StringBuilder()
        for (chunk in relevant) {
            if (result.length + chunk.length > 5000) break
            result.append(chunk).append("\n\n")
        }
        return result.toString().trim()
    }

    private val baseSystemPrompt =
        "你是一位專業的動物飼養助手，專門協助飼主解答寵物用藥與健康問題。\n" +
        "請以繁體中文回答，語氣親切專業，回答需簡潔。\n" +
        "在適當時建議飼主就醫確認診斷。\n"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val chatHistory = mutableListOf<Pair<String, String>>()

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        messages.add(ChatMessage(id = messages.size, content = userText, isFromUser = true))
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val context = withContext(Dispatchers.IO) { retrieveRelevantContext(userText) }
                val reply = withContext(Dispatchers.IO) { callGroq(userText, context) }
                chatHistory.add("user" to userText)
                chatHistory.add("assistant" to reply)
                messages.add(ChatMessage(id = messages.size, content = reply, isFromUser = false))
            } catch (e: Exception) {
                messages.add(ChatMessage(id = messages.size, content = "連線失敗：${e.message}", isFromUser = false))
            } finally {
                _isLoading.value = false
            }
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
            .put("model", "llama-3.3-70b-versatile")
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
