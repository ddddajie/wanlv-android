package com.wanlv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanlv.app.ui.theme.WanLvDivider
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary

@Composable
fun QuantityStepper(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StepButton("−", enabled = quantity > 0) {
            onQuantityChange((quantity - 1).coerceAtLeast(0))
        }
        Text(quantity.toString(), color = WanLvTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        StepButton("+", enabled = true) {
            onQuantityChange(quantity + 1)
        }
    }
}

@Composable
private fun StepButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .size(28.dp)
            .background(if (enabled) WanLvGreen.copy(alpha = 0.12f) else WanLvDivider, CircleShape)
            .clickable(enabled = enabled) { onClick() },
        color = if (enabled) WanLvGreen else WanLvTextSecondary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}
