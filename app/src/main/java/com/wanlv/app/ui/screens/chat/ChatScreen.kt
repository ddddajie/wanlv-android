package com.wanlv.app.ui.screens.chat

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wanlv.app.model.ChatMessage
import com.wanlv.app.ui.components.FloatingBottomBarAvoidance
import com.wanlv.app.ui.components.IOSCard
import com.wanlv.app.ui.theme.WanLvBackground
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvMint
import com.wanlv.app.ui.theme.WanLvSurface
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary
import com.wanlv.app.viewmodel.ChatViewModel

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WanLvBackground)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text("万旅小助手", color = WanLvTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("景区服务智能助手", color = WanLvTextSecondary, fontSize = 14.sp)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.messages, key = { it.id }) { message -> ChatBubble(message) }
            item { QuickPromptCards() }
        }
        ChatInputBar(
            value = viewModel.input.value,
            onValueChange = viewModel::updateInput,
            onSend = viewModel::sendMessage,
            modifier = Modifier.padding(bottom = FloatingBottomBarAvoidance)
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.fromUser) {
            Text(
                "AI",
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(WanLvMint)
                    .padding(top = 7.dp),
                color = WanLvGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = message.content,
            modifier = Modifier
                .padding(start = if (message.fromUser) 58.dp else 8.dp, end = if (message.fromUser) 0.dp else 42.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(if (message.fromUser) WanLvGreen else WanLvSurface)
                .padding(horizontal = 15.dp, vertical = 12.dp),
            color = if (message.fromUser) WanLvSurface else WanLvTextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun QuickPromptCards() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("最佳游览路线\n3-5小时", "亲子友好", "避开拥挤").forEach { text ->
            IOSCard(
                modifier = Modifier.weight(1f),
                padding = PaddingValues(12.dp),
                cornerRadius = 18.dp
            ) {
                Text(text, color = WanLvTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(WanLvSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(WanLvMint),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Rounded.Mic, contentDescription = "语音输入", tint = WanLvGreen)
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            singleLine = true,
            placeholder = { Text("有问题尽管问我", color = WanLvTextSecondary, fontSize = 14.sp) },
            shape = RoundedCornerShape(25.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = WanLvBackground,
                unfocusedContainerColor = WanLvBackground,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )
        Button(
            onClick = onSend,
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WanLvGreen)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Rounded.Send, contentDescription = "发送")
        }
    }
}
