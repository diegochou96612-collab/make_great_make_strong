package com.petmed.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petmed.app.ui.theme.Blue
import com.petmed.app.ui.theme.Brown
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.Cream
import com.petmed.app.ui.theme.CreamDark
import com.petmed.app.ui.theme.Green
import com.petmed.app.ui.theme.GreenDark
import com.petmed.app.ui.theme.Orange
import com.petmed.app.ui.theme.OrangeDark
import com.petmed.app.ui.theme.White

private data class AdCard(
    val kind: Int,
    val name: String,
    val category: String,
    val copy: String,
    val bg: Color
)

/**
 * 保養品專區。
 * - 名稱為虛構、插圖為程式繪製，沒有任何真實品牌、人物或個資。
 * - 只放日常保養類型，不含藥品，不含療效宣稱。
 * - 與AI 記錄助理完全分開：AI 不會推薦任何藥品或保養品。
 */
private val ads = listOf(
    AdCard(0, "潔淨護耳露", "日常清潔保養", "溫和配方，適合定期的耳朵清潔保養", Color(0xFFE3F1F8)),
    AdCard(1, "晶亮護眼配方", "營養保健食品", "日常營養補給，搭配均衡飲食", Color(0xFFE5F4EC)),
    AdCard(2, "腸道好夥伴", "營養保健食品", "日常飲食的好幫手，口味多種", Color(0xFFFFF0E3)),
    AdCard(3, "草本舒護膏", "日常護膚保養", "天然草本萃取，日常肌膚護理", Color(0xFFF4EBDD))
)

