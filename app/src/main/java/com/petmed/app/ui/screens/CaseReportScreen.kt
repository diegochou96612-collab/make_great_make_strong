package com.petmed.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petmed.app.data.model.CaseField
import com.petmed.app.data.model.DailyLog
import com.petmed.app.interview.DailyLogSpec
import com.petmed.app.interview.InterviewBank
import com.petmed.app.interview.LogUtil
import com.petmed.app.interview.MULTI_SEP
import com.petmed.app.interview.PdfExporter
import com.petmed.app.interview.QType
import com.petmed.app.interview.ReportBuilder
import com.petmed.app.interview.VideoCatalog
import com.petmed.app.ui.components.PhotoThumb
import com.petmed.app.ui.theme.Brown
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.Cream
import com.petmed.app.ui.theme.CreamDark
import com.petmed.app.ui.theme.Orange
import com.petmed.app.ui.theme.Red
import com.petmed.app.ui.theme.White
import com.petmed.app.ui.viewmodel.CaseReportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaseReportScreen(
    caseId: Long,
    onBack: () -> Unit = {},
    onOpenPhotos: () -> Unit = {},
    onContinueChat: () -> Unit = {},
    vm: CaseReportViewModel = viewModel()
) {
    // 必須用 remember 固定 Flow 實例，否則每次重組都會建立新的 Flow 並重新收集
    val caseFlow = remember(caseId) { vm.observeCase(caseId) }
    val fieldsFlow = remember(caseId) { vm.observeFields(caseId) }
    val logsFlow = remember(caseId) { vm.observeLogs(caseId) }
    val photosFlow = remember(caseId) { vm.observePhotos(caseId) }
    val case by caseFlow.collectAsState(initial = null)
    val fields by fieldsFlow.collectAsState(initial = emptyList())
    val logs by logsFlow.collectAsState(initial = emptyList())
    val photos by photosFlow.collectAsState(initial = emptyList())
    val events by vm.events.collectAsState()
    val busy by vm.busy.collectAsState()
    val message by vm.lastMessage.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf<CaseField?>(null) }
    var editingLog by remember { mutableStateOf<DailyLog?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var exportDays by remember { mutableStateOf<Int?>(7) }
    var exportPhotos by remember { mutableStateOf(false) }

    LaunchedEffect(caseId) { vm.loadEvents(caseId) }

    val sections = ReportBuilder.sections(fields)
    val flags = ReportBuilder.redFlags(events)
    val selectedCategories = fields.firstOrNull { it.fieldKey == "categories" }
        ?.value?.split(MULTI_SEP)?.map { it.trim() }?.toSet() ?: emptySet()

    Column(Modifier.fillMaxSize().background(Cream)) {
        Row(
            Modifier.fillMaxWidth().background(Orange).statusBarsPadding().padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = White)
            }
            Text("就診前摘要", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(enabled = !exporting && case != null, onClick = { showExport = true }) {
                Icon(Icons.Default.Share, contentDescription = "匯出 PDF 並分享", tint = White)
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Default.Delete, contentDescription = "刪除", tint = White)
            }
        }

        val c = case
        if (c == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("找不到這份紀錄", color = BrownLight, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card2 {
                        Text("寵物基本資料", color = Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        InfoLine("名字", c.petName.ifBlank { "未提供" })
                        InfoLine("物種", c.species.ifBlank { "未提供" })
                        InfoLine("品種", c.breed.ifBlank { "未提供" })
                        InfoLine("生日", c.birthDate.ifBlank { "未提供" })
                        InfoLine("紀錄時間", ReportBuilder.formatTime(c.createdAt))
                    }
                }
                if (flags.isNotEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFEBEE)).padding(12.dp)) {
                            Text(
                                "⚠ 記錄過程中曾出現緊急提示：" + flags.joinToString("、") { it.title } + "。已提醒你盡快就醫。",
                                color = Red, fontSize = 13.sp, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                item {
                    Card2 {
                        Text("摘要", color = Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        if (c.narrative.isNotBlank()) {
                            Text(c.narrative, color = Brown, fontSize = 14.sp, lineHeight = 21.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("由 AI 依下方飼主填寫的內容整理，未經獸醫審閱。", color = BrownLight, fontSize = 11.sp)
                        } else {
                            Text(
                                "目前沒有 AI 摘要。修改內容後，舊的摘要會被清除，避免和內容不一致。",
                                color = BrownLight, fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        if (busy) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(18.dp), color = Orange, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("整理中…", color = BrownLight, fontSize = 12.sp)
                            }
                        } else {
                            OutlinedButton(onClick = { vm.regenerateNarrative(caseId) }) {
                                Text(if (c.narrative.isBlank()) "產生 AI 摘要" else "重新產生摘要", color = Orange)
                            }
                        }
                        message?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, color = Red, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = CreamDark, thickness = 0.5.dp)
                        Spacer(Modifier.height(8.dp))
                        Text("想補充或更正說法？直接跟 AI 說，紀錄與摘要會跟著更新。", color = BrownLight, fontSize = 12.sp)
                        Button(onClick = onContinueChat, colors = ButtonDefaults.buttonColors(containerColor = Orange)) {
                            Text("繼續對話補充或修改", color = White)
                        }
                    }
                }

                // ───── 每日紀錄 ─────
                item {
                    Card2 {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("每日紀錄", color = Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                val today = LogUtil.today()
                                editingLog = logs.firstOrNull { it.date == today } ?: DailyLog(caseId = caseId, date = today)
                            }) { Text("＋ 記錄今天", color = Orange) }
                        }
                        val shown = LogUtil.recent(logs, 7)
                        if (shown.isEmpty()) {
                            Text(
                                "還沒有每日紀錄。連續幾天記下食慾、喝水、大便、小便、精神，獸醫會更容易看出變化。",
                                color = BrownLight, fontSize = 12.sp, lineHeight = 17.sp
                            )
                        } else {
                            shown.forEachIndexed { i, l ->
                                if (i > 0) HorizontalDivider(color = CreamDark, thickness = 0.5.dp)
                                Column(Modifier.fillMaxWidth().clickable { editingLog = l }.padding(vertical = 8.dp)) {
                                    Text(LogUtil.pretty(l.date), color = Brown, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(LogUtil.summary(l), color = BrownLight, fontSize = 12.sp, lineHeight = 17.sp)
                                }
                            }
                            if (logs.size > shown.size) {
                                Text("僅顯示最近 7 天；匯出 PDF 時可選擇天數。", color = BrownLight, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // ───── 照片相簿 ─────
                item {
                    Card2 {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("照片相簿（${photos.size}）", color = Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = onOpenPhotos) { Text(if (photos.isEmpty()) "＋ 新增照片" else "管理相簿", color = Orange) }
                        }
                        if (photos.isEmpty()) {
                            Text("把這次狀況的照片放在一起，獸醫看診時可以一起參考。", color = BrownLight, fontSize = 12.sp)
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                photos.takeLast(4).forEach { p ->
                                    PhotoThumb(
                                        p.filePath,
                                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).clickable { onOpenPhotos() },
                                        maxEdge = 240
                                    )
                                }
                                repeat(4 - photos.takeLast(4).size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                // ───── 衛教影片（人工審核清單）─────
                val videos = VideoCatalog.forCase(selectedCategories, c.species)
                if (videos.isNotEmpty()) {
                    item {
                        Card2 {
                            Text("相關衛教影片", color = Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("以下影片由人工查證存在並審核，不是 AI 產生的連結。", color = BrownLight, fontSize = 11.sp)
                            videos.forEach { v ->
                                Spacer(Modifier.height(8.dp))
                                Text(v.title, color = Brown, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("${v.channel}・${v.language}", color = BrownLight, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(v.about, color = Brown, fontSize = 12.sp, lineHeight = 17.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("⚠ ${v.caution}", color = Red, fontSize = 11.sp, lineHeight = 16.sp)
                                OutlinedButton(onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(v.url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                                }) { Text("前往觀看", color = Orange) }
                            }
                        }
                    }
                }

                sections.forEach { s ->
                    item {
                        Card2 {
                            Text(s.title, color = Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            s.rows.forEachIndexed { i, r ->
                                if (i > 0) HorizontalDivider(color = CreamDark, thickness = 0.5.dp)
                                Row(
                                    Modifier.fillMaxWidth().clickable { editing = fields.firstOrNull { it.id == r.fieldId } }.padding(vertical = 8.dp)
                                ) {
                                    Text(r.label + if (r.edited) " ※" else "", color = BrownLight, fontSize = 12.sp, modifier = Modifier.width(112.dp))
                                    Text(r.value, color = if (r.missing) BrownLight else Brown, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                item {
                    Text(
                        "點任一項可以修改。※＝飼主修改過。\n" + ReportBuilder.DISCLAIMER,
                        color = BrownLight, fontSize = 11.sp, lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // ───── 匯出選項 ─────
    if (showExport) {
        AlertDialog(
            onDismissRequest = { showExport = false },
            title = { Text("匯出 PDF", fontSize = 16.sp) },
            text = {
                Column {
                    Text("每日紀錄要放最近幾天？", color = BrownLight, fontSize = 12.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf<Pair<String, Int?>>("3 天" to 3, "7 天" to 7, "14 天" to 14, "全部" to null).forEach { (label, d) ->
                            FilterChip(selected = exportDays == d, onClick = { exportDays = d }, label = { Text(label, fontSize = 12.sp) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = photos.isNotEmpty()) { exportPhotos = !exportPhotos },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = exportPhotos && photos.isNotEmpty(), onCheckedChange = { exportPhotos = it }, enabled = photos.isNotEmpty())
                        Text(
                            if (photos.isEmpty()) "附上照片（相簿目前沒有照片）" else "附上照片（${photos.size} 張）",
                            color = Brown, fontSize = 14.sp
                        )
                    }
                    Text("照片會讓檔案變大。不附也可以，獸醫仍能看到完整的文字紀錄。", color = BrownLight, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExport = false
                    scope.launch {
                        exporting = true
                        try {
                            val data = vm.exportData(caseId)
                            if (data != null) {
                                val file = withContext(Dispatchers.IO) {
                                    PdfExporter.export(
                                        context, data.case, ReportBuilder.sections(data.fields), ReportBuilder.redFlags(data.events),
                                        data.logs, data.photos,
                                        PdfExporter.ExportOptions(exportDays, exportPhotos && data.photos.isNotEmpty())
                                    )
                                }
                                vm.recordExport(caseId)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(send, "分享就診前摘要"))
                            }
                        } finally {
                            exporting = false
                        }
                    }
                }) { Text("匯出並分享", color = Orange) }
            },
            dismissButton = { TextButton(onClick = { showExport = false }) { Text("取消", color = BrownLight) } }
        )
    }

    // ───── 修改欄位 ─────
    editing?.let { f ->
        val question = InterviewBank.get(f.fieldKey)
        var text by remember(f.id) { mutableStateOf(if (f.state == CaseField.STATE_ANSWERED) f.value else "") }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(f.label, fontSize = 16.sp) },
            text = {
                Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                    if (question != null && question.options.isNotEmpty()) {
                        Text("快速選擇", color = BrownLight, fontSize = 11.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            question.options.forEach { opt ->
                                val picked = text.split(MULTI_SEP).map { it.trim() }
                                FilterChip(
                                    selected = opt in picked,
                                    onClick = {
                                        text = if (question.type == QType.MULTI) {
                                            val set = picked.filter { it.isNotEmpty() }.toMutableList()
                                            if (opt in set) set.remove(opt) else set.add(opt)
                                            set.joinToString(MULTI_SEP)
                                        } else opt
                                    },
                                    label = { Text(opt, fontSize = 12.sp) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), label = { Text("內容") })
                    Spacer(Modifier.height(4.dp))
                    Text("清空代表「未提供」。修改會留下紀錄。", color = BrownLight, fontSize = 11.sp)
                }
            },
            confirmButton = { TextButton(onClick = { vm.editField(f, text); editing = null }) { Text("儲存", color = Orange) } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("取消", color = BrownLight) } }
        )
    }

    // ───── 每日紀錄編輯 ─────
    editingLog?.let { l ->
        DailyLogDialog(
            initial = l,
            isNew = logs.none { it.id == l.id && l.id != 0L },
            existingDates = logs.map { it.date }.toSet(),
            onSave = { vm.saveLog(it); editingLog = null },
            onDelete = { vm.deleteLog(l); editingLog = null },
            onDismiss = { editingLog = null }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("刪除這份紀錄？") },
            text = { Text("刪除後無法復原，包含記錄內容、修改紀錄、每日紀錄和相簿的所有照片。") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(caseId) { onBack() } }) { Text("刪除", color = Red) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消", color = BrownLight) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyLogDialog(
    initial: DailyLog,
    isNew: Boolean,
    existingDates: Set<String>,
    onSave: (DailyLog) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var log by remember(initial.id, initial.date) { mutableStateOf(initial) }
    val dateChoices = listOf(0, 1, 2).map { LogUtil.daysAgo(it) }

    @Composable
    fun choice(title: String, options: List<String>, value: String, set: (String) -> Unit) {
        Text(title, color = BrownLight, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            options.forEach { o ->
                FilterChip(selected = value == o, onClick = { set(if (value == o) "" else o) }, label = { Text(o, fontSize = 12.sp) })
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("每日紀錄", fontSize = 16.sp) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                if (isNew) {
                    Text("日期", color = BrownLight, fontSize = 11.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dateChoices.forEachIndexed { i, d ->
                            val label = listOf("今天", "昨天", "前天")[i] + if (d in existingDates) "（已有）" else ""
                            FilterChip(selected = log.date == d, onClick = { log = log.copy(date = d) }, label = { Text(label, fontSize = 12.sp) })
                        }
                    }
                } else {
                    Text(LogUtil.pretty(log.date), color = Brown, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(6.dp))
                choice("食慾", DailyLogSpec.appetite, log.appetite) { log = log.copy(appetite = it) }
                choice("喝水", DailyLogSpec.water, log.water) { log = log.copy(water = it) }
                choice("大便", DailyLogSpec.stool, log.stool) { log = log.copy(stool = it) }
                choice("小便", DailyLogSpec.urine, log.urine) { log = log.copy(urine = it) }
                choice("精神與活動力", DailyLogSpec.energy, log.energy) { log = log.copy(energy = it) }
                choice("睡眠", DailyLogSpec.sleep, log.sleep) { log = log.copy(sleep = it) }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = log.vomitCount, onValueChange = { log = log.copy(vomitCount = it.filter { c -> c.isDigit() }.take(3)) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("今天嘔吐幾次（沒有就留空）") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = log.weight, onValueChange = { log = log.copy(weight = it.take(8)) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("體重（例如 4.2 kg，沒量就留空）") }, singleLine = true
                )
                OutlinedTextField(
                    value = log.note, onValueChange = { log = log.copy(note = it) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("備註") }
                )
                Spacer(Modifier.height(4.dp))
                Text("只填你觀察得到的。沒填的欄位不會出現在報告裡。", color = BrownLight, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(log) }) { Text("儲存", color = Orange) } },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = onDelete) { Text("刪除", color = Red) }
                TextButton(onClick = onDismiss) { Text("取消", color = BrownLight) }
            }
        }
    )
}

@Composable
private fun Card2(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(White).padding(horizontal = 16.dp, vertical = 12.dp)) { content() }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = BrownLight, fontSize = 12.sp, modifier = Modifier.width(72.dp))
        Text(value, color = Brown, fontSize = 14.sp)
    }
}
