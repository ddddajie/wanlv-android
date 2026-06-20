package com.wanlv.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanlv.app.pojo.dto.NormalUserDto
import com.wanlv.app.pojo.dto.NormalUserUpdateRequest
import com.wanlv.app.pojo.dto.ReservationOrderDto
import com.wanlv.app.ui.components.LiquidGlassCard
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvMint
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary
import org.json.JSONArray

@Composable
internal fun EditProfileGlassDialog(
    user: NormalUserDto?,
    loading: Boolean,
    message: String,
    messageIsError: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (NormalUserUpdateRequest) -> Unit
) {
    var password by remember(user?.id, user?.updateTime) { mutableStateOf("") }
    var nickname by remember(user?.id, user?.updateTime) { mutableStateOf(user?.nickname.orEmpty()) }
    var email by remember(user?.id, user?.updateTime) { mutableStateOf(user?.email.orEmpty()) }
    var interestTags by remember(user?.id, user?.updateTime) {
        mutableStateOf(interestTagsForDisplay(user?.interestTags))
    }

    ProfileDialogShell(
        title = "修改个人信息",
        subtitle = "更新后将同步到当前账号",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 470.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileDialogTextField(
                value = user?.phone.orEmpty(),
                onValueChange = {},
                label = "手机号（不可修改）",
                keyboardType = KeyboardType.Phone,
                enabled = false
            )
            ProfileDialogTextField(
                value = password,
                onValueChange = { password = it },
                label = "新密码（不修改请留空）",
                keyboardType = KeyboardType.Password,
                password = true
            )
            ProfileDialogTextField(nickname, { nickname = it }, "昵称")
            ProfileDialogTextField(email, { email = it }, "邮箱", KeyboardType.Email)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileDialogTextField(
                    value = genderText(user?.gender),
                    onValueChange = {},
                    label = "性别（不可修改）",
                    modifier = Modifier.weight(1f),
                    enabled = false
                )
                ProfileDialogTextField(
                    value = user?.age?.toString().orEmpty(),
                    onValueChange = {},
                    label = "年龄（不可修改）",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                    enabled = false
                )
            }
            ProfileDialogTextField(
                value = user?.avatarUrl.orEmpty(),
                onValueChange = {},
                label = "头像地址（暂不支持修改）",
                enabled = false
            )
            ProfileDialogTextField(
                value = interestTags,
                onValueChange = { interestTags = it },
                label = "兴趣标签，用中文逗号或英文逗号分隔",
                singleLine = false,
                minLines = 3
            )
        }
        ProfileDialogMessage(message, messageIsError)
        ProfilePrimaryButton(
            text = "保存修改",
            loading = loading,
            onClick = {
                onSubmit(
                    NormalUserUpdateRequest(
                        id = user?.id ?: 0L,
                        username = null,
                        password = password.takeIf { it.isNotBlank() },
                        nickname = nickname,
                        phone = null,
                        email = email,
                        avatarUrl = null,
                        gender = null,
                        age = null,
                        interestTags = interestTagsForRequest(interestTags)
                    )
                )
            }
        )
    }
}

