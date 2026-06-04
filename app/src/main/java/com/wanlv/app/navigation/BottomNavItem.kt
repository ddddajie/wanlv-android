package com.wanlv.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem("home", "首页", Icons.Rounded.Home)
    data object Map : BottomNavItem("map", "地图", Icons.Rounded.Map)
    data object Booking : BottomNavItem("booking", "预约", Icons.Rounded.CalendarMonth)
    data object Chat : BottomNavItem("chat", "问答", Icons.Rounded.ChatBubbleOutline)
    data object Profile : BottomNavItem("profile", "我的", Icons.Rounded.PersonOutline)
}
