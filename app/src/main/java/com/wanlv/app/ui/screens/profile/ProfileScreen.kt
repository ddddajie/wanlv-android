package com.wanlv.app.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.wanlv.app.network.AuthSession
import com.wanlv.app.ui.components.FloatingBottomBarAvoidance
import com.wanlv.app.ui.components.LiquidGlassCard
import com.wanlv.app.ui.components.ScenicHeroHeader
import com.wanlv.app.ui.theme.WanLvBackground
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvGreenLight
import com.wanlv.app.ui.theme.WanLvMint
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary
import com.wanlv.app.viewmodel.LoginMode
import com.wanlv.app.viewmodel.ProfileViewModel

private enum class ProfileOverlay {
    EditProfile,
    RealName,
    AllReservations,
    PendingReservations,
    Logout
}

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onOverlayVisibilityChange: (Boolean) -> Unit = {}
) {
    LaunchedEffect(Unit) { viewModel.loadCurrentUser() }
    val isLoggedIn = AuthSession.userId != null && !AuthSession.token.isNullOrBlank()
    var activeOverlay by remember { mutableStateOf<ProfileOverlay?>(null) }
    var displayedOverlay by remember { mutableStateOf(ProfileOverlay.Logout) }
    val overlayVisible = activeOverlay != null
    val backgroundBlur by animateDpAsState(
        targetValue = if (overlayVisible) 18.dp else 0.dp,
        label = "profile-background-blur"
    )
    val veilAlpha by animateFloatAsState(
        targetValue = if (overlayVisible) 1f else 0f,
        label = "profile-background-veil"
    )

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) viewModel.clearUserWhenSessionMissing()
    }
    LaunchedEffect(activeOverlay) {
        activeOverlay?.let { displayedOverlay = it }
        onOverlayVisibilityChange(overlayVisible)
    }
    DisposableEffect(Unit) {
        onDispose { onOverlayVisibilityChange(false) }
    }
    BackHandler(enabled = overlayVisible) {
        activeOverlay = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 重点：任一资料、实名或订单弹窗出现时，都将页面信息整体虚化到背景中。
                .blur(backgroundBlur)
        ) {
            if (!isLoggedIn) {
                LoginScreen(viewModel)
            } else {
                ProfileContent(
                    viewModel = viewModel,
                    onEditProfileClick = {
                        viewModel.clearProfileActionMessage()
                        activeOverlay = ProfileOverlay.EditProfile
                    },
                    onRealNameClick = {
                        viewModel.clearProfileActionMessage()
                        activeOverlay = ProfileOverlay.RealName
                    },
                    onAllReservationsClick = {
                        activeOverlay = ProfileOverlay.AllReservations
                        viewModel.loadReservationOrders()
                    },
                    onPendingReservationsClick = {
                        activeOverlay = ProfileOverlay.PendingReservations
                        viewModel.loadReservationOrders()
                    },
                    onLogoutClick = { activeOverlay = ProfileOverlay.Logout }
                )
            }
        }

        if (overlayVisible || veilAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.24f * veilAlpha),
                                WanLvMint.copy(alpha = 0.18f * veilAlpha),
                                WanLvBackground.copy(alpha = 0.30f * veilAlpha)
                            )
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { activeOverlay = null }
                    )
            )
        }

        // 重点：弹窗进入时轻微放大回弹，退出时缩小淡出，与背景模糊动画同步衔接。
        AnimatedVisibility(
            visible = overlayVisible,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(animationSpec = tween(180)) + scaleIn(
                initialScale = 0.86f,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = 420f
                )
            ),
            exit = fadeOut(animationSpec = tween(140)) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(160)
            )
        ) {
            when (displayedOverlay) {
                ProfileOverlay.EditProfile -> EditProfileGlassDialog(
                    user = viewModel.currentUser.value,
                    loading = viewModel.profileActionLoading.value,
                    message = viewModel.profileActionMessage.value,
                    messageIsError = viewModel.profileActionMessageIsError.value,
                    onDismiss = { activeOverlay = null },
                    onSubmit = { request ->
                        viewModel.updateProfile(request) { activeOverlay = null }
                    }
                )
                ProfileOverlay.RealName -> RealNameGlassDialog(
                    user = viewModel.currentUser.value,
                    loading = viewModel.profileActionLoading.value,
                    message = viewModel.profileActionMessage.value,
                    messageIsError = viewModel.profileActionMessageIsError.value,
                    onDismiss = { activeOverlay = null },
                    onSubmit = { realName, idCardNo, clearSensitiveInput ->
                        viewModel.verifyRealName(realName, idCardNo) {
                            clearSensitiveInput()
                            activeOverlay = null
                        }
                    }
                )
                ProfileOverlay.AllReservations,
                ProfileOverlay.PendingReservations -> ReservationOrdersGlassDialog(
                    orders = viewModel.reservationOrders,
                    pendingOnly = displayedOverlay == ProfileOverlay.PendingReservations,
                    loading = viewModel.reservationOrdersLoading.value,
                    message = viewModel.reservationOrdersMessage.value,
                    messageIsError = viewModel.reservationOrdersMessageIsError.value,
                    enteringReservationNo = viewModel.enteringReservationNo.value,
                    onDismiss = { activeOverlay = null },
                    onRefresh = viewModel::loadReservationOrders,
                    onEnter = viewModel::enterReservationOrder
                )
                ProfileOverlay.Logout -> LogoutConfirmationCard(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    isLoading = viewModel.isLoading.value,
                    onCancel = { activeOverlay = null },
                    onConfirm = {
                        activeOverlay = null
                        viewModel.logout()
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    viewModel: ProfileViewModel,
    onEditProfileClick: () -> Unit,
    onRealNameClick: () -> Unit,
    onAllReservationsClick: () -> Unit,
    onPendingReservationsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WanLvBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = FloatingBottomBarAvoidance)
    ) {
        // 重点：头图与用户卡片交叠，让身份信息成为“我的”页第一视觉焦点。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(408.dp)
        ) {
            ScenicHeroHeader(
                title = "我的旅程",
                subtitle = "山水有约，下一程依然值得期待",
                height = 300.dp
            )
            UserProfileGlassCard(
                viewModel = viewModel,
                onRealNameClick = onRealNameClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp)
            )
        }

        ProfileSectionTitle(
            title = "预约记录",
            subtitle = "查看行程安排与待出发预约",
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileGlassActionCard(
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                title = "所有订单",
                description = "查看全部预约记录",
                modifier = Modifier.weight(1f),
                onClick = onAllReservationsClick
            )
            ProfileGlassActionCard(
                icon = Icons.Rounded.EventAvailable,
                title = "待使用",
                description = "查看即将出发的行程",
                modifier = Modifier.weight(1f),
                onClick = onPendingReservationsClick
            )
        }

        ProfileSectionTitle(
            title = "账户管理",
            subtitle = "管理个人资料与登录状态",
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp)
        )
        Column(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileGlassMenuItem(
                icon = Icons.Rounded.PersonOutline,
                title = "修改个人信息",
                description = "更新头像、昵称和联系方式",
                onClick = onEditProfileClick
            )
            ProfileGlassMenuItem(
                icon = Icons.Rounded.Lock,
                title = "退出登录",
                description = "安全退出当前账号",
                accentColor = Color(0xFFB84A4A),
                onClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun LoginScreen(viewModel: ProfileViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WanLvBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(670.dp)
        ) {
            ScenicHeroHeader(
                title = "欢迎回来",
                subtitle = "登录后，继续发现你的下一程风景"
            )
            LoginCard(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 218.dp, start = 18.dp, end = 18.dp)
            )
        }
        Spacer(Modifier.height(FloatingBottomBarAvoidance))
    }
}

