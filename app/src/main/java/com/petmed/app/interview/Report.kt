package com.petmed.app.interview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.petmed.app.data.model.CaseEvent
import com.petmed.app.data.model.CaseField
import com.petmed.app.data.model.CasePhoto
import com.petmed.app.data.model.DailyLog
import com.petmed.app.data.model.HealthCase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportRow(val fieldId: Long, val label: String, val value: String, val edited: Boolean, val missing: Boolean)
data class ReportSection(val title: String, val rows: List<ReportRow>)

object ReportBuilder {

    const val DISCLAIMER = "本報告由飼主陳述整理而成，不是診斷結果，也不能取代獸醫的專業判斷。實際用藥與治療請依獸醫指示。"

    fun displayValue(f: CaseField): String = when (f.state) {
        CaseField.STATE_NOT_PROVIDED -> "未提供"
        CaseField.STATE_UNKNOWN -> "不確定"
        else -> f.value.ifBlank { "未提供" }
    }

    fun sections(fields: List<CaseField>): List<ReportSection> {
        val grouped = fields.groupBy { it.section }
        return InterviewBank.sectionOrder.mapNotNull { title ->
            val rows = grouped[title]?.sortedBy { it.sortOrder }?.map {
                ReportRow(
                    fieldId = it.id,
                    label = it.label,
                    value = displayValue(it),
                    edited = it.origin == CaseField.ORIGIN_EDITED,
                    missing = it.state != CaseField.STATE_ANSWERED
                )
            }
            if (rows.isNullOrEmpty()) null else ReportSection(title, rows)
        }
    }

    /** 問診過程中觸發過的緊急提示（依事件紀錄還原，去除重複） */
    fun redFlags(events: List<CaseEvent>): List<RedFlag> =
        events.filter { it.kind == CaseEvent.RED_FLAG }
            .mapNotNull { e -> RedFlag.values().firstOrNull { it.name == e.detail.substringBefore("|") } }
            .distinct()

    fun formatTime(ms: Long): String =
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN).format(Date(ms))
}

