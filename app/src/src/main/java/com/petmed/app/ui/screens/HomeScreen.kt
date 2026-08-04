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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petmed.app.data.repository.OwnerStore
import com.petmed.app.data.repository.UploadConsentStore
import com.petmed.app.ui.viewmodel.MedicationViewModel
import com.petmed.app.ui.viewmodel.PetViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petmed.app.ui.theme.Blue
import com.petmed.app.ui.theme.Brown
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.Cream
import com.petmed.app.ui.theme.Green
import com.petmed.app.ui.theme.Orange
import com.petmed.app.ui.theme.White

@Composable
fun HomeScreen(
    onAIChatClick: () -> Unit = {},
    onMedRecordClick: () -> Unit = {},
    onHospitalClick: () -> Unit = {},
    petViewModel: PetViewModel = viewModel(),
    medicationViewModel: MedicationViewModel = viewModel()
) {
    val context = LocalContext.current
    val pet by petViewModel.firstPet.collectAsState()
    val ownerName = OwnerStore.getName(context)
    val displayName = if (ownerName.isNotBlank()) ownerName else (pet?.name ?: "您好")
    val nextMed by medicationViewModel.nextUpcoming.collectAsState()
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
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // 問候語
        Text(
            text = "午安",
            color = BrownLight,
            fontSize = 15.sp
        )
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
            Column {
                Text(
                    text = nextMed?.dateTime?.substringAfter(" ") ?: "--",
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
                if (nextMed != null) {
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

        Spacer(modifier = Modifier.height(28.dp))

        // 快速功能按鈕列
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Green)
                    .clickable { onMedRecordClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "用藥紀錄",
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Blue)
                    .clickable { onHospitalClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "醫院/疫苗查詢",
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI 問答入口
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(White)
                .clickable { onAIChatClick() }
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI 問答",
                    color = Brown,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = BrownLight
                )
            }
        }
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
