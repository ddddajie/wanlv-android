package com.wanlv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.wanlv.app.config.AppConfig
import com.wanlv.app.network.AuthSession
import com.wanlv.app.ui.theme.WanLvBackground
import com.wanlv.app.ui.theme.WanLvappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppConfig.init(applicationContext)
        AuthSession.initFromConfig()
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            WanLvappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = WanLvBackground
                ) {
                    WanLvApp()
                }
            }
        }
    }
}
