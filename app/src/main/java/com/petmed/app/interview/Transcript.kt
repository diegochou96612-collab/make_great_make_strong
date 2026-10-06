package com.petmed.app.interview

/**
 * 從稽核事件還原先前的對話，讓飼主離開後可以接續問診。
 * 事件格式（見 InterviewViewModel.submit）：「題目id｜題目文字｜答案｜出處：…」
 */
object Transcript {

    data class Turn(val questionId: String, val question: String, val answer: String)

    data class ChatLine(val fromUser: Boolean, val text: String)

    /** 自由對話的事件：「U｜飼主說的話」或「B｜助理回的話」。 */
    fun parseChat(detail: String): ChatLine? = when {
        detail.startsWith("U｜") -> ChatLine(true, detail.substring(2))
        detail.startsWith("B｜") -> ChatLine(false, detail.substring(2))
        else -> null
    }

    fun parseAnswered(detail: String): Turn? {
        val cut = detail.lastIndexOf("｜出處：")
        if (cut < 0) return null
        val parts = detail.substring(0, cut).split("｜", limit = 3)
        if (parts.size < 3) return null
        return Turn(parts[0], parts[1], parts[2])
    }
}
