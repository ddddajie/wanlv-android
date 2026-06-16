package com.wanlv.app.ui.screens.developer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanlv.app.config.AppConfig
import com.wanlv.app.config.DeveloperConfigItem
import com.wanlv.app.ui.theme.WanLvBackground
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvGreenLight
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary

@Composable
fun DeveloperSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(AppConfig.developerConfigItems()) }
    var fieldValues by remember { mutableStateOf(items.associate { it.key to it.value }) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun reloadFields() {
        items = AppConfig.developerConfigItems()
        fieldValues = items.associate { it.key to it.value }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF1FBF8),
                        WanLvBackground,
                        Color(0xFFF8FAFC)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            DeveloperTopBar(onBack = onBack)
            Spacer(Modifier.height(16.dp))

            DeveloperIntroCard()
            Spacer(Modifier.height(14.dp))

            items.forEach { item ->
                DeveloperConfigField(
                    item = item,
                    value = fieldValues[item.key].orEmpty(),
                    onValueChange = { value ->
                        fieldValues = fieldValues.toMutableMap().apply { put(item.key, value) }
                        errorMessage = null
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color(0xFFE5484D),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        AppConfig.resetDeveloperConfig()
                        reloadFields()
                        errorMessage = null
                        Toast.makeText(context, "已恢复默认接口地址", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.size(7.dp))
                    Text("恢复默认")
                }

                Button(
                    onClick = {
                        val error = validateConfig(items, fieldValues)
                        if (error != null) {
                            errorMessage = error
                        } else {
                            AppConfig.saveDeveloperConfig(fieldValues)
                            reloadFields()
                            errorMessage = null
                            Toast.makeText(context, "接口地址已保存", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WanLvGreen)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.size(7.dp))
                    Text("保存")
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DeveloperTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.78f))
                .border(1.dp, Color.White.copy(alpha = 0.9f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = WanLvTextPrimary
            )
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                text = "开发者模式",
                color = WanLvTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "接口地址调试",
                color = WanLvTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DeveloperIntroCard() {
    LiquidDeveloperCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(WanLvGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = WanLvGreen,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "连续点击首页 7 次进入",
                    color = WanLvTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "保存后会覆盖本机调试配置，新请求会读取新的接口地址。",
                    color = WanLvTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun DeveloperConfigField(
    item: DeveloperConfigItem,
    value: String,
    onValueChange: (String) -> Unit
) {
    LiquidDeveloperCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = WanLvTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.required) {
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = "必填",
                            color = WanLvGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(WanLvGreen.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.description,
                    color = WanLvTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            placeholder = {
                Text(
                    text = item.defaultValue.ifBlank { "请输入 URL" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )

        if (item.defaultValue.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "默认：${item.defaultValue}",
                color = WanLvTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LiquidDeveloperCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = WanLvGreenLight.copy(alpha = 0.14f),
                spotColor = Color(0xFF8BA6A0).copy(alpha = 0.16f)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.78f),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.76f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.86f),
                            Color.White.copy(alpha = 0.62f)
                        )
                    )
                )
                .padding(16.dp),
            content = content
        )
    }
}

private fun validateConfig(
    items: List<DeveloperConfigItem>,
    values: Map<String, String>
): String? {
    items.forEach { item ->
        val value = values[item.key].orEmpty().trim()
        if (item.required && value.isBlank()) {
            return "${item.title}不能为空"
        }
        if (value.isNotBlank() && !value.startsWith("http://") && !value.startsWith("https://")) {
            return "${item.title}需要以 http:// 或 https:// 开头"
        }
    }
    return null
}
