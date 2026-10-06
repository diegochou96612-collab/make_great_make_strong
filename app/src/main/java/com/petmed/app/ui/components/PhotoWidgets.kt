package com.petmed.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petmed.app.interview.PhotoStore
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.CreamDark
import com.petmed.app.ui.theme.Orange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** 載入縮圖（背景執行緒解碼，避免卡畫面）。 */
@Composable
fun PhotoThumb(
    path: String,
    modifier: Modifier = Modifier,
    maxEdge: Int = 480,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bmp by produceState<ImageBitmap?>(initialValue = null, path, maxEdge) {
        value = withContext(Dispatchers.IO) { PhotoStore.decodeThumb(path, maxEdge)?.asImageBitmap() }
    }
    val img = bmp
    if (img != null) {
        Image(bitmap = img, contentDescription = "照片", modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier.background(CreamDark))
    }
}

/**
 * 「拍照」與「從相簿選」兩個按鈕。選好之後讓飼主加說明與主題，按儲存才呼叫 onSave。
 * 照片處理（轉正、縮小、重新編碼以移除位置等 EXIF）見 PhotoStore。
 * hint：要拍什麼（顯示給飼主）。拍照建議只是方便獸醫參考，不是診斷。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoAddRow(
    caseId: Long,
    hint: String,
    defaultTopic: String,
    onSave: (file: File, note: String, topic: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingRaw by remember { mutableStateOf<File?>(null) }
    var processed by remember { mutableStateOf<File?>(null) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val raw = pendingRaw
        pendingRaw = null
        if (ok && raw != null) {
            scope.launch {
                busy = true
                val out = withContext(Dispatchers.IO) { PhotoStore.processCameraFile(context, caseId, raw) }
                busy = false
                if (out != null) processed = out else failed = true
            }
        } else raw?.delete()
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val out = withContext(Dispatchers.IO) { PhotoStore.processUri(context, caseId, uri) }
                busy = false
                if (out != null) processed = out else failed = true
            }
        }
    }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(enabled = !busy, onClick = {
                val f = PhotoStore.newCameraTarget(context, caseId)
                pendingRaw = f
                camera.launch(PhotoStore.uriFor(context, f))
            }) { Text("📷 拍照", color = Orange) }
            TextButton(enabled = !busy, onClick = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("🖼 從相簿選", color = Orange) }
            if (busy) Text("處理中…", color = BrownLight, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp))
        }
        if (hint.isNotBlank()) Text(hint, color = BrownLight, fontSize = 11.sp)
        if (failed) Text("這張照片處理失敗，請換一張再試。", color = BrownLight, fontSize = 11.sp)
    }

    processed?.let { file ->
        var note by remember(file.path) { mutableStateOf("") }
        var topic by remember(file.path) { mutableStateOf(defaultTopic.ifBlank { "其他" }) }
        AlertDialog(
            onDismissRequest = { file.delete(); processed = null },
            title = { Text("加入相簿", fontSize = 16.sp) },
            text = {
                Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                    PhotoThumb(
                        path = file.path, maxEdge = 700,
                        modifier = Modifier.fillMaxWidth().height(200.dp), contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("主題", color = BrownLight, fontSize = 11.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        PhotoStore.topics.forEach { t ->
                            FilterChip(selected = topic == t, onClick = { topic = t }, label = { Text(t, fontSize = 12.sp) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("說明（選填）") },
                        placeholder = { Text("例如：左耳，昨晚開始") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onSave(file, note, topic); processed = null }) { Text("儲存", color = Orange) }
            },
            dismissButton = {
                TextButton(onClick = { file.delete(); processed = null }) { Text("不要了", color = BrownLight) }
            }
        )
    }
}
