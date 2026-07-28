package com.petmed.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petmed.app.data.model.MedicationRecord
import com.petmed.app.ui.theme.Brown
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.Cream
import com.petmed.app.ui.theme.Orange
import com.petmed.app.ui.theme.White

@Composable
fun MedicationRecordScreen() {
    val records = listOf(
        MedicationRecord(1, "7/28 18:00 芬普尼（皮膚過敏藥）", "2026-07-28 18:00", isCompleted = false),
        MedicationRecord(2, "7/25 09:00 心絲蟲預防錠", "2026-07-25 09:00", isCompleted = true),
        MedicationRecord(3, "7/20 14:00 廣效驅蟲藥", "2026-07-20 14:00", isCompleted = true),
        MedicationRecord(4, "7/15 18:00 皮膚消炎藥膏", "2026-07-15 18:00", isCompleted = true),
        MedicationRecord(5, "7/10 10:00 綜合維他命", "2026-07-10 10:00", isCompleted = true),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // 橘色標題列
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(records) { index, record ->
                MedicationRecordItem(record = record, isHighlighted = index == 0)
            }
        }
    }
}

@Composable
private fun MedicationRecordItem(record: MedicationRecord, isHighlighted: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isHighlighted) Orange else White)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
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
            }
            if (!isHighlighted && record.isCompleted) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "已完成",
                    color = BrownLight,
                    fontSize = 12.sp
                )
            }
        }
    }
}
