package com.petmed.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import com.petmed.app.ui.theme.GreenDark
import com.petmed.app.ui.theme.OrangeDark
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petmed.app.data.model.VaccineRecord
import com.petmed.app.interview.DateUtil
import com.petmed.app.interview.GuideItem
import com.petmed.app.interview.GuideSpecies
import com.petmed.app.interview.GuideStatus
import com.petmed.app.interview.ItemState
import com.petmed.app.interview.VaccineGuide
import com.petmed.app.ui.components.WheelDatePickerDialog
import com.petmed.app.ui.theme.Blue
import com.petmed.app.ui.theme.Brown
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.Cream
import com.petmed.app.ui.theme.CreamDark
import com.petmed.app.ui.theme.Green
import com.petmed.app.ui.theme.Orange
import com.petmed.app.ui.theme.Red
import com.petmed.app.ui.theme.White
import com.petmed.app.ui.viewmodel.VaccineViewModel
import com.petmed.app.ui.viewmodel.toDoseRecords

private val BlueDark = Color(0xFF4A90B0)

private val TAB_LABELS = listOf("手冊", "紀錄", "問 AI")

/** 寵物手冊：依年齡列出可以打的疫苗與做的檢測，可記錄已打項目，也可以問 AI。不推播通知。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VaccineGuideScreen(
    onBack: () -> Unit = {},
    vm: VaccineViewModel = viewModel()
) {
    val pets by vm.pets.collectAsState()
    val pet by vm.selectedPet.collectAsState()
    val species by vm.species.collectAsState()
    val records by vm.records.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    /** null＝沒開對話框；空字串＝讓飼主自己選項目；其他＝已指定項目 */
    var dialogKey by remember { mutableStateOf<String?>(null) }

    val today = remember { DateUtil.today() }
    val birth = remember(pet?.birthDate) { pet?.birthDate?.let { DateUtil.parse(it) } }
    val doses = remember(records) { records.toDoseRecords() }
    val states = remember(species, birth, doses) {
        val sp = species
        val b = birth
        if (sp != null && b != null) VaccineGuide.evaluateAll(sp, b, today, doses) else emptyList()
    }

    Column(Modifier.fillMaxSize().background(Cream)) {
        Row(
            Modifier.fillMaxWidth().background(Orange).statusBarsPadding().padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = White)
            }
            Text("寵物手冊：疫苗與檢測", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        val p = pet
        if (p == null) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("還沒有寵物資料。\n請先到「個人」新增寵物（要填種類與生日），手冊才能依年齡整理。",
                    color = BrownLight, fontSize = 14.sp, lineHeight = 21.sp)
            }
            return@Column
        }

        if (pets.size > 1) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pets.forEach { x ->
                    FilterChip(selected = x.id == p.id, onClick = { vm.selectPet(x.id) }, label = { Text(x.name) })
                }
            }
        }

        val sp = species
        if (sp == null) {
            Column(Modifier.padding(16.dp).clip(RoundedCornerShape(14.dp)).background(White).padding(16.dp)) {
                Text("${p.name} 的種類是「${p.species.ifBlank { "未填" }}」", color = Brown, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text("手冊目前只有狗和貓。如果牠是狗或貓，請選一個：", color = BrownLight, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GuideSpecies.values().forEach { s ->
                        FilterChip(selected = false, onClick = { vm.pickSpecies(p.id, s) }, label = { Text(s.label) })
                    }
                }
            }
            return@Column
        }

        TabRow(selectedTabIndex = tab, containerColor = White, contentColor = Orange) {
            TAB_LABELS.forEachIndexed { i, label ->
                Tab(selected = i == tab, onClick = { tab = i }, text = { Text(label) })
            }
        }

        when (tab) {
            0 -> HandbookTab(
                modifier = Modifier.weight(1f),
                petName = p.name,
                birth = birth,
                today = today,
                states = states,
                items = VaccineGuide.itemsFor(sp),
                onRecord = { dialogKey = it }
            )
            1 -> RecordsTab(
                modifier = Modifier.weight(1f),
                records = records,
                onAdd = { dialogKey = "" },
                onDelete = { vm.deleteRecord(it) }
            )
            else -> AskTab(Modifier.weight(1f), vm)
        }

        val key = dialogKey
        if (key != null) {
            RecordDialog(
                items = VaccineGuide.itemsFor(sp),
                presetKey = key.ifEmpty { null },
                onConfirm = { itemKey, date ->
                    vm.addRecord(p.id, itemKey, date)
                    dialogKey = null
                },
                onDismiss = { dialogKey = null }
            )
        }
    }
}