@Composable
fun AdsScreen(onBack: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(Cream)) {
        Row(
            Modifier.fillMaxWidth().background(Orange).statusBarsPadding().padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = White)
            }
            Text("保養品專區", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(ads.size) { i -> AdCardView(ads[i]) }
            item {
                Text(
                    "本頁商品為日常保養類型，不含藥品，也不含任何療效宣稱。\n" +
                        "本頁與AI 記錄助理完全分開：AI 不會推薦任何藥品或保養品。\n" +
                        "寵物若有症狀，請諮詢獸醫師。",
                    color = BrownLight, fontSize = 11.sp, lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun AdCardView(ad: AdCard) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(White)) {
        Box(Modifier.fillMaxWidth().height(150.dp).background(ad.bg)) {
            Canvas(Modifier.fillMaxSize()) {
                when (ad.kind) {
                    0 -> drawEarBottle()
                    1 -> drawEye()
                    2 -> drawJar()
                    else -> drawTube()
                }
            }
            Box(
                Modifier.padding(10.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xCC3D2B1F))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) { Text("廣告", color = White, fontSize = 10.sp) }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(ad.name, color = Brown, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(ad.category, color = OrangeDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(ad.copy, color = BrownLight, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.clip(RoundedCornerShape(20.dp)).background(CreamDark).padding(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("了解更多", color = BrownLight, fontSize = 12.sp) }
        }
    }
}

// ───────── 插圖（全部用形狀繪製，沒有任何外部圖片）─────────

private fun DrawScope.drawEarBottle() {
    val cx = size.width / 2f
    val bodyW = size.width * 0.24f
    val bodyH = size.height * 0.52f
    val top = size.height * 0.30f
    // 瓶身
    drawRoundRect(Color.White, Offset(cx - bodyW / 2, top), Size(bodyW, bodyH), CornerRadius(24f, 24f))
    drawRoundRect(Blue, Offset(cx - bodyW / 2 + 8f, top + bodyH * 0.45f), Size(bodyW - 16f, bodyH * 0.50f), CornerRadius(14f, 14f))
    // 瓶頸與瓶蓋
    drawRoundRect(Color.White, Offset(cx - bodyW * 0.22f, top - 22f), Size(bodyW * 0.44f, 26f), CornerRadius(8f, 8f))
    drawRoundRect(GreenDark, Offset(cx - bodyW * 0.30f, top - 44f), Size(bodyW * 0.60f, 26f), CornerRadius(10f, 10f))
    // 標籤
    drawRoundRect(Color(0xFFFFFFFF), Offset(cx - bodyW * 0.32f, top + bodyH * 0.12f), Size(bodyW * 0.64f, bodyH * 0.22f), CornerRadius(8f, 8f))
    // 水滴
    val dropX = cx + bodyW * 1.25f
    val dropY = size.height * 0.40f
    val drop = Path().apply {
        moveTo(dropX, dropY - 34f)
        cubicTo(dropX + 30f, dropY, dropX + 24f, dropY + 34f, dropX, dropY + 34f)
        cubicTo(dropX - 24f, dropY + 34f, dropX - 30f, dropY, dropX, dropY - 34f)
        close()
    }
    drawPath(drop, Blue)
}

private fun DrawScope.drawEye() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val w = size.width * 0.46f
    val h = size.height * 0.46f
    val eye = Path().apply {
        moveTo(cx - w, cy)
        quadraticTo(cx, cy - h * 1.6f, cx + w, cy)
        quadraticTo(cx, cy + h * 1.6f, cx - w, cy)
        close()
    }
    drawPath(eye, Color.White)
    drawCircle(Green, radius = h * 0.78f, center = Offset(cx, cy))
    drawCircle(GreenDark, radius = h * 0.42f, center = Offset(cx, cy))
    drawCircle(Color(0xFF3D2B1F), radius = h * 0.24f, center = Offset(cx, cy))
    drawCircle(Color.White, radius = h * 0.10f, center = Offset(cx - h * 0.18f, cy - h * 0.18f))
    // 小星星
    drawCircle(Color(0xFFFFFFFF), radius = 6f, center = Offset(cx + w * 0.95f, cy - h * 1.1f))
    drawCircle(Color(0xFFFFFFFF), radius = 4f, center = Offset(cx - w * 0.9f, cy - h * 1.25f))
}

private fun DrawScope.drawJar() {
    val cx = size.width / 2f
    val w = size.width * 0.34f
    val h = size.height * 0.40f
    val top = size.height * 0.38f
    drawRoundRect(Orange, Offset(cx - w / 2, top), Size(w, h), CornerRadius(22f, 22f))
    drawRoundRect(OrangeDark, Offset(cx - w / 2 - 6f, top - 26f), Size(w + 12f, 30f), CornerRadius(12f, 12f))
    drawRoundRect(Color.White, Offset(cx - w * 0.32f, top + h * 0.22f), Size(w * 0.64f, h * 0.52f), CornerRadius(12f, 12f))
    // 腳掌印
    val pad = Offset(cx, top + h * 0.56f)
    drawCircle(Orange, 12f, pad)
    listOf(Offset(-16f, -18f), Offset(-6f, -26f), Offset(6f, -26f), Offset(16f, -18f)).forEach {
        drawCircle(Orange, 5f, pad + it)
    }
    // 葉子
    val lx = cx + w * 1.05f
    val ly = size.height * 0.55f
    val leaf = Path().apply {
        moveTo(lx, ly - 40f)
        quadraticTo(lx + 38f, ly - 4f, lx, ly + 40f)
        quadraticTo(lx - 38f, ly - 4f, lx, ly - 40f)
        close()
    }
    drawPath(leaf, Green)
}

private fun DrawScope.drawTube() {
    val cx = size.width / 2f
    val w = size.width * 0.30f
    val h = size.height * 0.56f
    val top = size.height * 0.22f
    val body = Path().apply {
        moveTo(cx - w / 2, top)
        lineTo(cx + w / 2, top)
        lineTo(cx + w * 0.42f, top + h)
        lineTo(cx - w * 0.42f, top + h)
        close()
    }
    drawPath(body, Color.White)
    drawRoundRect(GreenDark, Offset(cx - w * 0.43f, top + h - 10f), Size(w * 0.86f, 26f), CornerRadius(6f, 6f))
    drawRoundRect(Orange, Offset(cx - w * 0.40f, top - 34f), Size(w * 0.80f, 36f), CornerRadius(10f, 10f))
    // 葉片標誌
    val lx = cx
    val ly = top + h * 0.42f
    val leaf = Path().apply {
        moveTo(lx, ly - 30f)
        quadraticTo(lx + 28f, ly, lx, ly + 30f)
        quadraticTo(lx - 28f, ly, lx, ly - 30f)
        close()
    }
    drawPath(leaf, Green)
    drawLine(GreenDark, Offset(lx, ly - 24f), Offset(lx, ly + 24f), strokeWidth = 3f)
    // 草本小葉
    drawCircle(Green.copy(alpha = 0.55f), 14f, Offset(cx + w * 1.15f, size.height * 0.62f))
    drawCircle(Green.copy(alpha = 0.55f), 9f, Offset(cx + w * 1.5f, size.height * 0.5f))
}
