package com.wanlv.app.network

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wanlv.app.config.AppConfig

object AuthSession {
    var token by mutableStateOf<String?>(null)
        private set
    var userId by mutableStateOf<Long?>(null)
        private set

    fun initFromConfig() {
        token = AppConfig.debugToken
        userId = AppConfig.debugUserId
    }

    fun setLogin(userId: Long, token: String) {
        this.userId = userId
        this.token = token
    }

    fun clear() {
        userId = null
        token = null
    }
}
