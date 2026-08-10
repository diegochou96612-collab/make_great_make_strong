package com.petmed.app.ui.screens

import android.Manifest
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petmed.app.data.model.MedicationRecord
import com.petmed.app.ui.components.WheelTimePicker
import com.petmed.app.ui.theme.Brown
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.Cream
import com.petmed.app.ui.theme.CreamDark
import com.petmed.app.ui.theme.Orange
import com.petmed.app.ui.theme.Red
import com.petmed.app.ui.theme.White
import com.petmed.app.ui.viewmodel.MedicationViewModel

@Composable
fun MedicationRecordScreen(
    medicationViewModel: MedicationViewModel = viewModel()
) {
    val records by medicationViewModel.records.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<MedicationRecord?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    requestNotificationPermissionIfNeeded()
                    showAddDialog = true
                },
                containerColor = Orange
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增紀錄", tint = White)
            }
        },
        containerColor = Cream
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Orange)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "用藥紀錄",
                    color = White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("尚無用藥紀錄", color = BrownLight, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(records) { index, record ->
                        MedicationRecordItem(
                            record = record,
                            isHighlighted = index == 0,
                            onToggleRepeat = { active ->
                                medicationViewModel.toggleRepeatActive(record, active)
                            },
                            onClick = {
                                selectedRecord = record
                                showActionDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // 新增
    if (showAddDialog) {
        RecordDialog(
            existing = null,
            onConfirm = { record ->
                medicationViewModel.insert(record)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // 點擊後選項
    if (showActionDialog && selectedRecord != null) {
        val record = selectedRecord!!
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = { Text(record.medicationName) },
            text = {
                Column {
                    if (!record.isRepeat) {
                        val label = if (record.isCompleted) "取消完成" else "標記已完成"
                        ActionRow(
                            label = label,
                            color = Brown,
                            onClick = {
                                medicationViewModel.update(record.copy(isCompleted = !record.isCompleted))
                                showActionDialog = false
                            }
                        )
                        HorizontalDivider(color = CreamDark)
                    }
                    ActionRow(
                        label = "編輯",
                        color = Brown,
                        onClick = {
                            showActionDialog = false
                            showEditDialog = true
                        }
                    )
                    HorizontalDivider(color = CreamDark)
                    ActionRow(
                        label = "刪除",
                        color = Red,
                        onClick = {
                            medicationViewModel.delete(record)
                            showActionDialog = false
                            selectedRecord = null
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showActionDialog = false }) {
                    Text("取消", color = BrownLight)
                }
            }
        )
    }

    // 編輯
    if (showEditDialog && selectedRecord != null) {
        val old = selectedRecord!!
        RecordDialog(
            existing = old,
            onConfirm = { newRecord ->
                medicationViewModel.editRecord(old, newRecord)
                showEditDialog = false
                selectedRecord = null
            },
            onDismiss = {
                showEditDialog = false
                selectedRecord = null
            }
        )
    }
}

@Composable
private fun ActionRow(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(text = label, color = color, fontSize = 15.sp)
    }
}

@Composable
private fun MedicationRecordItem(
    record: MedicationRecord,
    isHighlighted: Boolean,
    onToggleRepeat: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val bgColor = if (isHighlighted) Orange else White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        if (record.isRepeat) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.medicationName,
                        color = if (isHighlighted) White else Brown,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val dayNames = listOf("日", "一", "二", "三", "四", "五", "六")
                    val dayLabel = record.repeatDays.split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .filter { it in 1..7 }
                        .joinToString(" ") { "週${dayNames[it - 1]}" }
                    Text(
                        text = "$dayLabel  ${record.repeatTime}",
                        color = if (isHighlighted) White.copy(alpha = 0.75f) else BrownLight,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = record.isRepeatActive,
                    onCheckedChange = onToggleRepeat,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (isHighlighted) White else Orange,
                        checkedTrackColor = if (isHighlighted) White.copy(alpha = 0.4f) else Orange.copy(alpha = 0.4f),
                        uncheckedThumbColor = if (isHighlighted) White.copy(alpha = 0.6f) else BrownLight,
                        uncheckedTrackColor = if (isHighlighted) White.copy(alpha = 0.2f) else CreamDark
                    )
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.medicationName,
                        color = if (isHighlighted) White else Brown,
                        fontSize = 14.sp,
                        fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal
                    )
                    if (record.dateTime.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = record.dateTime,
                            color = if (isHighlighted) White.copy(alpha = 0.75f) else BrownLight,
                            fontSize = 12.sp
                        )
                    }
                }
                if (record.isCompleted) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "已完成", color = if (isHighlighted) White.copy(alpha = 0.75f) else BrownLight, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RecordDialog(
    existing: MedicationRecord?,
    onConfirm: (MedicationRecord) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(TextFieldValue(existing?.medicationName ?: "")) }
    var dateTime by remember { mutableStateOf(TextFieldValue(existing?.dateTime ?: "")) }
    var isRepeat by remember { mutableStateOf(existing?.isRepeat ?: false) }
    var selectedDays by remember {
        mutableStateOf(
            existing?.repeatDays
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.toSet() ?: emptySet()
        )
    }
    val initTime = existing?.repeatTime?.split(":")
    var repeatHour by remember { mutableStateOf(initTime?.getOrNull(0)?.toIntOrNull() ?: 8) }
    var repeatMinute by remember { mutableStateOf(initTime?.getOrNull(1)?.toIntOrNull() ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "新增用藥紀錄" else "編輯用藥紀錄") },
        text = {
            val view = LocalView.current
            SideEffect {
                (view.parent as? DialogWindowProvider)?.window
                    ?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("藥名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("重複提醒", color = Brown, fontSize = 14.sp)
                    Switch(
                        checked = isRepeat,
                        onCheckedChange = { isRepeat = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Orange,
                            checkedTrackColor = Orange.copy(alpha = 0.4f)
                        )
                    )
                }

                if (isRepeat) {
                    WheelTimePicker(
                        initialHour = repeatHour,
                        initialMinute = repeatMinute,
                        onTimeChange = { h, m ->
                            repeatHour = h
                            repeatMinute = m
                        }
                    )

                    Text("提醒日期", color = BrownLight, fontSize = 12.sp)
                    val dayLabels = listOf("日", "一", "二", "三", "四", "五", "六")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dayLabels.forEachIndexed { index, label ->
                            val dayValue = index + 1
                            val isSelected = dayValue in selectedDays
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Orange else CreamDark)
                                    .clickable {
                                        selectedDays = if (isSelected)
                                            selectedDays - dayValue
                                        else
                                            selectedDays + dayValue
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) White else BrownLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = dateTime,
                        onValueChange = { dateTime = it },
                        label = { Text("時間（例：2026-08-01 18:00）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.text.isNotBlank()) {
                        if (isRepeat && selectedDays.isEmpty()) return@TextButton
                        onConfirm(
                            MedicationRecord(
                                id = existing?.id ?: 0,
                                medicationName = name.text.trim(),
                                dateTime = if (isRepeat) "" else dateTime.text.trim(),
                                isCompleted = existing?.isCompleted ?: false,
                                isRepeat = isRepeat,
                                repeatDays = if (isRepeat) selectedDays.sorted().joinToString(",") else "",
                                repeatTime = if (isRepeat) "%02d:%02d".format(repeatHour, repeatMinute) else "",
                                isRepeatActive = existing?.isRepeatActive ?: true
                            )
                        )
                    }
                }
            ) { Text(if (existing == null) "新增" else "儲存", color = Orange) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = BrownLight) }
        }
    )
}
