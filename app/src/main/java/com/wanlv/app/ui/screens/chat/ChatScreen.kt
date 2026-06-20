package com.wanlv.app.ui.screens.chat

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wanlv.app.model.ChatMessage
import com.wanlv.app.ui.components.FloatingBottomBarAvoidance
import com.wanlv.app.ui.theme.WanLvBackground
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvGreenLight
import com.wanlv.app.ui.theme.WanLvMint
import com.wanlv.app.ui.theme.WanLvSurface
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary
import com.wanlv.app.viewmodel.ChatDigitalHumanPersona
import com.wanlv.app.viewmodel.ChatDigitalHumanUiState
import com.wanlv.app.viewmodel.ChatViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull

private const val DigitalHumanPersonaHoldMillis = 3_000L

private enum class DigitalHumanHoldResult {
    Released,
    DragStarted
}

private fun constrainChatDigitalHumanDragOffset(
    dragOffset: Offset,
    viewportSize: IntSize,
    windowSize: IntSize,
    basePosition: Offset,
    safeBottomPx: Float
): Offset {
    if (viewportSize == IntSize.Zero || windowSize == IntSize.Zero) return dragOffset

    val minX = -basePosition.x
    val maxX = (viewportSize.width - windowSize.width - basePosition.x).coerceAtLeast(minX)
    val minY = -basePosition.y
    val maxY = (viewportSize.height - safeBottomPx - windowSize.height - basePosition.y)
        .coerceAtLeast(minY)
    return Offset(
        x = dragOffset.x.coerceIn(minX, maxX),
        y = dragOffset.y.coerceIn(minY, maxY)
    )
}

