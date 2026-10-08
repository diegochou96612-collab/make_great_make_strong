package com.petmed.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petmed.app.data.model.HealthCase
import com.petmed.app.interview.QType
import com.petmed.app.interview.RedFlag
import com.petmed.app.interview.ReportBuilder
import com.petmed.app.interview.AiAssist
import com.petmed.app.interview.AiKeyStore
import com.petmed.app.interview.AiProvider
import com.petmed.app.ui.components.PhotoAddRow
import com.petmed.app.ui.theme.Brown
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.Cream
import com.petmed.app.ui.theme.CreamDark
import com.petmed.app.ui.theme.Green
import com.petmed.app.ui.theme.Orange
import com.petmed.app.ui.theme.Red
import com.petmed.app.ui.theme.White
import com.petmed.app.ui.viewmodel.InterviewViewModel
import com.petmed.app.ui.viewmodel.Msg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterviewScreen(
    onBack: () -> Unit = {},
    onOpenCase: (Long) -> Unit = {},
    openCaseId: Long? = null,
    vm: InterviewViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    val cases by vm.cases.collectAsState()
    var input by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAiSettings by remember { mutableStateOf(false) }

    if (showAiSettings) AiSettingsDialog(onDismiss = { showAiSettings = false })

    // 第一次出現時才開始（避免從報告頁返回時把對話重置）；openCaseId 代表開啟既有紀錄來補充或更正
    LaunchedEffect(Unit) { vm.enter(openCaseId) }

    LaunchedEffect(ui.messages.size) {
        if (ui.messages.isNotEmpty()) listState.animateScrollToItem(ui.messages.size - 1)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp), drawerContainerColor = Cream) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Orange)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("狀況紀錄", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { vm.start(allowOffer = false); scope.launch { drawerState.close() } }) {
                            Icon(Icons.Default.Add, contentDescription = "新的狀況紀錄", tint = White)
                        }
                    }
                    if (cases.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("還沒有紀錄", color = BrownLight, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(cases) { c ->
                                CaseRow(c) {
                                    scope.launch { drawerState.close() }
                                    // 還沒問完的紀錄：直接接續問診；已完成的：看摘要與報告
                                    if (c.status == HealthCase.STATUS_DONE) onOpenCase(c.id) else vm.resume(c.id)
                                }
                                HorizontalDivider(color = CreamDark, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Cream)
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Orange)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "狀況紀錄", tint = White)
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AI", color = Orange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (ui.chatMode) "補充與修改" else "AI 記錄助理", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    if (ui.total > 0 && !ui.finished) {
                        Text("第 ${ui.answered + 1} 題・約共 ${ui.total} 題", color = White.copy(alpha = 0.85f), fontSize = 11.sp)
                    }
                }
                IconButton(onClick = { showAiSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "AI 設定", tint = White)
                }
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = White)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ui.messages) { m ->
                    when (m) {
                        is Msg.Bot -> Bubble(m.text, fromUser = false)
                        is Msg.User -> Bubble(m.text, fromUser = true)
                        is Msg.Alert -> AlertCard(m.flag)
                    }
                }
                if (ui.busy) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Orange, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("處理中…", color = BrownLight, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ───── 作答區 ─────
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(White)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val q = ui.current
                val offer = ui.resumeOffer
                if (offer != null) {
                    Text("你有一份還沒完成的紀錄", color = Brown, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "「${offer.title.ifBlank { "（未命名）" }}」，已回答 ${offer.answered} 題，最後更新 ${ReportBuilder.formatTime(offer.updatedAt)}。要接續嗎？",
                        color = BrownLight, fontSize = 12.sp, lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.resume(offer.caseId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Orange)
                    ) { Text("接續上次的紀錄", color = White) }
                    TextButton(onClick = { vm.declineResume() }, modifier = Modifier.fillMaxWidth()) {
                        Text("不接續，開始新的紀錄", color = BrownLight)
                    }
                } else if (ui.finished && ui.caseId != null) {
                    Button(
                        onClick = { onOpenCase(ui.caseId!!) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Orange)
                    ) { Text("查看摘要與報告", color = White) }
                    if (!ui.busy) {
                        Spacer(Modifier.height(6.dp))
                        ChatInputField(
                            value = input, onValueChange = { input = it },
                            placeholder = "補充、更正，或問我怎麼記錄…",
                            onSend = { vm.answerText(input.text); input = TextFieldValue("") }
                        )
                    }
                } else if (q != null && !ui.busy) {
                    if (q.hint.isNotBlank()) Text(q.hint, color = BrownLight, fontSize = 11.sp)
                    val cid = ui.caseId
                    if (q.photoHint.isNotBlank() && cid != null) {
                        PhotoAddRow(
                            caseId = cid,
                            hint = "要不要拍一張「${q.photoHint}」？近拍和全景各一張、光線充足。這是方便獸醫參考，不是診斷；不想拍可以略過。",
                            defaultTopic = q.photoTopic,
                            onSave = { file, note, topic -> vm.addPhoto(file, note, topic) }
                        )
                    }
                    if (q.options.isNotEmpty()) {
                        Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                q.options.forEach { opt ->
                                    val selected = opt in ui.picked
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            if (q.type == QType.MULTI) vm.toggle(opt) else vm.confirmChoice(opt)
                                        },
                                        label = { Text(opt, fontSize = 13.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Orange.copy(alpha = 0.2f),
                                            selectedLabelColor = Brown
                                        )
                                    )
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (q.type == QType.MULTI) {
                            Button(
                                onClick = { vm.confirmChoice() },
                                enabled = ui.picked.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = Orange)
                            ) { Text("確定", color = White) }
                            Spacer(Modifier.width(8.dp))
                        }
                        if (q.id != "pet_pick" && q.id != "species_pick") {
                            TextButton(onClick = { vm.answerUnknown() }) { Text("不確定", color = BrownLight) }
                        }
                        if (ui.canFinishEarly) {
                            TextButton(onClick = { vm.finishEarly() }) { Text("先到這邊，幫我整理", color = BrownLight) }
                        }
                    }
                    ChatInputField(
                        value = input, onValueChange = { input = it },
                        placeholder = if (q.options.isEmpty()) "輸入你的回答，或直接說想說的…" else "或直接用自己的話說…",
                        onSend = { vm.answerText(input.text); input = TextFieldValue("") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    onSend: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = BrownLight, fontSize = 14.sp) },
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Orange, unfocusedBorderColor = CreamDark,
            focusedContainerColor = Cream, unfocusedContainerColor = Cream
        ),
        trailingIcon = {
            IconButton(onClick = { if (value.text.isNotBlank()) onSend() }, enabled = value.text.isNotBlank()) {
                Icon(
                    Icons.AutoMirrored.Filled.Send, contentDescription = "送出",
                    tint = if (value.text.isNotBlank()) Orange else Color.Transparent
                )
            }
        }
    )
}

