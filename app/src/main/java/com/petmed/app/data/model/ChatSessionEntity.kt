package com.petmed.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun formattedDate(): String =
        SimpleDateFormat("MM/dd HH:mm", Locale.TAIWAN).format(Date(createdAt))
}
