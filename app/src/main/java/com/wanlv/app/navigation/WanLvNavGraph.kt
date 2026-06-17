package com.wanlv.app.navigation

import android.os.SystemClock
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wanlv.app.ui.components.BottomTabBar
import com.wanlv.app.ui.screens.booking.BookingScreen
import com.wanlv.app.ui.screens.chat.ChatScreen
import com.wanlv.app.ui.screens.developer.DeveloperSettingsScreen
import com.wanlv.app.ui.screens.home.HomeScreen
import com.wanlv.app.ui.screens.map.MapScreen
import com.wanlv.app.ui.screens.profile.ProfileScreen

private const val DeveloperModeRoute = "developer_mode"
private const val DeveloperModeTapCount = 7
private const val DeveloperModeTapWindowMillis = 1800L

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
    var homeTapCount by remember { mutableIntStateOf(0) }
    var lastHomeTapAt by remember { mutableStateOf(0L) }
    var bookingOverlayVisible by remember { mutableStateOf(false) }
    val blurBottomBar = currentRoute == BottomNavItem.Booking.route && bookingOverlayVisible
    val bottomBarBlur by animateDpAsState(
        targetValue = if (blurBottomBar) 18.dp else 0.dp,
        label = "bottom-bar-blur"
    )
    val bottomBarVeilAlpha by animateFloatAsState(
        targetValue = if (blurBottomBar) 1f else 0f,
        label = "bottom-bar-veil"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen()
            }
            composable(BottomNavItem.Map.route) { MapScreen() }
            composable(BottomNavItem.Booking.route) {
                BookingScreen(onOverlayVisibilityChange = { bookingOverlayVisible = it })
            }
            composable(BottomNavItem.Chat.route) { ChatScreen() }
            composable(BottomNavItem.Profile.route) { ProfileScreen() }
            composable(DeveloperModeRoute) {
                DeveloperSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (currentRoute != DeveloperModeRoute) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                BottomTabBar(
                    items = tabs,
                    currentRoute = currentRoute,
                    modifier = Modifier.blur(bottomBarBlur),
                    onTabClick = { item ->
                        if (!blurBottomBar) {
                            val now = SystemClock.elapsedRealtime()
                            var openDeveloperMode = false

                            if (item == BottomNavItem.Home) {
                                homeTapCount = if (now - lastHomeTapAt <= DeveloperModeTapWindowMillis) {
                                    homeTapCount + 1
                                } else {
                                    1
                                }
                                lastHomeTapAt = now
                                // 重点：首页按钮连续点击 7 次打开开发者模式，入口隐藏但调试时足够顺手。
                                if (homeTapCount >= DeveloperModeTapCount) {
                                    homeTapCount = 0
                                    lastHomeTapAt = 0L
                                    openDeveloperMode = true
                                }
                            } else {
                                homeTapCount = 0
                                lastHomeTapAt = 0L
                            }

                            if (openDeveloperMode) {
                                navController.navigate(DeveloperModeRoute) {
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate(item.route) {
                                    popUpTo(BottomNavItem.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                )

                if (blurBottomBar || bottomBarVeilAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .padding(top = 12.dp, bottom = 2.dp)
                            .height(76.dp)
                            .clip(RoundedCornerShape(42.dp))
                            // 重点：底部导航栏在页面外层绘制，预约弹窗打开时单独补一层雾面，保证和页面内容一起退到背景里。
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.20f * bottomBarVeilAlpha),
                                        Color(0xFFE7F7F1).copy(alpha = 0.30f * bottomBarVeilAlpha),
                                        Color.White.copy(alpha = 0.24f * bottomBarVeilAlpha)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