@Composable
private fun LoginCard(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val glassShape = RoundedCornerShape(30.dp)
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 26.dp,
                shape = glassShape,
                ambientColor = Color(0xFF5B7F78).copy(alpha = 0.22f),
                spotColor = Color(0xFF3D665E).copy(alpha = 0.16f)
            )
            .clip(glassShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.88f),
                        WanLvMint.copy(alpha = 0.70f),
                        Color.White.copy(alpha = 0.74f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.84f), glassShape)
    ) {
        // 重点：多层半透明色块、柔光和高光边缘共同形成液态玻璃质感。
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 10.dp)
                .size(86.dp)
                .blur(24.dp)
                .background(Color.White.copy(alpha = 0.72f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White,
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (viewModel.loginMode.value == LoginMode.PhoneCode) {
                        "手机号登录"
                    } else {
                        "账号登录"
                    },
                    color = WanLvTextPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                // 重点：登录方式切换收进卡片右上角，与标题同排，减少纵向空间占用。
                LoginModeSelector(
                    selectedMode = viewModel.loginMode.value,
                    onModeSelected = viewModel::switchLoginMode
                )
            }
            Text(
                text = "登录万旅，收藏沿途心动与每一次出发",
                color = WanLvTextSecondary,
                fontSize = 13.sp
            )
            if (viewModel.loginMode.value == LoginMode.PhoneCode) {
                PhoneCodeLoginFields(viewModel, focusManager::clearFocus)
            } else {
                PasswordLoginFields(viewModel, focusManager::clearFocus)
            }
            Text(
                text = viewModel.message.value,
                color = if (viewModel.messageIsError.value) Color(0xFFB84A4A) else WanLvTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (viewModel.loginMode.value == LoginMode.PhoneCode) {
                        viewModel.loginWithPhoneCode()
                    } else {
                        viewModel.login()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .alpha(if (viewModel.isLoading.value) 0.72f else 1f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(WanLvGreen, WanLvGreenLight)
                        )
                    ),
                enabled = !viewModel.isLoading.value,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            ) {
                if (viewModel.isLoading.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("登录", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.size(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = if (viewModel.loginMode.value == LoginMode.PhoneCode) {
                    "未注册手机号验证后将自动创建普通用户"
                } else {
                    "登录后可同步收藏、预约及个性化推荐"
                },
                color = WanLvTextSecondary.copy(alpha = 0.82f),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun LoginModeSelector(
    selectedMode: LoginMode,
    onModeSelected: (LoginMode) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(WanLvTextPrimary.copy(alpha = 0.05f))
            .padding(3.dp)
    ) {
        listOf(
            LoginMode.PhoneCode to "手机号",
            LoginMode.Password to "账号"
        ).forEach { (mode, title) ->
            val selected = selectedMode == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (selected) Color.White.copy(alpha = 0.80f) else Color.Transparent)
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (selected) WanLvGreen else WanLvTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PhoneCodeLoginFields(
    viewModel: ProfileViewModel,
    clearFocus: () -> Unit
) {
    OutlinedTextField(
        value = viewModel.phone.value,
        onValueChange = viewModel::updatePhone,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Rounded.PhoneAndroid, contentDescription = null)
        },
        placeholder = { Text("请输入手机号") },
        shape = RoundedCornerShape(18.dp),
        colors = loginFieldColors(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        )
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = viewModel.verificationCode.value,
            onValueChange = viewModel::updateVerificationCode,
            modifier = Modifier.weight(1f),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Rounded.Sms, contentDescription = null)
            },
            placeholder = { Text("6 位验证码") },
            shape = RoundedCornerShape(18.dp),
            colors = loginFieldColors(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    clearFocus()
                    viewModel.loginWithPhoneCode()
                }
            )
        )
        val canSendCode = !viewModel.isSendingCode.value &&
            viewModel.codeCountdownSeconds.intValue <= 0
        Button(
            onClick = viewModel::sendPhoneCode,
            modifier = Modifier
                .width(112.dp)
                .height(56.dp),
            enabled = canSendCode,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WanLvMint.copy(alpha = 0.90f),
                contentColor = WanLvGreen,
                disabledContainerColor = Color.White.copy(alpha = 0.42f),
                disabledContentColor = WanLvTextSecondary.copy(alpha = 0.70f)
            )
        ) {
            when {
                viewModel.isSendingCode.value -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = WanLvGreen,
                    strokeWidth = 2.dp
                )
                viewModel.codeCountdownSeconds.intValue > 0 -> Text(
                    text = "${viewModel.codeCountdownSeconds.intValue}s 后重试",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                else -> Text("获取验证码", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PasswordLoginFields(
    viewModel: ProfileViewModel,
    clearFocus: () -> Unit
) {
    OutlinedTextField(
        value = viewModel.username.value,
        onValueChange = viewModel::updateUsername,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Rounded.PersonOutline, contentDescription = null)
        },
        placeholder = { Text("请输入账号 / 用户名") },
        shape = RoundedCornerShape(18.dp),
        colors = loginFieldColors(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        )
    )
    OutlinedTextField(
        value = viewModel.password.value,
        onValueChange = viewModel::updatePassword,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        leadingIcon = {
            Icon(Icons.Rounded.Lock, contentDescription = null)
        },
        placeholder = { Text("请输入密码") },
        shape = RoundedCornerShape(18.dp),
        colors = loginFieldColors(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                clearFocus()
                viewModel.login()
            }
        )
    )
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White.copy(alpha = 0.58f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.46f),
    focusedBorderColor = WanLvGreen.copy(alpha = 0.58f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.88f),
    focusedLeadingIconColor = WanLvGreen,
    unfocusedLeadingIconColor = WanLvTextSecondary.copy(alpha = 0.72f),
    focusedPlaceholderColor = WanLvTextSecondary.copy(alpha = 0.72f),
    unfocusedPlaceholderColor = WanLvTextSecondary.copy(alpha = 0.62f)
)

@Composable
private fun UserProfileGlassCard(
    viewModel: ProfileViewModel,
    onRealNameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remoteUser = viewModel.currentUser.value
    val nickname = remoteUser?.displayName ?: remoteUser?.nickname ?: remoteUser?.username ?: "旅行者"
    val account = remoteUser?.username?.takeIf { it.isNotBlank() } ?: "资料同步中"
    val phone = maskPhone(remoteUser?.phone)
    // 重点：实名状态完全以后端 realNameStatus 为准，不能再回退到 Mock 数据。
    val realNameStatusText = when (remoteUser?.realNameStatus) {
        1 -> "已实名"
        2 -> "实名失败"
        else -> "未实名"
    }
    val realNameStatusColor = when (remoteUser?.realNameStatus) {
        1 -> WanLvGreen
        2 -> Color(0xFFB84A4A)
        else -> WanLvTextSecondary
    }
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 28.dp,
        padding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.92f),
                                WanLvMint,
                                WanLvGreenLight.copy(alpha = 0.44f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.90f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!remoteUser?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = remoteUser?.avatarUrl,
                        contentDescription = "用户头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = nickname.take(1).ifBlank { "旅" },
                        color = WanLvGreen,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 15.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = nickname,
                    color = WanLvTextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = remoteUser?.realName
                        ?.takeIf { remoteUser.realNameStatus == 1 && it.isNotBlank() }
                        ?.let { "实名用户 · $it" }
                        ?: "普通用户",
                    color = WanLvTextSecondary,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(realNameStatusColor.copy(alpha = 0.10f))
                    .border(
                        1.dp,
                        realNameStatusColor.copy(alpha = 0.22f),
                        RoundedCornerShape(14.dp)
                    )
                    // 重点：实名状态标签本身就是认证入口，减少账户管理区的重复按钮。
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRealNameClick
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = realNameStatusText,
                    color = realNameStatusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.72f))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileInfoValue(
                label = "登录账号",
                value = account,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(34.dp)
                    .background(WanLvTextSecondary.copy(alpha = 0.12f))
            )
            ProfileInfoValue(
                label = "手机号码",
                value = phone,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 18.dp)
            )
        }
    }
}

