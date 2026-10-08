package com.petmed.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petmed.app.data.repository.OwnerStore
import com.petmed.app.data.repository.UploadConsentStore
import com.petmed.app.ui.theme.Blue
import com.petmed.app.ui.theme.Brown
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.Cream
import com.petmed.app.ui.theme.Green
import com.petmed.app.ui.theme.Orange
import com.petmed.app.ui.theme.Pink
import com.petmed.app.ui.theme.Purple
import com.petmed.app.ui.theme.White
import com.petmed.app.ui.viewmodel.MedicationViewModel
import com.petmed.app.ui.viewmodel.PetViewModel
import com.petmed.app.ui.viewmodel.VaccineViewModel

@Composable
fun HomeScreen(
    onMedRecordClick: () -> Unit = {},
    onHospitalClick: () -> Unit = {},
    onAdsClick: () -> Unit = {},
    onVaccineClick: () -> Unit = {},
    petViewModel: PetViewModel = viewModel(),
    medicationViewModel: MedicationViewModel = viewModel(),
    vaccineViewModel: VaccineViewModel = viewModel()
) {
    val context = LocalContext.current
    val pet by petViewModel.firstPet.collectAsState()
    val ownerName = OwnerStore.getName(context)
    val displayName = if (ownerName.isNotBlank()) ownerName else (pet?.name ?: "您好")
    val nextMed by medicationViewModel.nextUpcoming.collectAsState()
    val vaccineNotice by vaccineViewModel.homeNotice.collectAsState()
    var showConsentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!UploadConsentStore.hasConsented(context)) {
            showConsentDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // 問候語
        Text(text = "午安", color = BrownLight, fontSize = 15.sp)
        Text(
            text = displayName,
            color = Brown,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 用藥提醒卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Orange)
                .padding(20.dp)
        ) {
            Icon(
                Icons.Default.Pets,
                contentDescription = null,
                tint = White.copy(alpha = 0.25f),
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.CenterEnd)
            )
            Column {
                val timeLabel = when {
                    nextMed == null -> "--"
                    nextMed!!.isRepeat -> {
                        val dayNames = listOf("日", "一", "二", "三", "四", "五", "六")
                        val days = nextMed!!.repeatDays.split(",")
                            .mapNotNull { it.trim().toIntOrNull() }
                            .filter { it in 1..7 }
                            .joinToString(" ") { "週${dayNames[it - 1]}" }
                        "$days  ${nextMed!!.repeatTime}"
                    }
                    else -> nextMed!!.dateTime.substringAfter(" ")
                }
                Text(
                    text = timeLabel,
                    color = White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (nextMed != null) "用藥提醒・${nextMed!!.medicationName}" else "目前無待辦提醒",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (nextMed != null && !nextMed!!.isRepeat) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(White.copy(alpha = 0.25f))
                            .clickable {
                                medicationViewModel.update(nextMed!!.copy(isCompleted = true), pet)
                            }
                            .padding(horizontal = 18.dp, vertical = 7.dp)
                    ) {
                        Text(text = "標記已服用", color = White, fontSize = 13.sp)
                    }
                }
            }
        }

        // 寵物手冊的提示（沒有生日或不是貓狗時不顯示）
        vaccineNotice?.let { notice ->
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Purple.copy(alpha = 0.2f))
                    .clickable { onVaccineClick() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = Brown,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = notice, color = Brown, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 快速功能按鈕
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureCard(
                title = "用藥紀錄",
                icon = Icons.Default.Medication,
                color = Green,
                onClick = onMedRecordClick,
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                title = "動物醫院查詢",
                icon = Icons.Default.LocalHospital,
                color = Blue,
                onClick = onHospitalClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureCard(
                title = "寵物手冊・\n疫苗與檢測",
                icon = Icons.Default.MenuBook,
                color = Purple,
                onClick = onVaccineClick,
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                title = "保養品專區",
                icon = Icons.Default.Spa,
                color = Pink,
                onClick = onAdsClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text("資料使用說明") },
            text = {
                Text(
                    "為了改善用藥提醒服務，當您標記藥物已服用時，App 會匿名上傳藥名、時間及寵物種類（不含任何個人資訊）用於分析用藥頻率。\n\n您可以隨時在設定中關閉此功能。",
                    color = Brown,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    UploadConsentStore.setConsented(context, true)
                    showConsentDialog = false
                }) { Text("同意", color = Orange) }
            },
            dismissButton = {
                TextButton(onClick = {
                    UploadConsentStore.setConsented(context, false)
                    showConsentDialog = false
                }) { Text("不同意", color = BrownLight) }
            }
        )
    }
}

@Composable
private fun FeatureCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.15f)
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            // 左上、右下的淡色圓形裝飾
            .drawBehind {
                drawCircle(
                    color = White.copy(alpha = 0.12f),
                    radius = size.minDimension * 0.4f,
                    center = Offset(0f, 0f)
                )
                drawCircle(
                    color = White.copy(alpha = 0.12f),
                    radius = size.minDimension * 0.3f,
                    center = Offset(size.width, size.height)
                )
            }
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = White,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}