@Composable
internal fun RealNameGlassDialog(
    user: NormalUserDto?,
    loading: Boolean,
    message: String,
    messageIsError: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (realName: String, idCardNo: String, clearSensitiveInput: () -> Unit) -> Unit
) {
    var realName by remember(user?.id) { mutableStateOf(user?.realName.orEmpty()) }
    // 重点：身份证号只保存在当前弹窗的临时状态中，关闭或提交成功后立即清空。
    var idCardNo by remember(user?.id) { mutableStateOf("") }
    val verified = user?.realNameStatus == 1

    ProfileDialogShell(
        title = if (verified) "实名认证" else "完成实名认证",
        subtitle = if (verified) "账号身份信息已通过认证" else "实名信息将用于预约本人身份核验",
        onDismiss = {
            idCardNo = ""
            onDismiss()
        }
    ) {
        if (verified) {
            VerifiedIdentityCard(user)
            ProfilePrimaryButton(text = "完成", loading = false, onClick = onDismiss)
        } else {
            ProfileDialogTextField(realName, { realName = it }, "真实姓名")
            ProfileDialogTextField(
                value = idCardNo,
                onValueChange = {
                    idCardNo = it.uppercase().filter { char -> char.isDigit() || char == 'X' }.take(18)
                },
                label = "18 位身份证号",
                keyboardType = KeyboardType.Ascii
            )
            Text(
                text = "身份证号仅用于本次认证提交，页面不会长期保存明文。",
                color = WanLvTextSecondary,
                fontSize = 11.sp
            )
            ProfileDialogMessage(message, messageIsError)
            ProfilePrimaryButton(
                text = if (user?.realNameStatus == 2) "重新提交认证" else "提交认证",
                loading = loading,
                onClick = {
                    onSubmit(realName, idCardNo) { idCardNo = "" }
                }
            )
        }
    }
}

