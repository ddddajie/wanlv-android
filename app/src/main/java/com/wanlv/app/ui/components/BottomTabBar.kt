package com.wanlv.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import com.wanlv.app.navigation.BottomNavItem
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun BottomTabBar(
    items: List<BottomNavItem>,
    currentRoute: String,
    onTabClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassShape = RoundedCornerShape(42.dp)
    val selectedShape = RoundedCornerShape(32.dp)
    val accentColor = Color(0xFF22D4CC)
    val barBackground = Color(0xFFF8F9FB)
    val selectedBackground = Color(0xFFE8EAEE)
    val idleColor = Color(0xFF5F6873)
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }
    var burstFromIndex by remember { mutableIntStateOf(selectedIndex) }
    var burstToIndex by remember { mutableIntStateOf(selectedIndex) }
    val bubbleProgress = remember { Animatable(1f) }
    val isWaitingForSwitchAnimation = selectedIndex != previousIndex

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != previousIndex) {
            val startIndex = previousIndex
            previousIndex = selectedIndex
            burstFromIndex = startIndex
            burstToIndex = selectedIndex
            bubbleProgress.snapTo(0f)
            bubbleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            // 重点：减少底部留白，让液态玻璃导航栏更贴近屏幕底部。
            .padding(horizontal = 22.dp)
            .padding(top = 12.dp, bottom = 2.dp)
            .height(76.dp)
            .shadow(
                elevation = 24.dp,
                shape = glassShape,
                ambientColor = Color(0xFF9AA3AD).copy(alpha = 0.22f),
                spotColor = Color(0xFF737D88).copy(alpha = 0.16f)
            )
            .clip(glassShape)
            // 重点：不再采样页面内容，直接使用纯白灰背景，避免底纹干扰。
            .background(barBackground.copy(alpha = 0.74f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.72f),
                shape = glassShape
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        ActiveBubbleBackground(
            itemCount = items.size,
            selectedIndex = selectedIndex,
            fromIndex = if (isWaitingForSwitchAnimation) previousIndex else burstFromIndex,
            toIndex = if (isWaitingForSwitchAnimation) selectedIndex else burstToIndex,
            // 重点：路由切换后的首帧就使用动画起点，避免新激活项先闪出灰色背景。
            progress = if (isWaitingForSwitchAnimation) 0f else bubbleProgress.value,
            backgroundColor = selectedBackground,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items.forEach { item ->
                val selected = item.route == currentRoute
                val itemColor = if (selected) accentColor else idleColor
                val titleWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .padding(horizontal = 2.dp)
                        .clip(selectedShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabClick(item) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(if (selected) 26.dp else 24.dp),
                        tint = itemColor
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        color = itemColor,
                        fontSize = 11.sp,
                        fontWeight = titleWeight
                    )
                }
            }
        }
    }
}

@Composable
fun MapBottomBarRevealHandle(
    onReveal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 4.dp)
            .size(width = 58.dp, height = 36.dp)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = Color(0xFF8A96A3).copy(alpha = 0.24f),
                spotColor = Color(0xFF5F6873).copy(alpha = 0.18f)
            )
            .clip(shape)
            // 重点：收起导航栏后仅保留轻量的液态玻璃箭头，点击或上拉都可以恢复菜单。
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.86f),
                        Color(0xFFEAF4F2).copy(alpha = 0.72f),
                        Color.White.copy(alpha = 0.68f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.82f), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onReveal
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowUp,
            contentDescription = "展开导航菜单",
            tint = Color(0xFF46515C),
            modifier = Modifier.size(25.dp)
        )
    }
}

