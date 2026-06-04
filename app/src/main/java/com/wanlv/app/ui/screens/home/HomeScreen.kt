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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.wanlv.app.R
import com.wanlv.app.model.ScenicSpot
import com.wanlv.app.ui.components.IOSCard
import com.wanlv.app.ui.components.ImageGradientMask
import com.wanlv.app.ui.components.SectionHeader
import com.wanlv.app.ui.theme.WanLvBackground
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary
import com.wanlv.app.viewmodel.HomeViewModel
import kotlin.math.absoluteValue

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
            .padding(bottom = 18.dp)
    ) {
        HomeTopBar()
        SectionHeader("游客必玩", modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp))
        Text(
            viewModel.loadMessage.value,
            color = WanLvTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        Spacer(Modifier.height(8.dp))
        if (viewModel.scenicSpots.isNotEmpty()) {
            FeaturedScenicCarousel(spots = viewModel.scenicSpots)
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
                    ) {
                        // 推荐路线默认图使用用户新增的 tjlx 资源。
                        Image(
                            painter = painterResource(id = R.drawable.tjlx),
                            contentDescription = "推荐路线",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        ImageGradientMask(seed = spot.name)
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
private fun FeaturedScenicCarousel(spots: List<ScenicSpot>) {
    val virtualPageCount = if (spots.size > 1) 10_000 else spots.size
    val initialPage = remember(spots.size) {
        if (spots.size > 1) {
            val middlePage = virtualPageCount / 2
            middlePage - middlePage % spots.size
        } else {
            0
        }
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { virtualPageCount }
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 50.dp),
            pageSpacing = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(238.dp)
        ) { page ->
            val spotIndex = page % spots.size
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val progress = 1f - pageOffset.coerceIn(0f, 1f)
            val scale = 0.88f + progress * 0.12f

            FeaturedScenicCard(
                spot = spots[spotIndex],
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.58f + progress * 0.42f
                    }
            )
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(spots.size) { index ->
                val selected = pagerState.currentPage % spots.size == index
                Box(
                    modifier = Modifier
                        .height(7.dp)
                        .width(if (selected) 20.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (selected) WanLvGreen else WanLvTextSecondary.copy(alpha = 0.22f))
                )
            }
        }
    }
}

@Composable
private fun FeaturedScenicCard(
    spot: ScenicSpot,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(WanLvGreen.copy(alpha = 0.10f))
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.34f),
                            Color.Black.copy(alpha = 0.62f)
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ScenicChip(text = spot.tag)
            ScenicChip(text = "推荐值 ${spot.rating}")
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Text(
                text = spot.name,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = spot.description,
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${spot.location}  ·  ${spot.distance}",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ScenicChip(text: String) {
    Text(
        text = text,
        color = WanLvGreen,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.86f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun HomeTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp)
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
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.04f),
                            Color.Black.copy(alpha = 0.30f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            WanLvBackground.copy(alpha = 0.72f),
                            WanLvBackground
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 34.dp)
        ) {
            // 顶部栏使用 gg1 山水图铺到状态栏后方，文字区单独避开状态栏。
            Text(
                text = "今天想去哪玩？",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "一眼找到适合你的目的地",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