// ───── 手冊 ─────

@Composable
private fun HandbookTab(
    modifier: Modifier,
    petName: String,
    birth: Long?,
    today: Long,
    states: List<ItemState>,
    items: List<GuideItem>,
    onRecord: (String) -> Unit
) {
    val stateByKey = remember(states) { states.associateBy { it.item.key } }
    val attention = remember(states) { VaccineGuide.attention(states) }
    var introKey by remember { mutableStateOf<String?>(null) }
    var expandedKey by remember { mutableStateOf<String?>(null) }
    var aboutOpen by remember { mutableStateOf(false) }
    val topics = remember(items) { items.firstOrNull()?.let { VaccineGuide.topicsFor(it.species) } ?: emptyList() }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 頂部：寵物與年齡
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Orange).padding(horizontal = 22.dp, vertical = 20.dp)) {
            Text(petName, color = White.copy(alpha = 0.9f), fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                if (birth != null) VaccineGuide.ageText(birth, today) else "還沒有生日",
                color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold
            )
            if (birth == null) {
                Spacer(Modifier.height(6.dp))
                Text("到「個人」編輯寵物資料，填上生日後，就能算出時間。", color = White, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
        Text(
            if (aboutOpen) "關於這份手冊　收起" else "關於這份手冊　展開",
            color = BrownLight, fontSize = 12.sp,
            modifier = Modifier.clickable { aboutOpen = !aboutOpen }.padding(horizontal = 6.dp, vertical = 2.dp)
        )
        if (aboutOpen) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(White).padding(16.dp)) {
                Text(VaccineGuide.TAIPEI_NOTE, color = BrownLight, fontSize = 12.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(8.dp))
                Text(VaccineGuide.DISCLAIMER, color = BrownLight, fontSize = 12.sp, lineHeight = 19.sp)
            }
        }

        // 名詞介紹：橫向滑動的小卡
        SectionTitle("先看懂這些名詞", "點一下看介紹")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val tints = listOf(Blue, Green, Orange)
            topics.forEachIndexed { i, t ->
                val tint = tints[i % tints.size]
                Column(
                    Modifier.width(176.dp).clip(RoundedCornerShape(20.dp)).background(tint.copy(alpha = 0.16f))
                        .clickable { introKey = t.key }.padding(16.dp)
                ) {
                    Text(t.title, color = Brown, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, minLines = 2)
                    Spacer(Modifier.height(6.dp))
                    Text(t.oneLine, color = BrownLight, fontSize = 12.sp, lineHeight = 18.sp, maxLines = 3, minLines = 3)
                }
            }
        }

        // 現在可以留意
        if (birth != null) {
            Spacer(Modifier.height(10.dp))
            SectionTitle("現在可以留意")
            if (attention.isEmpty()) {
                Text("目前沒有需要安排的項目。", color = BrownLight, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
            } else {
                attention.forEach { s ->
                    ItemCard(
                        s.item, s, today, highlight = true,
                        expanded = expandedKey == "a:" + s.item.key,
                        onToggle = { expandedKey = if (expandedKey == "a:" + s.item.key) null else "a:" + s.item.key },
                        onRecord = onRecord, onIntro = { introKey = it }
                    )
                }
            }
        }

        val young = items.filter { it.repeatDays == null && it.startWeek < 52 }.sortedBy { it.startWeek }
        val regular = items.filter { it !in young }.sortedBy { it.startWeek }

        Spacer(Modifier.height(10.dp))
        SectionTitle("幼年階段", "出生到約 1 歲")
        TimelineList(young, stateByKey, today, expandedKey, { expandedKey = it }, onRecord) { introKey = it }

        Spacer(Modifier.height(10.dp))
        SectionTitle("成年後的定期項目", "之後每隔一段時間")
        TimelineList(regular, stateByKey, today, expandedKey, { expandedKey = it }, onRecord) { introKey = it }

        Spacer(Modifier.height(16.dp))
    }

    introKey?.let { k ->
        VaccineGuide.topics.firstOrNull { it.key == k }?.let { t ->
            AlertDialog(
                onDismissRequest = { introKey = null },
                containerColor = White,
                shape = RoundedCornerShape(24.dp),
                title = { Text(t.title, color = Brown, fontWeight = FontWeight.Bold, fontSize = 19.sp) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(t.oneLine, color = OrangeDark, fontSize = 14.sp, lineHeight = 21.sp)
                        IntroBlock("預防什麼", t.protects)
                        IntroBlock("為什麼要打／做", t.why)
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cream).padding(14.dp)) {
                            Text("去診所可以這樣問", color = BrownLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("「${t.ask}」", color = Brown, fontSize = 14.sp, lineHeight = 21.sp)
                        }
                        Text("以上為一般性整理，實際請以獸醫的判斷為準。", color = BrownLight, fontSize = 11.sp)
                    }
                },
                confirmButton = { TextButton(onClick = { introKey = null }) { Text("知道了", color = Orange, fontWeight = FontWeight.Bold) } }
            )
        }
    }
}

