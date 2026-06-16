package com.wanlv.app.ui.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Tour
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wanlv.app.data.MockData
import com.wanlv.app.network.AuthSession
import com.wanlv.app.ui.components.FloatingBottomBarAvoidance
import com.wanlv.app.ui.components.IOSCard
import com.wanlv.app.ui.components.SectionHeader
import com.wanlv.app.ui.theme.WanLvBackground
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvMint
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary
import com.wanlv.app.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    LaunchedEffect(Unit) { viewModel.loadCurrentUser() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WanLvBackground)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("我的", color = WanLvTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (AuthSession.userId == null && viewModel.currentUser.value == null) {
            LoginCard(viewModel)
        } else {
            UserCard(viewModel)
        }
        StatsCard()
        SectionHeader("常用功能", showMore = false)
        FunctionGrid(
            listOf(
                ProfileAction(Icons.Rounded.FavoriteBorder, "我的收藏"),
                ProfileAction(Icons.Rounded.History, "浏览历史"),
                ProfileAction(Icons.Rounded.RateReview, "我的评价"),
                ProfileAction(Icons.Rounded.NotificationsNone, "消息通知")
            )
        )
        SectionHeader("我的订单", showMore = true)
        FunctionGrid(
            listOf(
                ProfileAction(Icons.Rounded.Payments, "待支付"),
                ProfileAction(Icons.Rounded.Wallet, "待使用"),
                ProfileAction(Icons.Rounded.TaskAlt, "已完成"),
                ProfileAction(Icons.Rounded.Tour, "已取消")
            )
        )
        Spacer(Modifier.height(FloatingBottomBarAvoidance))
    }
}

@Composable
private fun LoginCard(viewModel: ProfileViewModel) {
    IOSCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("普通用户登录", color = WanLvTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(viewModel.message.value, color = WanLvTextSecondary, fontSize = 13.sp)
            TextField(
                value = viewModel.username.value,
                onValueChange = viewModel::updateUsername,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("账号 / 用户名") },
                shape = RoundedCornerShape(18.dp),
                colors = loginFieldColors()
            )
            TextField(
                value = viewModel.password.value,
                onValueChange = viewModel::updatePassword,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                placeholder = { Text("密码") },
                shape = RoundedCornerShape(18.dp),
                colors = loginFieldColors()
            )
            Button(
                onClick = viewModel::login,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !viewModel.isLoading.value,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WanLvGreen)
            ) {
                Text(if (viewModel.isLoading.value) "登录中..." else "登录", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun loginFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = WanLvBackground,
    unfocusedContainerColor = WanLvBackground,
    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
)

@Composable
private fun UserCard(viewModel: ProfileViewModel) {
    val remoteUser = viewModel.currentUser.value
    val user = MockData.userProfile
    val nickname = remoteUser?.displayName ?: remoteUser?.nickname ?: remoteUser?.username ?: user.nickname
    val level = remoteUser?.phone ?: user.level
    val verified = (remoteUser?.realNameStatus ?: 0) == 1 || user.verified
    IOSCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 26.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(WanLvMint),
                contentAlignment = Alignment.Center
            ) {
                Text("游", color = WanLvGreen, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                .padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(nickname, color = WanLvTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(level, color = WanLvTextSecondary, fontSize = 13.sp)
                Text(viewModel.message.value, color = WanLvTextSecondary, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (verified) "已实名" else "未实名", color = WanLvGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = viewModel::logout) {
                    Text("退出", color = WanLvTextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StatsCard() {
    IOSCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, padding = PaddingValues(vertical = 16.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
            listOf("3\n我的收藏", "2\n我的预约", "1\n我的路线", "120\n积分").forEach { item ->
                val parts = item.split("\n")
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(parts[0], color = WanLvTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(parts[1], color = WanLvTextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FunctionGrid(items: List<ProfileAction>) {
    IOSCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, padding = PaddingValues(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                    row.forEach { item ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(WanLvMint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = item.icon, contentDescription = item.title, tint = WanLvGreen)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(item.title, color = WanLvTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

private data class ProfileAction(
    val icon: ImageVector,
    val title: String
)
