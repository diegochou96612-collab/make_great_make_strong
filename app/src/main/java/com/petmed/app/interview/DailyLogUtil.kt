package com.petmed.app.interview

import com.petmed.app.data.model.DailyLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 每日紀錄的欄位與選項。
 * 追蹤項目來源：WSAVA 營養評估檢核表「是否有建議客戶持續關注的部分」——
 * 體重、進食狀態、食慾、消化道症狀、活力、外觀整體狀況（外觀以照片紀錄）。
 * 選項沿用 UTenn 年度問卷的分類。睡眠與「吐幾次」為補充項目，無文獻，待獸醫審閱。
 */
object DailyLogSpec {
    val appetite = listOf("正常", "增加", "減少", "幾乎不吃")
    val water = listOf("正常", "變多", "變少")
    val stool = listOf("正常", "軟便或腹瀉", "便秘", "今天沒排便")
    val urine = listOf("正常", "變多", "變少")
    val energy = listOf("比平常有精神", "正常", "比平常沒精神")
    val sleep = listOf("沒變化", "睡比較多", "睡比較少")
}

object LogUtil {

    private fun fmt() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun dateOf(ms: Long): String = fmt().format(Date(ms))

    fun today(): String = dateOf(System.currentTimeMillis())

    fun daysAgo(n: Int, nowMs: Long = System.currentTimeMillis()): String {
        val c = Calendar.getInstance().apply { timeInMillis = nowMs; add(Calendar.DAY_OF_YEAR, -n) }
        return fmt().format(c.time)
    }

    /** 顯示用：10/02（週四） */
    fun pretty(date: String): String {
        val d = runCatching { fmt().parse(date) }.getOrNull() ?: return date
        val c = Calendar.getInstance().apply { time = d }
        val wd = listOf("日", "一", "二", "三", "四", "五", "六")[c.get(Calendar.DAY_OF_WEEK) - 1]
        return "%02d/%02d（週%s）".format(c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), wd)
    }

    /** 只留最近 days 天（含今天）。days 為 null 代表全部。依日期新到舊排序。 */
    fun recent(logs: List<DailyLog>, days: Int?, nowMs: Long = System.currentTimeMillis()): List<DailyLog> {
        val sorted = logs.sortedByDescending { it.date }
        if (days == null) return sorted
        val earliest = daysAgo(days - 1, nowMs)
        return sorted.filter { it.date >= earliest }
    }

    /** 一筆紀錄的文字摘要，只列出有填的欄位。 */
    fun summary(l: DailyLog): String {
        val parts = buildList {
            if (l.appetite.isNotBlank()) add("食慾：${l.appetite}")
            if (l.water.isNotBlank()) add("喝水：${l.water}")
            if (l.stool.isNotBlank()) add("大便：${l.stool}")
            if (l.urine.isNotBlank()) add("小便：${l.urine}")
            if (l.energy.isNotBlank()) add("精神：${l.energy}")
            if (l.sleep.isNotBlank()) add("睡眠：${l.sleep}")
            if (l.vomitCount.isNotBlank()) add("嘔吐：${l.vomitCount} 次")
            if (l.weight.isNotBlank()) add("體重：${l.weight}")
            if (l.note.isNotBlank()) add("備註：${l.note}")
        }
        return if (parts.isEmpty()) "（沒有填寫內容）" else parts.joinToString("；")
    }

    fun isEmpty(l: DailyLog) = summary(l) == "（沒有填寫內容）"
}
