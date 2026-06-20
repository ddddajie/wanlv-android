package com.wanlv.app.network

internal class TokenRefreshCoordinator {
    private val refreshLock = Any()

    fun accessTokenForRetry(
        requestAccessToken: String?,
        latestAccessToken: () -> String?,
        latestRefreshToken: () -> String?,
        refresh: (String) -> String?,
        onLoginExpired: () -> Unit
    ): String? = synchronized(refreshLock) {
        val currentAccessToken = latestAccessToken()?.takeIf { it.isNotBlank() }
        if (currentAccessToken != null && currentAccessToken != requestAccessToken) {
            // 当前请求使用的是旧 Token，说明另一个请求已刷新完成，直接使用最新 Token 重放。
            return@synchronized currentAccessToken
        }

        val currentRefreshToken = latestRefreshToken()?.takeIf { it.isNotBlank() }
        if (currentRefreshToken == null) {
            onLoginExpired()
            return@synchronized null
        }

        // 重点：刷新和新 Token 落盘都在同一把锁内完成，并发 401 只会消费一次 refreshToken。
        refresh(currentRefreshToken) ?: run {
            onLoginExpired()
            null
        }
    }
}
