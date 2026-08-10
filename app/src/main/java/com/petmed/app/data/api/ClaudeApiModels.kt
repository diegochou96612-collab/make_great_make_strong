package com.petmed.app.data.api

import com.google.gson.annotations.SerializedName

data class ClaudeMessage(
    val role: String,
    val content: String
)

data class ClaudeRequest(
    val model: String = "claude-haiku-4-5-20251001",
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val system: String,
    val messages: List<ClaudeMessage>
)

data class ClaudeResponse(
    val id: String,
    val content: List<ClaudeContentBlock>
)

data class ClaudeContentBlock(
    val type: String,
    val text: String
)
