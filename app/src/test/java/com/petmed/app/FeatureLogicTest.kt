package com.petmed.app

import com.petmed.app.data.model.DailyLog
import com.petmed.app.interview.AiAssist
import com.petmed.app.interview.AiKeyStore
import com.petmed.app.interview.DailyLogSpec
import com.petmed.app.interview.InterviewBank
import com.petmed.app.interview.LogUtil
import com.petmed.app.interview.PhotoStore
import com.petmed.app.interview.Species
import com.petmed.app.interview.VideoCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FeatureLogicTest {

    private fun ms(y: Int, m: Int, d: Int): Long =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, 12, 0, 0) }.timeInMillis

    private fun log(date: String, appetite: String = "") = DailyLog(caseId = 1, date = date, appetite = appetite)

    // ───── 每日紀錄 ─────

    @Test fun `recent 只留最近 N 天含今天，且新到舊排序`() {
        val now = ms(2026, 10, 2)
        val logs = listOf(log("2026-09-28"), log("2026-10-02"), log("2026-10-01"), log("2026-09-30"))
        assertEquals(listOf("2026-10-02", "2026-10-01", "2026-09-30"), LogUtil.recent(logs, 3, now).map { it.date })
        assertEquals(4, LogUtil.recent(logs, null, now).size)
        assertEquals(listOf("2026-10-02"), LogUtil.recent(logs, 1, now).map { it.date })
    }

    @Test fun `daysAgo 跨月正確`() {
        assertEquals("2026-09-30", LogUtil.daysAgo(2, ms(2026, 10, 2)))
        assertEquals("2026-10-02", LogUtil.daysAgo(0, ms(2026, 10, 2)))
    }

    @Test fun `summary 只列出有填的欄位`() {
        val l = DailyLog(caseId = 1, date = "2026-10-02", appetite = "減少", vomitCount = "2", note = "早上吐了")
        assertEquals("食慾：減少；嘔吐：2 次；備註：早上吐了", LogUtil.summary(l))
    }

    @Test fun `全空的紀錄視為空`() {
        assertTrue(LogUtil.isEmpty(log("2026-10-02")))
        assertFalse(LogUtil.isEmpty(log("2026-10-02", "正常")))
    }

    @Test fun `pretty 日期顯示星期`() {
        assertEquals("10/02（週五）", LogUtil.pretty("2026-10-02"))
        assertEquals("not-a-date", LogUtil.pretty("not-a-date"))
    }

    @Test fun `每日紀錄選項都不為空`() {
        listOf(
            DailyLogSpec.appetite, DailyLogSpec.water, DailyLogSpec.stool,
            DailyLogSpec.urine, DailyLogSpec.energy, DailyLogSpec.sleep
        ).forEach { assertTrue(it.isNotEmpty()) }
    }

    // ───── 衛教影片 ─────

    @Test fun `影片清單的網址格式正確且都有說明與注意事項`() {
        VideoCatalog.items.forEach { v ->
            assertTrue("${v.id} 網址格式", Regex("^https://www\\.youtube\\.com/watch\\?v=[A-Za-z0-9_-]{11}$").matches(v.url))
            assertTrue(v.about.isNotBlank())
            assertTrue(v.caution.isNotBlank())
            assertTrue(v.categories.isNotEmpty())
            assertTrue(v.verifiedOn.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        }
        assertEquals(VideoCatalog.items.size, VideoCatalog.items.map { it.id }.toSet().size)
    }

    @Test fun `耳朵影片只對狗顯示`() {
        val ear = setOf(InterviewBank.CAT_EAR)
        assertEquals(1, VideoCatalog.forCase(ear, "狗").size)
        assertEquals(1, VideoCatalog.forCase(ear, "黃金獵犬（犬）").size)
        assertTrue(VideoCatalog.forCase(ear, "貓").isEmpty())
        assertEquals(1, VideoCatalog.forCase(ear, "").size)
    }

    @Test fun `沒有對應類別就沒有影片`() {
        assertTrue(VideoCatalog.forCase(setOf(InterviewBank.CAT_VOMIT), "狗").isEmpty())
    }

    // ───── 照片尺寸 ─────

    @Test fun `sampleSize 讓最長邊不小於 maxEdge`() {
        assertEquals(1, PhotoStore.sampleSize(1000, 800, 1280))
        assertEquals(2, PhotoStore.sampleSize(4000, 3000, 1280))
        assertEquals(4, PhotoStore.sampleSize(6000, 4000, 1280))
        assertEquals(1, PhotoStore.sampleSize(0, 0, 1280))
    }

    @Test fun `fitWithin 不放大、等比縮小`() {
        assertEquals(800 to 600, PhotoStore.fitWithin(800, 600, 1280))
        assertEquals(1280 to 960, PhotoStore.fitWithin(4000, 3000, 1280))
        assertEquals(960 to 1280, PhotoStore.fitWithin(3000, 4000, 1280))
    }

    // ───── AI 金鑰與錯誤說明 ─────

    @Test fun `金鑰過期要明確說過期，不可只說連線失敗`() {
        val body = """{"error":{"message":"Invalid API Key","type":"invalid_request_error","code":"expired_api_key"}}"""
        val msg = AiAssist.explainError(401, body)
        assertTrue(msg.contains("過期"))
        assertTrue(msg.contains("AI 設定"))
    }

    @Test fun `其他錯誤的說明`() {
        assertTrue(AiAssist.explainError(401, """{"error":{"code":"invalid_api_key"}}""").contains("無效"))
        assertTrue(AiAssist.explainError(429, "{}").contains("上限"))
        assertTrue(AiAssist.explainError(404, """{"error":{"code":"model_not_found"}}""").contains("模型"))
        assertTrue(AiAssist.explainError(503, "").contains("暫時"))
    }

    @Test fun `金鑰格式檢查`() {
        val good = "gsk_" + "a".repeat(52)
        assertEquals(null, AiKeyStore.formatProblem(good))
        assertEquals(null, AiKeyStore.formatProblem("   "))
        assertTrue(AiKeyStore.formatProblem("sk-or-v1-" + "a".repeat(60))!!.contains("OpenRouter"))
        assertTrue(AiKeyStore.formatProblem("abc123")!!.contains("gsk_"))
        assertTrue(AiKeyStore.formatProblem("gsk_short")!!.contains("太短"))
        assertTrue(AiKeyStore.formatProblem("gsk_" + "a".repeat(30) + " " + "b".repeat(30))!!.contains("空白"))
    }

    // ───── 拍照提示 ─────

    @Test fun `有拍照提示的題目，相簿主題必須在主題清單內`() {
        val withPhoto = InterviewBank.all.filter { it.photoHint.isNotBlank() }
        assertTrue(withPhoto.isNotEmpty())
        withPhoto.forEach { assertTrue("${it.id} 的主題 ${it.photoTopic}", it.photoTopic in PhotoStore.topics) }
    }

    @Test fun `plan 回傳的題目帶有拍照提示`() {
        val a = mapOf("complaint" to "x", "categories" to InterviewBank.CAT_EAR)
        val p = InterviewBank.plan(a, Species.DOG)
        assertTrue(p.first { it.id == "ear_signs" }.photoHint.isNotBlank())
        assertTrue(p.first { it.id == "med_current" }.photoHint.isBlank())
    }
}