private suspend fun PointerInputScope.detectFloatingDigitalHumanGestures(
    onDrag: (Offset) -> Unit,
    onShowPersonaPicker: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
        var initialDragAmount = Offset.Zero
        val personaDelayMillis = (DigitalHumanPersonaHoldMillis - viewConfiguration.longPressTimeoutMillis)
            .coerceAtLeast(0L)
        val holdResult = withTimeoutOrNull(personaDelayMillis) {
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == longPress.id }
                    ?: return@withTimeoutOrNull DigitalHumanHoldResult.Released
                if (!change.pressed) return@withTimeoutOrNull DigitalHumanHoldResult.Released

                val displacement = change.position - longPress.position
                if (displacement.getDistance() >= viewConfiguration.touchSlop) {
                    initialDragAmount = displacement
                    change.consume()
                    return@withTimeoutOrNull DigitalHumanHoldResult.DragStarted
                }
            }
        }

        when (holdResult) {
            null -> onShowPersonaPicker()
            DigitalHumanHoldResult.Released -> Unit
            DigitalHumanHoldResult.DragStarted -> {
                onDrag(initialDragAmount)
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == longPress.id } ?: break
                    if (!change.pressed) {
                        change.consume()
                        break
                    }
                    val dragAmount = change.positionChange()
                    if (dragAmount != Offset.Zero) {
                        change.consume()
                        onDrag(dragAmount)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(
    bottomBarExpanded: Boolean = true,
    onRequestBottomBarExpand: () -> Unit = {},
    onRequestBottomBarCollapse: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val digitalHumanState = viewModel.digitalHumanState
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var showPersonaPicker by remember { mutableStateOf(false) }
    var chatViewportSize by remember { mutableStateOf(IntSize.Zero) }
    var digitalHumanWindowSize by remember { mutableStateOf(IntSize.Zero) }
    var digitalHumanDragOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navigationBarInsetPx = with(density) { navigationBarInset.toPx() }
    val digitalHumanBasePosition = with(density) {
        Offset(x = 18.dp.toPx(), y = 4.dp.toPx())
    }
    val inputBottomAvoidance by animateDpAsState(
        targetValue = when {
            // 重点：键盘出现时只使用 imePadding 避让，不能再叠加导航栏留白，否则输入框会被重复上推。
            isImeVisible -> 0.dp
            bottomBarExpanded -> FloatingBottomBarAvoidance
            else -> navigationBarInset + 8.dp
        },
        label = "chat-input-bottom-avoidance"
    )
    val messageTopPadding by animateDpAsState(
        targetValue = if (digitalHumanState.connected) 164.dp else 74.dp,
        label = "chat-message-top-padding"
    )

    LaunchedEffect(Unit) {
        viewModel.refreshScenicContext()
    }

    LaunchedEffect(
        chatViewportSize,
        digitalHumanWindowSize,
        digitalHumanBasePosition,
        navigationBarInsetPx
    ) {
        // 重点：屏幕方向、键盘或系统栏变化后收紧可拖动范围，确保数字人始终能够被再次操作。
        digitalHumanDragOffset = constrainChatDigitalHumanDragOffset(
            dragOffset = digitalHumanDragOffset,
            viewportSize = chatViewportSize,
            windowSize = digitalHumanWindowSize,
            basePosition = digitalHumanBasePosition,
            safeBottomPx = navigationBarInsetPx
        )
    }

    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onHostStopped()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            // 重点：底部导航保留 ViewModel 时，离开聊天页也要主动归还数字人 session。
            viewModel.onRouteDisposed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WanLvBackground)
            .statusBarsPadding()
            .imePadding()
            .onSizeChanged { chatViewportSize = it }
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = messageTopPadding,
                    end = 18.dp,
                    bottom = 10.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { ChatUsageTip() }
                viewModel.scenicContext?.let { scenicContext ->
                    item { ScenicContextMessage(scenicContext.scenicAreaName) }
                }
                items(viewModel.messages, key = { it.id }) { message -> ChatBubble(message) }
            }
            ChatInputBar(
                value = viewModel.input.value,
                onValueChange = viewModel::updateInput,
                onSend = viewModel::sendMessage,
                modifier = Modifier
                    .padding(bottom = inputBottomAvoidance)
                    .pointerInput(bottomBarExpanded) {
                        var verticalDragDistance = 0f
                        detectVerticalDragGestures(
                            onDragStart = { verticalDragDistance = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                verticalDragDistance += dragAmount
                            },
                            onDragEnd = {
                                // 重点：上下滑动问答输入框即可展开或收起导航栏，轻微滑动不会误触。
                                when {
                                    !bottomBarExpanded && verticalDragDistance <= -18.dp.toPx() -> onRequestBottomBarExpand()
                                    bottomBarExpanded && verticalDragDistance >= 18.dp.toPx() -> onRequestBottomBarCollapse()
                                }
                            }
                        )
                    }
            )
        }

        DigitalHumanConnectionButton(
            state = digitalHumanState,
            onClick = viewModel::toggleDigitalHumanConnection,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 16.dp)
        )

        AnimatedVisibility(
            visible = digitalHumanState.connected,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        x = (digitalHumanBasePosition.x + digitalHumanDragOffset.x).roundToInt(),
                        y = (digitalHumanBasePosition.y + digitalHumanDragOffset.y).roundToInt()
                    )
                },
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FloatingDigitalHuman(
                state = digitalHumanState,
                previewBitmap = viewModel.currentDigitalHumanPreviewBitmap,
                onDrag = { dragAmount ->
                    digitalHumanDragOffset = constrainChatDigitalHumanDragOffset(
                        dragOffset = digitalHumanDragOffset + dragAmount,
                        viewportSize = chatViewportSize,
                        windowSize = digitalHumanWindowSize,
                        basePosition = digitalHumanBasePosition,
                        safeBottomPx = navigationBarInsetPx
                    )
                },
                onShowPersonaPicker = { showPersonaPicker = true },
                modifier = Modifier.onSizeChanged { digitalHumanWindowSize = it }
            )
        }
    }

    if (showPersonaPicker) {
        DigitalHumanPersonaDialog(
            currentPersona = digitalHumanState.persona,
            previewBitmap = viewModel::previewBitmap,
            enabled = !digitalHumanState.connecting,
            onDismiss = { showPersonaPicker = false },
            onPersonaSelected = { persona ->
                showPersonaPicker = false
                viewModel.switchDigitalHumanPersona(persona)
            }
        )
    }
}

