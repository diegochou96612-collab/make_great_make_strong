package com.petmed.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petmed.app.data.model.Hospital
import com.petmed.app.ui.theme.Blue
import com.petmed.app.ui.theme.Brown
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.Cream
import com.petmed.app.ui.theme.CreamDark
import com.petmed.app.ui.theme.Green
import com.petmed.app.ui.theme.White

@Composable
fun HospitalSearchScreen() {
    var searchQuery by remember { mutableStateOf("") }

    val allHospitals = listOf(
        Hospital(1, "士林動物醫院", "02-2831-2345", district = "士林"),
        Hospital(2, "天母寵物診所", "02-2888-9999", district = "士林"),
        Hospital(3, "芝山動物醫療中心", "02-2888-0000", district = "士林"),
        Hospital(4, "大同動物醫院", "02-2500-1234", district = "大同"),
        Hospital(5, "中山寵物診所", "02-2512-3456", district = "中山"),
        Hospital(6, "信義動物醫院", "02-2756-7890", district = "信義"),
    )

    val filtered = allHospitals.filter {
        searchQuery.isBlank() ||
                it.district.contains(searchQuery) ||
                it.name.contains(searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // 藍色標題列
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Blue)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "動物醫院查詢",
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 搜尋欄
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("輸入地區或醫院名稱", color = BrownLight, fontSize = 14.sp) },
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "搜尋", tint = BrownLight)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Blue,
                    unfocusedBorderColor = CreamDark,
                    focusedContainerColor = White,
                    unfocusedContainerColor = White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(filtered) { index, hospital ->
                    HospitalItem(hospital = hospital, isFirst = index == 0)
                }
            }
        }
    }
}

@Composable
private fun HospitalItem(hospital: Hospital, isFirst: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFirst) Green else White)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = hospital.name,
                color = if (isFirst) White else Brown,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "電話：${hospital.phone}",
                color = if (isFirst) White.copy(alpha = 0.9f) else BrownLight,
                fontSize = 13.sp
            )
        }
    }
}
