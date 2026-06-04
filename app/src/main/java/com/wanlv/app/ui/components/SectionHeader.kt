package com.wanlv.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    showMore: Boolean = true,
    onMoreClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = WanLvTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        if (showMore) {
            Text(
                text = "更多 >",
                modifier = Modifier.clickable { onMoreClick() },
                color = WanLvTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}