@Composable
private fun DigitalHumanConnectionButton(
    state: ChatDigitalHumanUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haloTransition = rememberInfiniteTransition(label = "digital-human-connection-halo")
    val haloScale by haloTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_250),
            repeatMode = RepeatMode.Reverse
        ),
        label = "digital-human-connection-halo-scale"
    )
    val haloAlpha by haloTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.76f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_250),
            repeatMode = RepeatMode.Reverse
        ),
        label = "digital-human-connection-halo-alpha"
    )
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = Color(0xFF8A96A3).copy(alpha = 0.22f),
                spotColor = Color(0xFF5F6873).copy(alpha = 0.16f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.84f),
                        WanLvSurface.copy(alpha = 0.64f),
                        if (state.connected) WanLvMint.copy(alpha = 0.56f) else Color.White.copy(alpha = 0.72f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.78f), shape)
            .clickable(
                enabled = !state.connecting,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (state.connected) {
            // 重点：连接成功后在玻璃按钮内部显示呼吸光圈，明确反馈数字人会话处于在线状态。
            Box(
                modifier = Modifier
                    .size(31.dp)
                    .scale(haloScale)
                    .border(2.dp, WanLvGreen.copy(alpha = haloAlpha), CircleShape)
            )
        }
        if (state.connecting) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = WanLvGreen, strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = if (state.connected) "断开数字人" else "连接数字人",
                tint = if (state.connected) WanLvGreen else WanLvTextPrimary,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun FloatingDigitalHuman(
    state: ChatDigitalHumanUiState,
    previewBitmap: Bitmap,
    onDrag: (Offset) -> Unit,
    onShowPersonaPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayBitmap = state.videoFrame ?: previewBitmap
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnShowPersonaPicker by rememberUpdatedState(onShowPersonaPicker)
    Box(
        modifier = modifier
            .size(width = 104.dp, height = 150.dp)
            .pointerInput(Unit) {
                // 重点：移动超过防抖距离后锁定为拖动；保持不动满 3 秒才切换形象，两种手势不会同时触发。
                detectFloatingDigitalHumanGestures(
                    onDrag = currentOnDrag,
                    onShowPersonaPicker = currentOnShowPersonaPicker
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = displayBitmap.asImageBitmap(),
            contentDescription = "${state.persona.label}，长按拖动，保持三秒切换形象",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        if (state.speaking) {
            Text(
                "播报中",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WanLvGreen.copy(alpha = 0.76f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun DigitalHumanPersonaDialog(
    currentPersona: ChatDigitalHumanPersona,
    previewBitmap: (ChatDigitalHumanPersona) -> Bitmap,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onPersonaSelected: (ChatDigitalHumanPersona) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(24.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("选择数字人形象", color = WanLvTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "关闭形象选择",
                    tint = WanLvTextSecondary,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(4.dp)
                )
            }
            ChatDigitalHumanPersona.all.forEach { persona ->
                DigitalHumanPersonaOption(
                    persona = persona,
                    previewBitmap = previewBitmap(persona),
                    selected = persona == currentPersona,
                    enabled = enabled,
                    onClick = { onPersonaSelected(persona) }
                )
            }
        }
    }
}

@Composable
private fun DigitalHumanPersonaOption(
    persona: ChatDigitalHumanPersona,
    previewBitmap: Bitmap,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(shape)
            .background(if (selected) Color(0xFFF1F6FF) else Color.White)
            .border(1.dp, if (selected) Color(0xFF5D88FF) else Color(0xFFE2E7EC), shape)
            .clickable(
                enabled = enabled && !selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            bitmap = previewBitmap.asImageBitmap(),
            contentDescription = persona.label,
            modifier = Modifier
                .size(width = 56.dp, height = 66.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(WanLvMint.copy(alpha = 0.52f)),
            contentScale = ContentScale.Fit
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(persona.label, color = WanLvTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(persona.description, color = WanLvTextSecondary, fontSize = 12.sp)
        }
        if (selected) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(WanLvGreenLight.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = WanLvGreen, modifier = Modifier.size(13.dp))
                Text("当前", color = WanLvGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChatUsageTip() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF737985))
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Text(
            "连接数字人后可同步播报回复；不连接也能正常文字问答。\n按住数字人并移动可更换位置，保持不动满三秒可切换形象。",
            color = Color.White,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun ScenicContextMessage(scenicAreaName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE7EFF9)),
            contentAlignment = Alignment.Center
        ) {
            Text("SYS", color = Color(0xFF29435D), fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.82f))
                .border(1.dp, Color(0xFFDDE4EB), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("系统提示", color = WanLvTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("已带入景区上下文：$scenicAreaName", color = WanLvTextPrimary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.fromUser) {
            AiBadge()
        }
        val bubbleShape = RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 22.dp,
            bottomStart = if (message.fromUser) 22.dp else 7.dp,
            bottomEnd = if (message.fromUser) 7.dp else 22.dp
        )
        val bubbleGradient = if (message.fromUser) {
            Brush.linearGradient(
                listOf(
                    WanLvGreenLight.copy(alpha = 0.82f),
                    WanLvGreen.copy(alpha = 0.76f),
                    Color.White.copy(alpha = 0.18f)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.90f),
                    WanLvMint.copy(alpha = 0.54f),
                    WanLvSurface.copy(alpha = 0.72f),
                    Color.White.copy(alpha = 0.78f)
                )
            )
        }
        Box(
            modifier = Modifier
                .padding(start = if (message.fromUser) 58.dp else 8.dp, end = if (message.fromUser) 0.dp else 42.dp)
                .padding(bottom = 4.dp)
        ) {
            // 重点：先绘制半透明气泡尾巴，再让主体覆盖接缝，形成完整的液态玻璃气泡轮廓。
            Box(
                modifier = Modifier
                    .align(if (message.fromUser) Alignment.BottomEnd else Alignment.BottomStart)
                    .offset(
                        x = if (message.fromUser) (-2).dp else 2.dp,
                        y = 2.dp
                    )
                    .size(16.dp)
                    .rotate(45f)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(4.dp),
                        ambientColor = WanLvGreen.copy(alpha = 0.10f),
                        spotColor = Color(0xFF64727F).copy(alpha = 0.10f)
                    )
                    .background(bubbleGradient, RoundedCornerShape(4.dp))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = if (message.fromUser) 0.40f else 0.72f),
                        RoundedCornerShape(4.dp)
                    )
            )

            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (message.fromUser) 13.dp else 17.dp,
                        shape = bubbleShape,
                        ambientColor = Color(0xFF8FA0AE).copy(alpha = 0.18f),
                        spotColor = Color(0xFF64727F).copy(alpha = 0.12f)
                    )
                    .clip(bubbleShape)
                    .background(bubbleGradient)
                    .border(
                        1.dp,
                        Color.White.copy(alpha = if (message.fromUser) 0.46f else 0.82f),
                        bubbleShape
                    )
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            ) {
                Text(
                    text = message.content,
                    color = if (message.fromUser) Color.White else WanLvTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun AiBadge() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .shadow(10.dp, CircleShape, ambientColor = WanLvGreen.copy(alpha = 0.12f), spotColor = WanLvGreen.copy(alpha = 0.10f))
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.88f),
                        WanLvMint.copy(alpha = 0.78f),
                        Color.White.copy(alpha = 0.70f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.82f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("AI", color = WanLvGreen, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSend = value.trim().isNotEmpty()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.00f),
                        Color.White.copy(alpha = 0.78f),
                        WanLvSurface.copy(alpha = 0.92f)
                    )
                )
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            cornerRadius = 28.dp,
            padding = PaddingValues(horizontal = 7.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    placeholder = {
                        Text(
                            "有问题尽管问我",
                            color = WanLvTextSecondary.copy(alpha = 0.72f),
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 1
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                LiquidGlassIconButton(
                    icon = Icons.Rounded.Mic,
                    label = "语音输入",
                    size = 38.dp,
                    enabled = false,
                    onClick = {}
                )
                LiquidGlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.Send,
                    label = "发送",
                    size = 40.dp,
                    enabled = canSend,
                    accent = canSend,
                    onClick = onSend
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassIconButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    enabled: Boolean = true,
    accent: Boolean = false,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = Color(0xFF8FA0AE).copy(alpha = 0.20f),
                spotColor = Color(0xFF64727F).copy(alpha = 0.14f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    if (accent) {
                        listOf(
                            WanLvGreenLight.copy(alpha = 0.90f),
                            WanLvGreen.copy(alpha = 0.86f),
                            Color.White.copy(alpha = 0.28f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = if (enabled) 0.86f else 0.52f),
                            WanLvSurface.copy(alpha = if (enabled) 0.64f else 0.42f),
                            Color.White.copy(alpha = if (enabled) 0.72f else 0.48f)
                        )
                    }
                )
            )
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.78f else 0.58f), shape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size * 0.42f),
                color = if (accent) Color.White else WanLvGreen,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                icon,
                contentDescription = label,
                tint = when {
                    accent -> Color.White
                    enabled -> WanLvTextPrimary
                    else -> WanLvTextSecondary.copy(alpha = 0.50f)
                },
                modifier = Modifier.size(size * 0.44f)
            )
        }
    }
}

@Composable
private fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = Color(0xFF8FA0AE).copy(alpha = 0.18f),
                spotColor = Color(0xFF64727F).copy(alpha = 0.12f)
            )
            .clip(shape)
            // 重点：问答页按钮、输入框和面板统一使用半透明高光与白色描边，保持液态玻璃质感一致。
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.88f),
                        WanLvSurface.copy(alpha = 0.66f),
                        WanLvMint.copy(alpha = 0.32f),
                        Color.White.copy(alpha = 0.76f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.78f), shape)
            .padding(padding)
    ) {
        content()
    }
}
