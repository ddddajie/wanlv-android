package com.wanlv.app.network

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenRefreshCoordinatorTest {
    @Test
    fun staleRequest_reusesTokenRefreshedByAnotherRequest() {
        val refreshCalls = AtomicInteger(0)
        val token = TokenRefreshCoordinator().accessTokenForRetry(
            requestAccessToken = "old-access",
            latestAccessToken = { "new-access" },
            latestRefreshToken = { "new-refresh" },
            refresh = {
                refreshCalls.incrementAndGet()
                null
            },
            onLoginExpired = {}
        )

        assertEquals("new-access", token)
        assertEquals(0, refreshCalls.get())
    }

    @Test
    fun concurrentUnauthorizedRequests_refreshOnlyOnce() {
        val coordinator = TokenRefreshCoordinator()
        val accessToken = AtomicReference("old-access")
        val refreshToken = AtomicReference("old-refresh")
        val refreshCalls = AtomicInteger(0)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        val tasks = List(2) {
            executor.submit<String?> {
                start.await()
                coordinator.accessTokenForRetry(
                    requestAccessToken = "old-access",
                    latestAccessToken = accessToken::get,
                    latestRefreshToken = refreshToken::get,
                    refresh = {
                        refreshCalls.incrementAndGet()
                        accessToken.set("new-access")
                        refreshToken.set("new-refresh")
                        "new-access"
                    },
                    onLoginExpired = {}
                )
            }
        }

        start.countDown()
        assertTrue(tasks.all { it.get(2, TimeUnit.SECONDS) == "new-access" })
        assertEquals(1, refreshCalls.get())
        executor.shutdownNow()
    }
}
