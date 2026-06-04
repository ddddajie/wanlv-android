package com.wanlv.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.wanlv.app.navigation.WanLvNavGraph
import com.wanlv.app.ui.theme.WanLvGreen
import kotlinx.coroutines.delay

@Composable
fun WanLvApp() {
    var showSplash by remember { mutableStateOf(true) }

    // 启动加载页使用用户新增的 bg 图片，先承接应用打开时的视觉过渡。
    LaunchedEffect(Unit) {
        delay(1400)
        showSplash = false
    }

    if (showSplash) {
        WanLvSplashScreen()
        return
    }

    val navController = rememberNavController()
    WanLvNavGraph(navController = navController)
}

@Composable
private fun WanLvSplashScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        LinearProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 34.dp)
                .width(180.dp),
            color = WanLvGreen
        )
    }
}
