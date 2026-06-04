package com.wanlv.app.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wanlv.app.ui.components.BottomTabBar
import com.wanlv.app.ui.screens.booking.BookingScreen
import com.wanlv.app.ui.screens.chat.ChatScreen
import com.wanlv.app.ui.screens.home.HomeScreen
import com.wanlv.app.ui.screens.map.MapScreen
import com.wanlv.app.ui.screens.profile.ProfileScreen

@Composable
fun WanLvNavGraph(navController: NavHostController) {
    val tabs = listOf(
        BottomNavItem.Home,
        BottomNavItem.Map,
        BottomNavItem.Booking,
        BottomNavItem.Chat,
        BottomNavItem.Profile
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: BottomNavItem.Home.route

    Scaffold(
        containerColor = Color.Transparent,
        // 顶部不预留系统栏，首页头图保持伪全屏；底部栏由 Scaffold 固定占位，避免内容重叠。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomTabBar(
                items = tabs,
                currentRoute = currentRoute,
                onTabClick = { item ->
                    navController.navigate(item.route) {
                        popUpTo(BottomNavItem.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen()
            }
            composable(BottomNavItem.Map.route) { MapScreen() }
            composable(BottomNavItem.Booking.route) { BookingScreen() }
            composable(BottomNavItem.Chat.route) { ChatScreen() }
            composable(BottomNavItem.Profile.route) { ProfileScreen() }
        }
    }
}
