package com.petmed.app.interview

/**
 * 人工審核過的衛教影片清單。
 *
 * 原則：**AI 不會生成任何連結**（會編造不存在的網址、也無法追溯）。
 * App 只依「狀況類別」從這份清單挑影片；每一支都經過人工查證存在，並註明來源與限制。
 * 目前只有 1 支，**需要業師推薦更多（尤其是台灣獸醫師的中文影片）並審閱**。
 */
data class VideoItem(
    val id: String,
    val title: String,
    val channel: String,
    val url: String,
    /** 對應的狀況類別（InterviewBank.CAT_*） */
    val categories: Set<String>,
    /** 適用物種關鍵字（例如「狗」）；空集合代表不限 */
    val speciesKeywords: Set<String>,
    val language: String,
    val verifiedOn: String,
    /** 內容說明（來自機構官網頁面，我沒有看過影片本身） */
    val about: String,
    /** 顯示給飼主的注意事項 */
    val caution: String
)

object VideoCatalog {

    val items = listOf(
        VideoItem(
            id = "ear_clean_ontariospca",
            title = "How to clean your dog's ears",
            channel = "Ontario SPCA and Humane Society（加拿大動物福利機構）",
            url = "https://www.youtube.com/watch?v=aGxUL8BIFbU",
            categories = setOf(InterviewBank.CAT_EAR),
            speciesKeywords = setOf("狗", "犬"),
            language = "英文",
            verifiedOn = "2026-10-02",
            about = "講者 April 為該機構的註冊獸醫技術員（RVT）。官網文字說明：用獸醫建議的清耳液、棉球（不用棉花棒），" +
                "擠入後按摩耳根約 30 秒，讓狗甩頭，再擦乾。",
            caution = "這是「一般清潔」的示範，只適用於狗。如果耳朵已經有紅腫、異味或分泌物，請先問獸醫能不能自己清，" +
                "以及該用哪一種清潔液。影片為英文，尚未經獸醫審閱。"
        )
    )

    /** 依狀況類別與物種挑出影片。物種不明時不排除。 */
    fun forCase(categories: Set<String>, speciesText: String): List<VideoItem> =
        items.filter { v ->
            v.categories.any { it in categories } &&
                (v.speciesKeywords.isEmpty() || speciesText.isBlank() || v.speciesKeywords.any { speciesText.contains(it) })
        }
}