@Composable
private fun ProfileInfoValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = WanLvTextSecondary, fontSize = 11.sp)
        Text(value, color = WanLvTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileSectionTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = WanLvTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = WanLvTextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ProfileGlassActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = modifier
            .height(148.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        cornerRadius = 24.dp,
        padding = PaddingValues(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(WanLvGreen.copy(alpha = 0.11f))
                .border(1.dp, Color.White.copy(alpha = 0.78f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = WanLvGreen, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(title, color = WanLvTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(description, color = WanLvTextSecondary, fontSize = 11.sp)
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = WanLvGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ProfileGlassMenuItem(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color = WanLvGreen,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        cornerRadius = 22.dp,
        padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(accentColor.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.76f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(21.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 13.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(title, color = WanLvTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(description, color = WanLvTextSecondary, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.46f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun maskPhone(phone: String?): String {
    if (phone.isNullOrBlank()) return "未绑定"
    return if (phone.length == 11) {
        "${phone.take(3)}****${phone.takeLast(4)}"
    } else {
        phone
    }
}

@Composable
private fun LogoutConfirmationCard(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            // 重点：弹窗本体消费点击事件，只有点击弹窗外的雾面区域才会取消。
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        cornerRadius = 28.dp,
        padding = PaddingValues(horizontal = 22.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.56f))
                    .border(1.dp, Color.White.copy(alpha = 0.82f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = WanLvGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "确认退出登录？",
                color = WanLvTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "退出后将无法继续查看和管理当前账号的预约记录。",
                color = WanLvTextSecondary,
                fontSize = 13.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.62f),
                        contentColor = WanLvTextPrimary
                    )
                ) {
                    Text("取消", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB84A4A),
                        contentColor = Color.White
                    )
                ) {
                    Text("确认退出", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
