package com.wanlv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wanlv.app.R
import com.wanlv.app.model.ScenicSpot
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary

@Composable
fun ScenicSpotCard(
    spot: ScenicSpot,
    modifier: Modifier = Modifier
) {
    IOSCard(
        modifier = modifier.width(154.dp),
        padding = androidx.compose.foundation.layout.PaddingValues(10.dp),
        cornerRadius = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(WanLvGreen.copy(alpha = 0.08f))
            ) {
                AsyncImage(
                    model = spot.coverImageUrl,
                    contentDescription = spot.name,
                    placeholder = painterResource(R.drawable.default_spot),
                    error = painterResource(R.drawable.default_spot),
                    fallback = painterResource(R.drawable.default_spot),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                ImageGradientMask(seed = spot.name)
            }
            Text(spot.name, color = WanLvTextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(spot.location, color = WanLvTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("★ ${spot.rating}", color = WanLvGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(2.dp))
        }
    }
}

@Composable
fun ImageGradientMask(
    seed: String,
    modifier: Modifier = Modifier
) {
    val variants = listOf(
        listOf(Color(0xFF0EA5E9).copy(alpha = 0.10f), Color(0xFF10B981).copy(alpha = 0.26f), Color.Black.copy(alpha = 0.16f)),
        listOf(Color(0xFFF59E0B).copy(alpha = 0.10f), Color(0xFF14B8A6).copy(alpha = 0.22f), Color.Black.copy(alpha = 0.18f)),
        listOf(Color(0xFF22C55E).copy(alpha = 0.08f), Color(0xFF3B82F6).copy(alpha = 0.22f), Color.Black.copy(alpha = 0.15f)),
        listOf(Color(0xFF06B6D4).copy(alpha = 0.10f), Color(0xFF84CC16).copy(alpha = 0.22f), Color.Black.copy(alpha = 0.16f))
    )
    val colors = variants[(seed.hashCode() and Int.MAX_VALUE) % variants.size]

    // 根据图片名称稳定选择蒙版，让每张景区图有细微差异但不会刷新乱跳。
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors))
    )
}
