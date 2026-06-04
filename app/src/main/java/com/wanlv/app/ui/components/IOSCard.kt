package com.wanlv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wanlv.app.ui.theme.WanLvSurface

@Composable
fun IOSCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = androidx.compose.ui.graphics.Color(0x16000000),
                spotColor = androidx.compose.ui.graphics.Color(0x12000000)
            )
            .background(WanLvSurface, RoundedCornerShape(cornerRadius))
            .padding(padding)
    ) {
        content()
    }
}