@Composable
private fun ActiveBubbleBackground(
    itemCount: Int,
    selectedIndex: Int,
    fromIndex: Int,
    toIndex: Int,
    progress: Float,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val borderColor = Color.White.copy(alpha = 0.56f)
    val bubbleOffsets = remember {
        listOf(
            Offset(-0.34f, -0.18f),
            Offset(-0.22f, 0.22f),
            Offset(-0.04f, -0.26f),
            Offset(0.14f, 0.24f),
            Offset(0.30f, -0.12f),
            Offset(0.02f, 0.02f)
        )
    }

    Canvas(modifier = modifier) {
        if (itemCount <= 0) return@Canvas

        val itemWidth = size.width / itemCount
        val pillWidth = itemWidth - 4.dp.toPx()
        val pillHeight = size.height

        fun pillTopLeft(index: Int): Offset {
            val left = itemWidth * index + (itemWidth - pillWidth) / 2f
            return Offset(left, 0f)
        }

        fun pillCenter(index: Int): Offset {
            val topLeft = pillTopLeft(index)
            return Offset(topLeft.x + pillWidth / 2f, topLeft.y + pillHeight / 2f)
        }

        fun ease(value: Float): Float {
            val t = value.coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }

        fun interval(start: Float, end: Float): Float =
            ((progress - start) / (end - start)).coerceIn(0f, 1f)

        fun lerp(start: Float, end: Float, fraction: Float): Float =
            start + (end - start) * fraction

        fun lerpOffset(start: Offset, end: Offset, fraction: Float): Offset =
            Offset(lerp(start.x, end.x, fraction), lerp(start.y, end.y, fraction))

        fun drawPill(index: Int, alpha: Float, scaleX: Float = 1f, scaleY: Float = 1f) {
            if (alpha <= 0f) return
            val center = pillCenter(index)
            val width = pillWidth * scaleX
            val height = pillHeight * scaleY
            val scaledTopLeft = Offset(center.x - width / 2f, center.y - height / 2f)
            val scaledCorner = CornerRadius(height / 2f, height / 2f)
            drawRoundRect(
                color = backgroundColor.copy(alpha = 0.72f * alpha),
                topLeft = scaledTopLeft,
                size = Size(width, height),
                cornerRadius = scaledCorner
            )
            drawRoundRect(
                color = borderColor.copy(alpha = alpha),
                topLeft = scaledTopLeft,
                size = Size(width, height),
                cornerRadius = scaledCorner,
                style = Stroke(width = 0.6.dp.toPx())
            )
        }

        // 重点：切换时同一组气泡从旧位置拆出，移动到新位置后再重组成胶囊。
        if (progress >= 0.99f || fromIndex == toIndex) {
            drawPill(selectedIndex, 1f)
        } else {
            val breakProgress = ease(interval(0f, 0.24f))
            val travelProgress = ease(interval(0.12f, 0.92f))
            val mergeProgress = ease(interval(0.86f, 1f))
            val oldCenter = pillCenter(fromIndex)
            val newCenter = pillCenter(toIndex)
            val baseRadius = pillHeight * 0.18f

            drawPill(
                index = fromIndex,
                alpha = 1f - breakProgress,
                scaleX = 1f - breakProgress * 0.34f,
                scaleY = 1f - breakProgress * 0.18f
            )

            val newPillScaleX = 0.18f + mergeProgress * 0.82f
            val newPillScaleY = 0.62f + mergeProgress * 0.38f
            drawPill(
                index = toIndex,
                alpha = mergeProgress,
                scaleX = newPillScaleX,
                scaleY = newPillScaleY
            )

            bubbleOffsets.forEachIndexed { index, offset ->
                val radiusScale = 1f - index * 0.075f
                val start = oldCenter + Offset(
                    x = offset.x * pillWidth * 0.34f * breakProgress,
                    y = offset.y * pillHeight * 0.46f * breakProgress
                )
                val end = newCenter + Offset(
                    x = offset.x * pillWidth * 0.18f * (1f - mergeProgress),
                    y = offset.y * pillHeight * 0.28f * (1f - mergeProgress)
                )
                val arc = sin(travelProgress * PI).toFloat() * pillHeight * 0.16f
                val center = lerpOffset(start, end, travelProgress) + Offset(0f, offset.y * arc)
                val appearAlpha = interval(0.06f, 0.18f)
                val disappearAlpha = 1f - interval(0.90f, 1f)
                val bubbleAlpha = (appearAlpha * disappearAlpha).coerceIn(0f, 1f)
                val radius = baseRadius * radiusScale * (0.82f + (1f - mergeProgress) * 0.18f)

                drawCircle(
                    color = backgroundColor.copy(alpha = bubbleAlpha),
                    radius = radius,
                    center = center
                )
            }
        }
    }
}
