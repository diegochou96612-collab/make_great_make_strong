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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.petmed.app.ui.theme.Pink
import com.petmed.app.ui.theme.Purple
import com.petmed.app.ui.theme.White

@Composable
fun HomeScreen(
    onAIChatClick: () -> Unit = {},
    onMedRecordClick: () -> Unit = {},
    onHospitalClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // 問候語
        Text(
            text = "午安",
            color = BrownLight,
            fontSize = 15.sp
        )
        Text(
            text = "Tom",
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
                Text(
                    text = "10:00",
                    color = White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "用藥提醒・滴耳易",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(White.copy(alpha = 0.25f))
                        .padding(horizontal = 18.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "標記已服用",
                        color = White,
                        fontSize = 13.sp
                    )
                }
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
                title = "醫院/疫苗查詢",
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
                onClick = onMedRecordClick,
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                title = "保養品專區",
                icon = Icons.Default.Spa,
                color = Pink,
                onClick = onHospitalClick,
                modifier = Modifier.weight(1f)
            )
        }
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
