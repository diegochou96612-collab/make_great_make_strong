package com.petmed.app.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petmed.app.BuildConfig
import com.petmed.app.data.api.ClaudeApiClient
import com.petmed.app.data.api.ClaudeMessage
import com.petmed.app.data.api.ClaudeRequest
import com.petmed.app.data.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AIChatViewModel : ViewModel() {

    val messages = mutableStateListOf<ChatMessage>().apply {
        add(ChatMessage(
            id = 0,
            content = "您好！我是動物飼養助手，專門提供寵物用藥諮詢。請問您的寵物有什麼問題嗎？",
            isFromUser = false
        ))
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val systemPrompt = """
        你是一位專業的動物飼養助手，專門協助飼主解答寵物用藥與健康問題。
        請以繁體中文回答，語氣親切專業。
        回答需簡潔，並在適當時建議飼主就醫確認診斷。
    """.trimIndent()

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        messages.add(ChatMessage(id = messages.size, content = userText, isFromUser = true))
        _isLoading.value = true

        val history = messages
            .filter { it.id > 0 }
            .map { ClaudeMessage(role = if (it.isFromUser) "user" else "assistant", content = it.content) }

        viewModelScope.launch {
            try {
                val response = ClaudeApiClient.service.sendMessage(
                    apiKey = BuildConfig.CLAUDE_API_KEY,
                    request = ClaudeRequest(
                        system = systemPrompt,
                        messages = history
                    )
                )
                val replyText = response.content.firstOrNull()?.text ?: "無法取得回應"
                messages.add(ChatMessage(id = messages.size, content = replyText, isFromUser = false))
            } catch (e: Exception) {
                messages.add(ChatMessage(id = messages.size, content = "連線失敗，請稍後再試。", isFromUser = false))
            } finally {
                _isLoading.value = false
            }
        }
    }
}
