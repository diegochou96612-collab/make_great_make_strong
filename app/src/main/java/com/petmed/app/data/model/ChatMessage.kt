package com.petmed.app.data.model

data class ChatMessage(
    val id: Int,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: String = ""
)
