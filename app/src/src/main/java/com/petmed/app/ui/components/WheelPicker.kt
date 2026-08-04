package com.petmed.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petmed.app.ui.theme.Brown
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.Cream
import com.petmed.app.ui.theme.CreamDark
import com.petmed.app.ui.theme.Orange
import kotlinx.coroutines.flow.filter

@Composable
fun WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex)
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val currentSelected by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    LaunchedEffect(Unit) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect {
                onSelectionChange(listState.firstVisibleItemIndex.coerceIn(0, items.lastIndex))
            }
    }

    Box(modifier = modifier.height(200.dp)) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = 80.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, item ->
                val isSelected = index == currentSelected
                Box(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        color = if (isSelected) Brown else BrownLight.copy(alpha = 0.5f),
                        fontSize = if (isSelected) 16.sp else 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.align(Alignment.Center).offset(y = (-20).dp),
            color = CreamDark, thickness = 1.dp
        )
        HorizontalDivider(
            modifier = Modifier.align(Alignment.Center).offset(y = 20.dp),
            color = CreamDark, thickness = 1.dp
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter).fillMaxWidth().height(80.dp)
                .background(Brush.verticalGradient(listOf(Cream.copy(alpha = 0.95f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter).fillMaxWidth().height(80.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Cream.copy(alpha = 0.95f))))
        )
    }
}

@Composable
fun WheelTimePicker(
    initialHour: Int = 8,
    initialMinute: Int = 0,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hours = (0..23).map { "%02d".format(it) }
    val minutes = (0..59).map { "%02d".format(it) }

    var selectedHour by remember { mutableStateOf(initialHour.coerceIn(0, 23)) }
    var selectedMinute by remember { mutableStateOf(initialMinute.coerceIn(0, 59)) }

    LaunchedEffect(selectedHour, selectedMinute) {
        onTimeChange(selectedHour, selectedMinute)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        WheelColumn(
            items = hours,
            selectedIndex = selectedHour,
            onSelectionChange = { selectedHour = it },
            modifier = Modifier.width(70.dp)
        )
        Text(
            ":",
            color = Brown, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        WheelColumn(
            items = minutes,
            selectedIndex = selectedMinute,
            onSelectionChange = { selectedMinute = it },
            modifier = Modifier.width(70.dp)
        )
    }
}

@Composable
fun WheelDatePickerDialog(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onConfirm: (year: Int, month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val years = (1990..currentYear).map { it.toString() }
    val months = (1..12).map { "%02d".format(it) }
    val days = (1..31).map { "%02d".format(it) }

    var selectedYear by remember { mutableStateOf(initialYear.coerceIn(1990, currentYear)) }
    var selectedMonth by remember { mutableStateOf(initialMonth.coerceIn(1, 12)) }
    var selectedDay by remember { mutableStateOf(initialDay.coerceIn(1, 31)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("選擇生日") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(2.5f)) {
                    Text("年", fontSize = 12.sp, color = BrownLight)
                    WheelColumn(
                        items = years,
                        selectedIndex = years.indexOf(selectedYear.toString()).coerceAtLeast(0),
                        onSelectionChange = { selectedYear = years[it].toInt() }
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("月", fontSize = 12.sp, color = BrownLight)
                    WheelColumn(
                        items = months,
                        selectedIndex = selectedMonth - 1,
                        onSelectionChange = { selectedMonth = it + 1 }
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("日", fontSize = 12.sp, color = BrownLight)
                    WheelColumn(
                        items = days,
                        selectedIndex = selectedDay - 1,
                        onSelectionChange = { selectedDay = it + 1 }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedYear, selectedMonth, selectedDay) }) {
                Text("確定", color = Orange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = BrownLight) }
        }
    )
}