/** 以 Android 內建 PdfDocument 輸出 A4 報告。中文使用系統字型。 */
object PdfExporter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40
    private const val FOOTER_H = 36

    /** days：每日紀錄只放最近幾天（null＝全部）；includePhotos：是否附上照片 */
    data class ExportOptions(val days: Int? = 7, val includePhotos: Boolean = false)

    fun export(
        context: Context,
        case: HealthCase,
        sections: List<ReportSection>,
        redFlags: List<RedFlag>,
        logs: List<DailyLog> = emptyList(),
        photos: List<CasePhoto> = emptyList(),
        options: ExportOptions = ExportOptions()
    ): File {
        val doc = PdfDocument()
        val contentW = PAGE_W - MARGIN * 2
        val bottom = PAGE_H - MARGIN - FOOTER_H

        val title = paint(20f, Color.BLACK, bold = true)
        val h2 = paint(13f, Color.rgb(0xC0, 0x5A, 0x1E), bold = true)
        val label = paint(10f, Color.rgb(0x66, 0x66, 0x66), bold = true)
        val body = paint(11f, Color.BLACK)
        val small = paint(9f, Color.rgb(0x66, 0x66, 0x66))
        val warn = paint(11f, Color.rgb(0xB0, 0x20, 0x20), bold = true)
        val line = Paint().apply { color = Color.rgb(0xDD, 0xDD, 0xDD); strokeWidth = 0.8f }

        var pageNo = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = MARGIN.toFloat()

        fun finishPage() {
            val c = canvas ?: return
            val p = page ?: return
            val footer = layout(
                ReportBuilder.DISCLAIMER + "　第 $pageNo 頁", small, contentW
            )
            c.save(); c.translate(MARGIN.toFloat(), (PAGE_H - MARGIN - footer.height + 6).toFloat())
            footer.draw(c); c.restore()
            doc.finishPage(p)
            page = null; canvas = null
        }

        fun newPage() {
            finishPage()
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            canvas = page!!.canvas
            y = MARGIN.toFloat()
        }

        fun ensure(h: Float) { if (page == null || y + h > bottom) newPage() }

        fun block(text: String, p: TextPaint, gapAfter: Float = 6f) {
            val l = layout(text, p, contentW)
            ensure(l.height.toFloat())
            val c = canvas!!
            c.save(); c.translate(MARGIN.toFloat(), y); l.draw(c); c.restore()
            y += l.height + gapAfter
        }

        fun row(lab: String, value: String) {
            val labelW = 130
            val l1 = layout(lab, label, labelW)
            val l2 = layout(value, body, contentW - labelW - 8)
            val h = maxOf(l1.height, l2.height).toFloat()
            ensure(h + 6f)
            val c = canvas!!
            c.save(); c.translate(MARGIN.toFloat(), y); l1.draw(c); c.restore()
            c.save(); c.translate((MARGIN + labelW + 8).toFloat(), y); l2.draw(c); c.restore()
            y += h + 3f
            c.drawLine(MARGIN.toFloat(), y, (PAGE_W - MARGIN).toFloat(), y, line)
            y += 4f
        }

        newPage()
        block("就診前摘要（飼主陳述整理）", title, 4f)
        block("製作時間：${ReportBuilder.formatTime(System.currentTimeMillis())}　｜　PetMed App", small, 10f)

        block("寵物基本資料", h2, 4f)
        row("名字", case.petName.ifBlank { "未提供" })
        row("物種", case.species.ifBlank { "未提供" })
        row("品種", case.breed.ifBlank { "未提供" })
        row("生日", case.birthDate.ifBlank { "未提供" })
        y += 6f

        if (redFlags.isNotEmpty()) {
            block("⚠ 記錄過程中曾出現緊急提示：" + redFlags.joinToString("、") { it.title } + "。已提醒飼主盡快就醫。", warn, 8f)
        }

        if (case.narrative.isNotBlank()) {
            block("摘要（由 AI 依下列飼主填寫的內容整理，未經獸醫審閱）", h2, 3f)
            block(case.narrative, body, 10f)
        }

        sections.forEach { s ->
            ensure(40f)
            block(s.title, h2, 4f)
            s.rows.forEach { r -> row(r.label + if (r.edited) " ※" else "", r.value) }
            y += 6f
        }

        // ───── 每日紀錄 ─────
        val shownLogs = LogUtil.recent(logs, options.days).filterNot { LogUtil.isEmpty(it) }
        if (shownLogs.isNotEmpty()) {
            ensure(40f)
            val range = options.days?.let { "（最近 $it 天）" } ?: "（全部）"
            block("每日紀錄$range", h2, 4f)
            shownLogs.forEach { row(LogUtil.pretty(it.date), LogUtil.summary(it)) }
            y += 6f
        }

        // ───── 照片（飼主選擇才附上）─────
        if (options.includePhotos && photos.isNotEmpty()) {
            ensure(60f)
            block("照片（共 ${photos.size} 張）", h2, 4f)
            val gap = 10f
            val cellW = ((contentW - gap) / 2f)
            photos.chunked(2).forEach { pair ->
                val cells = pair.map { p ->
                    val bmp = PhotoStore.decodeThumb(p.filePath, 700)
                    val h = if (bmp != null) cellW * bmp.height / bmp.width else cellW * 0.75f
                    val caption = buildString {
                        append(ReportBuilder.formatTime(p.takenAt))
                        if (p.topic.isNotBlank()) append("　").append(p.topic)
                        if (p.note.isNotBlank()) append("\n").append(p.note)
                    }
                    Triple(bmp, h, layout(caption, small, cellW.toInt()))
                }
                val rowH = cells.maxOf { it.second + it.third.height + 8f }
                ensure(rowH + 6f)
                val c = canvas!!
                cells.forEachIndexed { i, (bmp, h, cap) ->
                    val x = MARGIN + i * (cellW + gap)
                    if (bmp != null) {
                        c.drawBitmap(bmp, null, RectF(x, y, x + cellW, y + h), Paint(Paint.FILTER_BITMAP_FLAG))
                        bmp.recycle()
                    } else {
                        c.drawRect(RectF(x, y, x + cellW, y + h), Paint().apply { color = Color.rgb(0xEE, 0xEE, 0xEE) })
                    }
                    c.save(); c.translate(x, y + h + 3f); cap.draw(c); c.restore()
                }
                y += rowH + 6f
            }
        }

        block("※＝飼主在記錄完成後修改過的內容", small, 2f)
        block(ReportBuilder.DISCLAIMER, small, 0f)
        finishPage()

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "summary_${case.id}_$stamp.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun layout(text: String, p: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1f)
            .setIncludePad(false)
            .build()
}
