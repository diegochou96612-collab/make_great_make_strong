package com.petmed.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petmed.app.data.model.Hospital
import com.petmed.app.ui.viewmodel.HospitalViewModel
import com.petmed.app.ui.theme.Blue
import com.petmed.app.ui.theme.Brown
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.Cream
import com.petmed.app.ui.theme.CreamDark
import com.petmed.app.ui.theme.Orange
import com.petmed.app.ui.theme.White

private const val DEFAULT_HOSPITAL_URL = "https://www.google.com"

@Composable
fun HospitalSearchScreen(
    hospitalViewModel: HospitalViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val allHospitals by hospitalViewModel.hospitals.collectAsState()
    val isLoading by hospitalViewModel.isLoading.collectAsState()
    val error by hospitalViewModel.error.collectAsState()

    val filtered = allHospitals.filter {
        searchQuery.text.isBlank() ||
                it.district.contains(searchQuery.text) ||
                it.name.contains(searchQuery.text) ||
                it.address.contains(searchQuery.text)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
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

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Blue)
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = error!!, color = BrownLight, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            IconButton(onClick = { hospitalViewModel.fetchHospitals() }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "重試",
                                    tint = Blue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
                filtered.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("找不到符合的醫院", color = BrownLight, fontSize = 14.sp)
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(filtered) { _, hospital ->
                            HospitalItem(
                                hospital = hospital,
                                websiteUrl = DEFAULT_HOSPITAL_URL
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HospitalItem(hospital: Hospital, websiteUrl: String? = null) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var showLinkDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPressed) CreamDark else White)
            .clickable(interactionSource = interactionSource, indication = null) {
                if (websiteUrl != null) showLinkDialog = true
            }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hospital.name,
                    color = Brown,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = hospital.phone,
                    color = BrownLight,
                    fontSize = 13.sp
                )
            }
            if (hospital.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = hospital.address,
                    color = BrownLight,
                    fontSize = 12.sp
                )
            }
            if (websiteUrl != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "點此前往官網 →",
                    color = Blue,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showLinkDialog && websiteUrl != null) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("前往外部網站") },
            text = {
                Text(
                    "即將離開 App，前往\n$websiteUrl\n\n確定要繼續嗎？",
                    color = Brown,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLinkDialog = false
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
                    )
                }) { Text("前往", color = Orange) }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text("取消", color = BrownLight)
                }
            }
        )
    }
}