/** AI 設定：選供應商、貼金鑰、改模型、測試連線。金鑰過期或模型被下架時不必重新 Build。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var provider by remember { mutableStateOf(AiKeyStore.savedProvider(context)) }
    var key by remember(provider) { mutableStateOf(AiKeyStore.savedKey(context, provider)) }
    var model by remember(provider) { mutableStateOf(AiKeyStore.savedModel(context, provider)) }
    var url by remember(provider) { mutableStateOf(AiKeyStore.savedUrl(context, provider)) }
    var show by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val problem = AiKeyStore.formatProblem(key, provider)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 設定", fontSize = 16.sp) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "AI 只用來把你打的字對應到選項，以及整理摘要。AI 失效時，記錄照常運作。",
                    color = BrownLight, fontSize = 11.sp, lineHeight = 16.sp
                )
                Spacer(Modifier.height(8.dp))
                Text("供應商", color = BrownLight, fontSize = 11.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AiProvider.values().forEach { p ->
                        FilterChip(selected = provider == p, onClick = { provider = p; status = null }, label = { Text(p.label, fontSize = 12.sp) })
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    when (provider) {
                        AiProvider.ANTHROPIC -> "建議使用。到 Anthropic 的網站建立帳號、儲值並建立金鑰（sk-ant- 開頭）。金額與方案請以網站顯示為準。預設模型 claude-sonnet-5-5，官方承諾不早於 2027/9/28 退役。"
                        AiProvider.GEMINI -> "Google 的 AI，官方文件列出有免費額度的模型。到 aistudio.google.com 的 API keys 頁面建立金鑰。如果測試時出現 limit: 0，代表這個模型在你的帳號沒有免費額度，請換模型名稱再試。"
                        AiProvider.GROQ -> "免費，但金鑰會自己過期，需要定期重新建立。建立時如果有到期時間選項，請選最長。"
                        AiProvider.CUSTOM -> "任何 OpenAI 相容的服務，請自行填網址與模型名稱。"
                    },
                    color = BrownLight, fontSize = 11.sp, lineHeight = 16.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = key, onValueChange = { key = it; status = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("金鑰" + if (provider.keyPrefix.isNotEmpty()) "（${provider.keyPrefix} 開頭）" else "") },
                    singleLine = true,
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation()
                )
                TextButton(onClick = { show = !show }) { Text(if (show) "隱藏金鑰" else "顯示金鑰", color = BrownLight) }
                if (problem != null) Text(problem, color = Red, fontSize = 11.sp)
                OutlinedTextField(
                    value = model, onValueChange = { model = it; status = null },
                    modifier = Modifier.fillMaxWidth(), label = { Text("模型名稱") }, singleLine = true
                )
                if (provider == AiProvider.CUSTOM) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = url, onValueChange = { url = it; status = null },
                        modifier = Modifier.fillMaxWidth(), label = { Text("完整網址（結尾是 /chat/completions）") }
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        enabled = !testing,
                        onClick = {
                            if (problem != null) { status = problem; return@TextButton }
                            AiKeyStore.save(context, provider, key, model, url)
                            testing = true; status = "測試中…"
                            scope.launch {
                                status = withContext(Dispatchers.IO) { AiAssist.ping() }
                                testing = false
                            }
                        }
                    ) { Text("儲存並測試連線", color = Orange) }
                    if (testing) CircularProgressIndicator(Modifier.size(18.dp), color = Orange, strokeWidth = 2.dp)
                }
                status?.let { Text(it, color = if (it.startsWith("連線成功")) Green else Red, fontSize = 12.sp, lineHeight = 17.sp) }
                Spacer(Modifier.height(6.dp))
                Text(
                    "金鑰只存在這支手機的 App 裡，僅適合示範使用。",
                    color = BrownLight, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("關閉", color = Orange) } }
    )
}

@Composable
private fun Bubble(text: String, fromUser: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (fromUser) 16.dp else 4.dp, topEnd = if (fromUser) 4.dp else 16.dp,
                        bottomStart = 16.dp, bottomEnd = 16.dp
                    )
                )
                .background(if (fromUser) Orange else Green)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(text, color = White, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun AlertCard(flag: RedFlag) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFEBEE))
            .padding(1.dp)
            .background(White, RoundedCornerShape(13.dp))
            .padding(14.dp)
    ) {
        Column {
            Text("⚠ ${flag.title}", color = Red, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(flag.guidance, color = Brown, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(6.dp))
            Text("依據：${flag.source}", color = BrownLight, fontSize = 10.sp)
            Text(RedFlag.DISCLAIMER, color = BrownLight, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun CaseRow(c: HealthCase, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            c.title.ifBlank { "（未命名）" }, color = Brown, fontSize = 14.sp,
            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        val status = if (c.status == HealthCase.STATUS_DONE) "已完成" else "未完成・點此繼續"
        Text(
            "${c.petName.ifBlank { "未指定寵物" }}・${ReportBuilder.formatTime(c.updatedAt)}・$status",
            color = BrownLight, fontSize = 12.sp
        )
    }
}
