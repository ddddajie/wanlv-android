package com.wanlv.app.ui.screens.chat

import android.graphics.Bitmap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun ChatScreen(
    bottomBarExpanded: Boolean = true,
    onRequestBottomBarExpand: () -> Unit = {},
    onRequestBottomBarCollapse: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val digitalHumanState = viewModel.digitalHumanState
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val inputBottomAvoidance by animateDpAsState(
        targetValue = if (bottomBarExpanded) FloatingBottomBarAvoidance else navigationBarInset + 8.dp,
        label = "chat-input-bottom-avoidance"
    )

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WanLvBackground)
            .statusBarsPadding()
            .imePadding()
    ) {
        ChatHeader(
            state = digitalHumanState,
            previewBitmap = viewModel.currentDigitalHumanPreviewBitmap,
            onToggleDigitalHuman = viewModel::toggleDigitalHumanConnection,
            onPersonaSelected = viewModel::switchDigitalHumanPersona
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.messages, key = { it.id }) { message -> ChatBubble(message) }
            item { QuickPromptCards(onPromptSelected = viewModel::updateInput) }
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
}

@Composable
private fun ChatHeader(
    state: ChatDigitalHumanUiState,
    previewBitmap: Bitmap,
    onToggleDigitalHuman: () -> Unit,
    onPersonaSelected: (ChatDigitalHumanPersona) -> Unit
) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text("万旅小助手", color = WanLvTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("景区服务智能助手", color = WanLvTextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        DigitalHumanControlPanel(
            state = state,
            previewBitmap = previewBitmap,
            onToggleDigitalHuman = onToggleDigitalHuman,
            onPersonaSelected = onPersonaSelected
        )
    }
}

@Composable
private fun DigitalHumanControlPanel(
    state: ChatDigitalHumanUiState,
    previewBitmap: Bitmap,
    onToggleDigitalHuman: () -> Unit,
    onPersonaSelected: (ChatDigitalHumanPersona) -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        padding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DigitalHumanPreview(state = state, previewBitmap = previewBitmap)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("数字人播报", color = WanLvTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    DigitalHumanStatusPill(state)
                }
                Text(
                    text = state.status,
                    color = WanLvTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChatDigitalHumanPersona.all.forEach { persona ->
                        PersonaChip(
                            persona = persona,
                            selected = state.persona == persona,
                            enabled = !state.connecting,
                            onClick = { onPersonaSelected(persona) }
                        )
                    }
                }
            }
            LiquidGlassIconButton(
                icon = if (state.connected || state.connecting) Icons.Rounded.Close else Icons.Rounded.AutoAwesome,
                label = if (state.connected || state.connecting) "断开数字人" else "连接数字人",
                size = 46.dp,
                accent = state.connected,
                loading = state.connecting,
                onClick = onToggleDigitalHuman
            )
        }
    }
}

@Composable
private fun DigitalHumanPreview(
    state: ChatDigitalHumanUiState,
    previewBitmap: Bitmap
) {
    val displayBitmap = state.videoFrame ?: previewBitmap
    Box(
        modifier = Modifier
            .size(width = 68.dp, height = 98.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.42f))
            .border(1.dp, Color.White.copy(alpha = 0.78f), RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = displayBitmap.asImageBitmap(),
            contentDescription = state.persona.label,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        if (state.speaking) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WanLvGreen.copy(alpha = 0.74f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text("播报中", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DigitalHumanStatusPill(state: ChatDigitalHumanUiState) {
    val text = when {
        state.connecting -> "连接中"
        state.connected -> "已连接"
        else -> "未连接"
    }
    val tint = if (state.connected || state.connecting) WanLvGreen else WanLvTextSecondary
    Text(
        text = text,
        color = tint,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.54f))
            .border(1.dp, Color.White.copy(alpha = 0.78f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun PersonaChip(
    persona: ChatDigitalHumanPersona,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (selected) 0.92f else 0.62f),
                        if (selected) WanLvGreenLight.copy(alpha = 0.18f) else WanLvSurface.copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.68f)
                    )
                )
            )
            .border(1.dp, if (selected) WanLvGreen.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.72f), shape)
            .clickable(
                enabled = enabled && !selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            persona.shortLabel,
            color = if (selected) WanLvGreen else WanLvTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
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
        Box(
            modifier = Modifier
                .padding(start = if (message.fromUser) 58.dp else 8.dp, end = if (message.fromUser) 0.dp else 42.dp)
                .shadow(
                    elevation = if (message.fromUser) 10.dp else 16.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color(0xFF64727F).copy(alpha = 0.12f),
                    spotColor = Color(0xFF64727F).copy(alpha = 0.10f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(
                    if (message.fromUser) {
                        Brush.linearGradient(
                            listOf(
                                WanLvGreenLight.copy(alpha = 0.92f),
                                WanLvGreen.copy(alpha = 0.92f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.94f),
                                WanLvSurface.copy(alpha = 0.72f),
                                Color.White.copy(alpha = 0.82f)
                            )
                        )
                    }
                )
                .border(1.dp, Color.White.copy(alpha = if (message.fromUser) 0.38f else 0.76f), RoundedCornerShape(22.dp))
                .padding(horizontal = 15.dp, vertical = 12.dp)
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
private fun QuickPromptCards(onPromptSelected: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        QuickPrompts.forEach { prompt ->
            QuickPromptButton(
                prompt = prompt,
                modifier = Modifier.weight(1f),
                onClick = { onPromptSelected(prompt.question) }
            )
        }
    }
}

@Composable
private fun QuickPromptButton(
    prompt: QuickPrompt,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        cornerRadius = 20.dp,
        padding = PaddingValues(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(prompt.title, color = WanLvTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black, lineHeight = 17.sp)
            if (prompt.subtitle.isNotBlank()) {
                Text(prompt.subtitle, color = WanLvTextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
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
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            cornerRadius = 32.dp,
            padding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidGlassIconButton(
                    icon = Icons.Rounded.Mic,
                    label = "语音输入",
                    size = 46.dp,
                    enabled = false,
                    onClick = {}
                )
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    singleLine = true,
                    placeholder = { Text("有问题尽管问我", color = WanLvTextSecondary.copy(alpha = 0.72f), fontSize = 14.sp) },
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
                    icon = Icons.AutoMirrored.Rounded.Send,
                    label = "发送",
                    size = 48.dp,
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

private data class QuickPrompt(
    val title: String,
    val subtitle: String,
    val question: String
)

private val QuickPrompts = listOf(
    QuickPrompt("最佳游览路线", "3-5 小时", "帮我规划一条3-5小时的最佳游览路线"),
    QuickPrompt("亲子友好", "适合带小朋友", "附近有哪些适合亲子游的项目？"),
    QuickPrompt("避开拥挤", "错峰游览建议", "现在怎么游览可以尽量避开拥挤？")
)
