package com.wanlv.app.network

import com.wanlv.app.config.AppConfig

object AuthSession {
    var token: String? = null
        private set
    var userId: Long? = null
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
