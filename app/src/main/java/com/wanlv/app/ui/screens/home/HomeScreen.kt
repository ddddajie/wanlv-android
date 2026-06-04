package com.wanlv.app.ui.screens.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wanlv.app.R
import com.wanlv.app.ui.components.IOSCard
import com.wanlv.app.ui.components.ScenicSpotCard
import com.wanlv.app.ui.components.SectionHeader
import com.wanlv.app.ui.theme.WanLvBackground
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvGreenLight
import com.wanlv.app.ui.theme.WanLvMint
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary
import com.wanlv.app.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadScenicAreas() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WanLvBackground)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = 18.dp)
    ) {
        HomeTopBar()
        SectionHeader("游玩必玩", modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp))
        Text(
            viewModel.loadMessage.value,
            color = WanLvTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(viewModel.scenicSpots) { spot -> ScenicSpotCard(spot) }
        }
        SectionHeader("精选推荐路线", modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp))
        viewModel.scenicSpots.take(3).forEach { spot ->
            IOSCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                cornerRadius = 20.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .height(64.dp)
                            .width(82.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(WanLvMint, WanLvGreenLight.copy(alpha = 0.7f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("路线", color = WanLvGreen, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("${spot.name} 推荐路线", color = WanLvTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${spot.tag} · ${spot.location}", color = WanLvTextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.gg1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.06f),
                            Color.Black.copy(alpha = 0.24f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            // 顶部栏使用 gg1 山水图，叠加简短品牌信息保证首页第一眼聚焦。
            Text(
                text = "万旅",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "AI 规划路线，讲解与导航一步到位",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