@Composable
private fun IntroBlock(label: String, body: String) {
    Column {
        Text(label, color = BrownLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(body, color = Brown, fontSize = 14.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun SectionTitle(text: String, sub: String? = null) {
    Column(Modifier.padding(horizontal = 4.dp)) {
        Text(text, color = Brown, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (sub != null) Text(sub, color = BrownLight, fontSize = 12.sp)
    }
}

private fun statusColor(s: GuideStatus): Color = when (s) {
    GuideStatus.DONE -> GreenDark
    GuideStatus.DUE -> OrangeDark
    GuideStatus.SOON -> Blue
    GuideStatus.LATER -> BrownLight
    GuideStatus.PAST -> Red
    GuideStatus.OPTIONAL -> BrownLight
}

/** 左邊一條時間軸，右邊是卡片 */
@Composable
private fun TimelineList(
    list: List<GuideItem>,
    stateByKey: Map<String, ItemState>,
    today: Long,
    expandedKey: String?,
    onExpand: (String?) -> Unit,
    onRecord: (String) -> Unit,
    onIntro: (String) -> Unit
) {
    Column {
        list.forEachIndexed { i, item ->
            val st = stateByKey[item.key]
            val dot = st?.let { statusColor(it.status) } ?: CreamDark
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Column(Modifier.width(22.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    Box(Modifier.size(12.dp).clip(CircleShape).background(dot))
                    if (i < list.lastIndex) Box(Modifier.width(2.dp).weight(1f).background(CreamDark))
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f).padding(bottom = 12.dp)) {
                    ItemCard(
                        item, st, today, highlight = false,
                        expanded = expandedKey == item.key,
                        onToggle = { onExpand(if (expandedKey == item.key) null else item.key) },
                        onRecord = onRecord, onIntro = onIntro
                    )
                }
            }
        }
    }
}

@Composable
private fun PillButton(text: String, filled: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (filled) color else color.copy(alpha = 0.12f))
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(text, color = if (filled) White else color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ItemCard(
    item: GuideItem,
    state: ItemState?,
    today: Long,
    highlight: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRecord: (String) -> Unit,
    onIntro: (String) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(if (highlight) Color(0xFFFFF1E6) else White)
            .clickable(onClick = onToggle)
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, color = Brown, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(
                    "${item.kind.label}　${item.ageText}${if (item.optional) "　視情況" else ""}",
                    color = BrownLight, fontSize = 12.sp
                )
            }
            if (state != null) {
                Spacer(Modifier.width(10.dp))
                val c = statusColor(state.status)
                Text(
                    state.status.label, color = c, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(c.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        if (state != null && (highlight || expanded)) {
            Spacer(Modifier.height(8.dp))
            Text(VaccineGuide.describe(state, today), color = OrangeDark, fontSize = 13.sp)
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Text(item.desc, color = Brown, fontSize = 13.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(8.dp))
            Text("依據：${item.source}", color = BrownLight, fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("這是什麼？", filled = false, color = BlueDark) { onIntro(VaccineGuide.topicKeyOf(item)) }
                PillButton(if (item.kind.label == "檢測") "記錄已做" else "記錄已打", filled = true, color = Orange) { onRecord(item.key) }
            }
        }
    }
}

// ───── 紀錄 ─────

@Composable
private fun RecordsTab(
    modifier: Modifier,
    records: List<VaccineRecord>,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit
) {
    var deleting by remember { mutableStateOf<VaccineRecord?>(null) }
    Column(modifier.padding(16.dp)) {
        TextButton(onClick = onAdd, modifier = Modifier.align(Alignment.End)) { Text("＋ 新增紀錄", color = Orange) }
        if (records.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("還沒有紀錄。\n打完疫苗或做完檢測後，在這裡記下日期，\n手冊就會算出下次時間。",
                    color = BrownLight, fontSize = 14.sp, lineHeight = 21.sp)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records, key = { it.id }) { r ->
                    val title = VaccineGuide.items.firstOrNull { it.key == r.itemKey }?.title ?: r.itemKey
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(White).padding(start = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                            Text(title, color = Brown, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(r.doneDate, color = BrownLight, fontSize = 12.sp)
                        }
                        IconButton(onClick = { deleting = r }) {
                            Icon(Icons.Default.Delete, contentDescription = "刪除", tint = BrownLight)
                        }
                    }
                }
            }
        }
    }
    deleting?.let { r ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("刪除這筆紀錄？") },
            text = { Text("${r.doneDate}　${VaccineGuide.items.firstOrNull { it.key == r.itemKey }?.title ?: r.itemKey}") },
            confirmButton = { TextButton(onClick = { onDelete(r.id); deleting = null }) { Text("刪除", color = Red) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消", color = BrownLight) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordDialog(
    items: List<GuideItem>,
    presetKey: String?,
    onConfirm: (itemKey: String, date: String) -> Unit,
    onDismiss: () -> Unit
) {
    var chosen by remember { mutableStateOf(presetKey) }
    val todayStr = remember { DateUtil.format(DateUtil.today()) }
    var date by remember { mutableStateOf(todayStr) }
    var picking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增紀錄") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (presetKey != null) {
                    Text(items.firstOrNull { it.key == presetKey }?.title ?: presetKey, color = Brown, fontWeight = FontWeight.Bold)
                } else {
                    Text("選擇項目", color = BrownLight, fontSize = 12.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items.forEach { it2 ->
                            FilterChip(selected = chosen == it2.key, onClick = { chosen = it2.key }, label = { Text(it2.title, fontSize = 12.sp) })
                        }
                    }
                }
                Text("日期", color = BrownLight, fontSize = 12.sp)
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(CreamDark)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    TextButton(onClick = { picking = true }) { Text(date, color = Brown, fontSize = 15.sp) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = chosen != null, onClick = { chosen?.let { onConfirm(it, date) } }) {
                Text("儲存", color = if (chosen != null) Orange else BrownLight)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = BrownLight) } }
    )

    if (picking) {
        val d = DateUtil.parse(date)?.let { date.split("-") }
        WheelDatePickerDialog(
            initialYear = d?.getOrNull(0)?.toIntOrNull() ?: 2026,
            initialMonth = d?.getOrNull(1)?.toIntOrNull() ?: 1,
            initialDay = d?.getOrNull(2)?.toIntOrNull() ?: 1,
            title = "施打／檢測日期",
            onConfirm = { y, m, day ->
                val s = "%04d-%02d-%02d".format(y, m, day)
                // 選到不存在的日期（例如 2/31）或未來日期時，不接受
                if (DateUtil.parse(s)?.let { it <= DateUtil.today() } == true) date = s
                picking = false
            },
            onDismiss = { picking = false }
        )
    }
}

// ───── 問 AI ─────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AskTab(modifier: Modifier, vm: VaccineViewModel) {
    val chat by vm.chat.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.chatError.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chat.size, busy) {
        if (chat.isNotEmpty()) listState.animateScrollToItem(chat.size)
    }

    Column(modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(White).padding(14.dp)) {
                    Text("問手冊", color = Brown, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("AI 只會根據這本手冊的內容和牠已記錄的資料回答，不診斷、不推薦藥物或產品。" +
                        "牠現在該不該打，一律要問獸醫。", color = BrownLight, fontSize = 12.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("牠現在有哪些可以安排？", "狂犬病疫苗多久打一次？", "為什麼幼犬貓要打好幾劑？", "快篩是什麼時候做？").forEach { q ->
                            FilterChip(selected = false, enabled = !busy, onClick = { vm.ask(q) }, label = { Text(q, fontSize = 12.sp) })
                        }
                    }
                }
            }
            items(chat.size) { i ->
                val line = chat[i]
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        line.question, color = White, fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.End).clip(RoundedCornerShape(14.dp))
                            .background(Orange).padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    Text(
                        line.answer, color = Brown, fontSize = 14.sp, lineHeight = 21.sp,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(White).padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            if (busy) item { Text("AI 整理中…", color = BrownLight, fontSize = 13.sp) }
            error?.let { e -> item { Text(e, color = Red, fontSize = 13.sp, lineHeight = 19.sp) } }
        }
        Row(Modifier.fillMaxWidth().background(White).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                placeholder = { Text("問一個關於疫苗或檢測的問題") },
                modifier = Modifier.weight(1f), maxLines = 3
            )
            IconButton(enabled = input.isNotBlank() && !busy, onClick = { vm.ask(input); input = "" }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "送出",
                    tint = if (input.isNotBlank() && !busy) Orange else BrownLight)
            }
        }
    }
}
