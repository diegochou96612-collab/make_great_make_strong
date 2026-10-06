package com.petmed.app.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petmed.app.data.model.CasePhoto
import com.petmed.app.interview.PhotoStore
import com.petmed.app.interview.ReportBuilder
import com.petmed.app.ui.components.PhotoAddRow
import com.petmed.app.ui.components.PhotoThumb
import com.petmed.app.ui.theme.Brown
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.Cream
import com.petmed.app.ui.theme.Orange
import com.petmed.app.ui.theme.Red
import com.petmed.app.ui.theme.White
import com.petmed.app.ui.viewmodel.CaseReportViewModel

/** 一個狀況的照片相簿：同一次狀況的各種照片放在一起，每張可以寫說明。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CasePhotoScreen(
    caseId: Long,
    onBack: () -> Unit = {},
    vm: CaseReportViewModel = viewModel()
) {
    val photosFlow = remember(caseId) { vm.observePhotos(caseId) }
    val photos by photosFlow.collectAsState(initial = emptyList())
    var viewing by remember { mutableStateOf<CasePhoto?>(null) }

    Column(Modifier.fillMaxSize().background(Cream)) {
        Row(
            Modifier.fillMaxWidth().background(Orange).statusBarsPadding().padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = White)
            }
            Text("照片相簿（${photos.size}）", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        if (photos.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "還沒有照片。\n把這次狀況的各種照片放在這裡，\n獸醫看診時可以一起參考。",
                    color = BrownLight, fontSize = 14.sp, lineHeight = 21.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(photos, key = { it.id }) { p ->
                    Column(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(White).clickable { viewing = p }
                    ) {
                        PhotoThumb(p.filePath, Modifier.fillMaxWidth().aspectRatio(1f))
                        Column(Modifier.padding(8.dp)) {
                            Text(
                                p.topic.ifBlank { "照片" }, color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Medium
                            )
                            Text(
                                p.note.ifBlank { ReportBuilder.formatTime(p.takenAt) },
                                color = Brown, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(White).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp)) {
            PhotoAddRow(
                caseId = caseId,
                hint = "建議：近拍和全景各一張，光線充足。照片只存在這支手機的 App 裡，不含拍攝位置。這是方便獸醫參考，不是診斷。",
                defaultTopic = "其他",
                onSave = { file, note, topic -> vm.addPhoto(caseId, file, note, topic) }
            )
        }
    }

    viewing?.let { p ->
        var note by remember(p.id) { mutableStateOf(p.note) }
        var topic by remember(p.id) { mutableStateOf(p.topic.ifBlank { "其他" }) }
        AlertDialog(
            onDismissRequest = { viewing = null },
            title = { Text(ReportBuilder.formatTime(p.takenAt), fontSize = 14.sp) },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                    PhotoThumb(
                        p.filePath, Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        maxEdge = 1200, contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        PhotoStore.topics.forEach { t ->
                            FilterChip(selected = topic == t, onClick = { topic = t }, label = { Text(t, fontSize = 12.sp) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text("說明") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.updatePhoto(p, note, topic); viewing = null }) { Text("儲存", color = Orange) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.deletePhoto(p); viewing = null }) { Text("刪除", color = Red) }
                    TextButton(onClick = { viewing = null }) { Text("關閉", color = BrownLight) }
                }
            }
        )
    }
}