@Composable
internal fun ReservationOrdersGlassDialog(
    orders: List<ReservationOrderDto>,
    pendingOnly: Boolean,
    loading: Boolean,
    message: String,
    messageIsError: Boolean,
    enteringReservationNo: String?,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onEnter: (String) -> Unit
) {
    val visibleOrders = if (pendingOnly) {
        orders.filter { it.status == "PENDING" || it.status == "CONFIRMED" }
    } else {
        orders
    }
    ProfileDialogShell(
        title = if (pendingOnly) "待使用预约" else "所有预约订单",
        subtitle = if (pendingOnly) "查看即将出发的预约" else "查看当前账号的全部预约记录",
        onDismiss = onDismiss
    ) {
        ProfileDialogMessage(message, messageIsError)
        if (message.isNotBlank()) Spacer(Modifier.height(8.dp))
        when {
            loading && orders.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WanLvGreen, strokeWidth = 2.dp)
            }
            messageIsError && orders.isEmpty() -> EmptyOrdersState(
                message = "暂时无法获取预约记录",
                onRefresh = onRefresh
            )
            visibleOrders.isEmpty() -> EmptyOrdersState(
                message = if (pendingOnly) "暂无待使用的预约" else "暂无预约记录",
                onRefresh = onRefresh
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(visibleOrders, key = { it.id }) { order ->
                    ReservationOrderGlassCard(
                        order = order,
                        allowEnter = pendingOnly,
                        entering = enteringReservationNo == order.reservationNo,
                        onEnter = { onEnter(order.reservationNo) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileDialogShell(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .imePadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        cornerRadius = 28.dp,
        padding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = WanLvTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = WanLvTextSecondary, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.54f))
                    .border(1.dp, Color.White.copy(alpha = 0.80f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = WanLvTextPrimary, modifier = Modifier.size(17.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun ProfileDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // 重点：标题放在输入框外部，避免 Material 浮动标签自动生成底纹色块。
        Text(
            text = label,
            color = if (enabled) WanLvTextPrimary else WanLvTextSecondary.copy(alpha = 0.68f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            shape = RoundedCornerShape(17.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.54f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.40f),
                focusedBorderColor = WanLvGreen.copy(alpha = 0.62f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.78f),
                disabledContainerColor = Color(0xFFF0F1F3).copy(alpha = 0.72f),
                disabledBorderColor = WanLvTextSecondary.copy(alpha = 0.16f),
                disabledTextColor = WanLvTextSecondary.copy(alpha = 0.58f)
            )
        )
    }
}

private fun genderText(gender: Int?): String = when (gender) {
    1 -> "男"
    2 -> "女"
    else -> "未设置"
}

private fun interestTagsForDisplay(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return runCatching {
        val array = JSONArray(value)
        List(array.length()) { index -> array.optString(index) }
            .filter { it.isNotBlank() }
            .joinToString("，")
    }.getOrDefault(value)
}

private fun interestTagsForRequest(value: String): String {
    val tags = value
        .split(',', '，')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
    return JSONArray(tags).toString()
}

@Composable
private fun ProfilePrimaryButton(text: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .height(48.dp),
        enabled = !loading,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = WanLvGreen)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileDialogMessage(message: String, isError: Boolean) {
    if (message.isNotBlank()) {
        Text(
            text = message,
            color = if (isError) Color(0xFFB84A4A) else WanLvGreen,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun VerifiedIdentityCard(user: NormalUserDto) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        padding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(WanLvGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Badge, contentDescription = null, tint = WanLvGreen)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(user.realName ?: "已认证用户", color = WanLvTextPrimary, fontWeight = FontWeight.Bold)
                Text(user.idCardMasked ?: "证件信息已保护", color = WanLvTextSecondary, fontSize = 12.sp)
                user.realNameTime?.let {
                    Text("认证时间 ${it.replace('T', ' ')}", color = WanLvTextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyOrdersState(message: String, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = WanLvTextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRefresh,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WanLvMint,
                contentColor = WanLvGreen
            )
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.size(6.dp))
            Text("刷新", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReservationOrderGlassCard(
    order: ReservationOrderDto,
    allowEnter: Boolean,
    entering: Boolean,
    onEnter: () -> Unit
) {
    val statusColor = reservationStatusColor(order.status)
    val canEnter = allowEnter && order.status == "CONFIRMED" && !entering
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        padding = PaddingValues(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = order.spotName ?: order.scenicName ?: "景区预约",
                    color = WanLvTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                order.scenicName?.let {
                    Text(it, color = WanLvTextSecondary, fontSize = 11.sp)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.10f))
                    .border(
                        1.dp,
                        statusColor.copy(
                            alpha = if (allowEnter && order.status == "CONFIRMED") 0.28f else 0.12f
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(
                        enabled = canEnter,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEnter
                    )
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                if (entering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        color = statusColor,
                        strokeWidth = 1.5.dp
                    )
                } else {
                    Text(
                        reservationStatusText(order.status),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "${order.visitDate}  ${order.startTime.take(5)}-${order.endTime.take(5)}",
            color = WanLvTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Text("预约编号 ${order.reservationNo}", color = WanLvTextSecondary, fontSize = 10.sp)
        Text("入园人数 ${order.visitorCount} 人", color = WanLvTextSecondary, fontSize = 11.sp)
        if (allowEnter && order.status == "CONFIRMED") {
            Text(
                text = "点击右上角“已预约”模拟检票入园",
                color = WanLvGreen,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
        if (order.visitors.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.42f))
                    .padding(horizontal = 11.dp, vertical = 8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    order.visitors.forEach { visitor ->
                        Text(
                            text = "${if (visitor.booker) "预约本人" else "同行人"} · ${visitor.realName} · ${visitor.idCardMasked}",
                            color = WanLvTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        order.cancelReason?.takeIf { it.isNotBlank() }?.let {
            Text("取消原因：$it", color = Color(0xFFB84A4A), fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

private fun reservationStatusText(status: String): String = when (status) {
    "PENDING" -> "待确认"
    "CONFIRMED" -> "已预约"
    "ENTERED" -> "已入园"
    "CANCELLED" -> "已取消"
    "COMPLETED" -> "已完成"
    "EXPIRED" -> "已过期"
    else -> status.ifBlank { "未知状态" }
}

private fun reservationStatusColor(status: String): Color = when (status) {
    "PENDING" -> Color(0xFFD58A22)
    "CONFIRMED" -> WanLvGreen
    "ENTERED" -> Color(0xFF3786A8)
    "CANCELLED", "EXPIRED" -> Color(0xFFB84A4A)
    else -> WanLvTextSecondary
}
