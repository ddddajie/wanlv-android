package com.wanlv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wanlv.app.ui.theme.WanLvMint
import com.wanlv.app.ui.theme.WanLvSurface

/**
 * 重点：弹窗和底部面板统一使用同一套液态玻璃背景、高光描边与柔和阴影。
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .shadow(
                elevation = 22.dp,
                shape = shape,
                ambientColor = Color(0xFF8FA0AE).copy(alpha = 0.18f),
                spotColor = Color(0xFF64727F).copy(alpha = 0.13f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.88f),
                        WanLvSurface.copy(alpha = 0.68f),
                        WanLvMint.copy(alpha = 0.36f),
                        Color.White.copy(alpha = 0.76f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.80f), shape)
            .padding(padding),
        content = content
    )
}
