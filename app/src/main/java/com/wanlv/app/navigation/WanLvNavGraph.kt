package com.wanlv.app.navigation

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wanlv.app.ui.components.BottomTabBar
import com.wanlv.app.ui.components.MapBottomBarRevealHandle
import com.wanlv.app.ui.screens.booking.BookingScreen
import com.wanlv.app.ui.screens.chat.ChatScreen
import com.wanlv.app.ui.screens.developer.DeveloperSettingsScreen
import com.wanlv.app.ui.screens.home.HomeScreen
import com.wanlv.app.ui.screens.map.MapScreen
import com.wanlv.app.ui.screens.profile.ProfileScreen
import kotlinx.coroutines.delay

private const val DeveloperModeRoute = "developer_mode"
private const val DeveloperModeTapCount = 7
private const val DeveloperModeTapWindowMillis = 1800L
private const val MapBottomBarAutoHideDelayMillis = 10_000L
private const val ChatBottomBarAutoHideDelayMillis = 2_000L

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
    var mapBottomBarExpanded by remember { mutableStateOf(true) }
    var chatBottomBarExpanded by remember { mutableStateOf(true) }
    var mapDigitalHumanVisible by remember { mutableStateOf(false) }
    val isMapRoute = currentRoute == BottomNavItem.Map.route
    val isChatRoute = currentRoute == BottomNavItem.Chat.route
    val isHideableBottomBarRoute = isMapRoute || isChatRoute
    val currentBottomBarExpanded = when {
        isMapRoute -> mapBottomBarExpanded
        isChatRoute -> chatBottomBarExpanded
        else -> true
    }
    val blurBottomBar = currentRoute == BottomNavItem.Booking.route && bookingOverlayVisible
    val bottomBarBlur by animateDpAsState(
        targetValue = if (blurBottomBar) 18.dp else 0.dp,
        label = "bottom-bar-blur"
    )
    val bottomBarVeilAlpha by animateFloatAsState(
        targetValue = if (blurBottomBar) 1f else 0f,
        label = "bottom-bar-veil"
    )
    val bottomBarOffset by animateDpAsState(
        targetValue = if (isHideableBottomBarRoute && !currentBottomBarExpanded) 128.dp else 0.dp,
        animationSpec = tween(durationMillis = 320),
        label = "hideable-bottom-bar-offset"
    )

    LaunchedEffect(currentRoute) {
        when {
            isMapRoute -> {
                // 重点：进入地图先保留导航入口，10 秒后再自动收起，避免用户刚进入时找不到菜单。
                mapBottomBarExpanded = true
                delay(MapBottomBarAutoHideDelayMillis)
                mapBottomBarExpanded = false
            }
            isChatRoute -> {
                mapDigitalHumanVisible = false
                // 重点：问答页进入后保留导航入口 2 秒，再自动收起给输入区域腾出空间。
                chatBottomBarExpanded = true
                delay(ChatBottomBarAutoHideDelayMillis)
                chatBottomBarExpanded = false
            }
            else -> mapDigitalHumanVisible = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen()
            }
            composable(BottomNavItem.Map.route) {
                MapScreen(
                    bottomBarExpanded = mapBottomBarExpanded,
                    onDigitalHumanVisibilityChange = { mapDigitalHumanVisible = it },
                    onRequestBottomBarExpand = { mapBottomBarExpanded = true },
                    onRequestBottomBarCollapse = { mapBottomBarExpanded = false }
                )
            }
            composable(BottomNavItem.Booking.route) {
                BookingScreen(onOverlayVisibilityChange = { bookingOverlayVisible = it })
            }
            composable(BottomNavItem.Chat.route) {
                ChatScreen(
                    bottomBarExpanded = chatBottomBarExpanded,
                    onRequestBottomBarExpand = { chatBottomBarExpanded = true },
                    onRequestBottomBarCollapse = { chatBottomBarExpanded = false }
                )
            }
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
                    modifier = Modifier
                        .blur(bottomBarBlur)
                        .offset(y = bottomBarOffset)
                        .pointerInput(isHideableBottomBarRoute, currentBottomBarExpanded) {
                            if (!isHideableBottomBarRoute || !currentBottomBarExpanded) return@pointerInput
                            var downwardDrag = 0f
                            detectVerticalDragGestures(
                                onDragStart = { downwardDrag = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    downwardDrag = (downwardDrag + dragAmount).coerceAtLeast(0f)
                                },
                                onDragEnd = {
                                    // 重点：导航栏向下拖过约 24dp 后收起，轻微滑动不会误触。
                                    if (downwardDrag >= 24.dp.toPx()) {
                                        if (isMapRoute) mapBottomBarExpanded = false
                                        if (isChatRoute) chatBottomBarExpanded = false
                                    }
                                }
                            )
                        },
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
                                when (item) {
                                    BottomNavItem.Map -> mapBottomBarExpanded = true
                                    BottomNavItem.Chat -> chatBottomBarExpanded = true
                                    else -> Unit
                                }
                                navController.navigate(item.route) {
                                    popUpTo(BottomNavItem.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                )

                AnimatedVisibility(
                    visible = isMapRoute && !mapBottomBarExpanded && !mapDigitalHumanVisible,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 2 },
                    exit = fadeOut(tween(120)) + slideOutVertically(tween(160)) { it / 2 }
                ) {
                    MapBottomBarRevealHandle(
                        onReveal = { mapBottomBarExpanded = true },
                        modifier = Modifier.pointerInput(Unit) {
                            var upwardDrag = 0f
                            detectVerticalDragGestures(
                                onDragStart = { upwardDrag = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    upwardDrag = (upwardDrag + dragAmount).coerceAtMost(0f)
                                },
                                onDragEnd = {
                                    if (upwardDrag <= -18.dp.toPx()) mapBottomBarExpanded = true
                                }
                            )
                        }
                    )
                }

                if (blurBottomBar || bottomBarVeilAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
//                            .padding(top = 1.dp, bottom = 1.dp)
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
