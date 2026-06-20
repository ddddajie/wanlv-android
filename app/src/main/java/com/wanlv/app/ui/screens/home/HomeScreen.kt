package com.wanlv.app.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.shadow
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
import com.wanlv.app.ui.components.FloatingBottomBarAvoidance
import com.wanlv.app.ui.components.ImageGradientMask
import com.wanlv.app.ui.components.SectionHeader
import com.wanlv.app.ui.components.ScenicHeroHeader
import com.wanlv.app.ui.theme.WanLvBackground
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvGreenLight
import com.wanlv.app.ui.theme.WanLvMint
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
            .padding(bottom = FloatingBottomBarAvoidance)
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
            RecommendedRouteGlassCard(spot = spot)
        }
    }
}

@Composable
private fun RecommendedRouteGlassCard(spot: ScenicSpot) {
    val glassShape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .shadow(
                elevation = 18.dp,
                shape = glassShape,
                ambientColor = Color(0xFF9AA3AD).copy(alpha = 0.18f),
                spotColor = Color(0xFF64707C).copy(alpha = 0.12f)
            )
            .clip(glassShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.84f),
                        WanLvMint.copy(alpha = 0.64f),
                        Color.White.copy(alpha = 0.70f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.76f),
                shape = glassShape
            )
    ) {
        // 重点：用半透明高光和柔和色块叠加出液态玻璃的流动质感。
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 22.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(WanLvGreenLight.copy(alpha = 0.11f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 112.dp, bottom = 8.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.54f))
        )
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(66.dp)
                    .width(84.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(17.dp),
                        ambientColor = Color(0xFF000000).copy(alpha = 0.08f),
                        spotColor = Color(0xFF000000).copy(alpha = 0.06f)
                    )
                    .clip(RoundedCornerShape(17.dp))
            ) {
                // 推荐路线默认图使用用户新增的 tjlx 资源。
                Image(
                    painter = painterResource(id = R.drawable.tjlx),
                    contentDescription = "推荐路线",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                ImageGradientMask(seed = spot.name)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.08f))
                )
            }
            Column(Modifier.padding(start = 13.dp)) {
                Text(
                    text = "${spot.name} 推荐路线",
                    color = WanLvTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${spot.tag} · ${spot.location}",
                    color = WanLvTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
    ScenicHeroHeader(
        title = "今天想去哪玩？",
        subtitle = "一眼找到适合你的目的地"
    )
}
