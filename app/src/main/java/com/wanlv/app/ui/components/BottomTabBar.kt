package com.wanlv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanlv.app.navigation.BottomNavItem
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvSurface
import com.wanlv.app.ui.theme.WanLvTextSecondary

@Composable
fun BottomTabBar(
    items: List<BottomNavItem>,
    currentRoute: String,
    onTabClick: (BottomNavItem) -> Unit
) {
    Row(
        modifier = Modifier
            .shadow(8.dp)
            .background(WanLvSurface)
            .navigationBarsPadding()
            .height(64.dp)
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        items.forEach { item ->
            val selected = item.route == currentRoute
            val color = if (selected) WanLvGreen else WanLvTextSecondary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabClick(item) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(22.dp),
                    tint = color
                )
                Spacer(Modifier.height(3.dp))
                Text(item.title, color = color, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
