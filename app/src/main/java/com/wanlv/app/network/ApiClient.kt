package com.wanlv.app.network

import com.wanlv.app.config.AppConfig
import java.net.SocketTimeoutException
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val DialogueTimeoutSeconds = 90L
    private const val RefreshTimeoutSeconds = 10L
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val refreshCoordinator = TokenRefreshCoordinator()
    private val client = OkHttpClient.Builder()
        // 重点：AI 对话可能需要较长推理时间，连接、写入和响应读取统一允许等待 90 秒。
        .connectTimeout(DialogueTimeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(DialogueTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(DialogueTimeoutSeconds, TimeUnit.SECONDS)
        .build()
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(RefreshTimeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(RefreshTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(RefreshTimeoutSeconds, TimeUnit.SECONDS)
        .build()

    suspend fun get(path: String, query: Map<String, Any?> = emptyMap()): Any? =
        request("GET", buildUrl(path, query), null)

    suspend fun post(path: String, body: JSONObject = JSONObject()): Any? =
        request("POST", buildUrl(path), body, attachAccessToken = true)

    suspend fun postWithoutAccessToken(path: String, body: JSONObject = JSONObject()): Any? =
        request("POST", buildUrl(path), body, attachAccessToken = false)

    suspend fun put(path: String, body: JSONObject = JSONObject()): Any? =
        request("PUT", buildUrl(path), body)

    private suspend fun request(
        method: String,
        url: String,
        body: JSONObject?,
        attachAccessToken: Boolean = true
    ): Any? = withContext(Dispatchers.IO) {
        val requestBody = body?.toString()?.toRequestBody(jsonMediaType)
        val requestAccessToken = AuthSession.token?.takeIf { it.isNotBlank() }
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .apply {
                requestAccessToken?.takeIf { attachAccessToken }?.let {
                    header("Authorization", "Bearer $it")
                }
                when (method) {
                    "GET" -> get()
                    "POST" -> post(requestBody ?: "{}".toRequestBody(jsonMediaType))
                    "PUT" -> put(requestBody ?: "{}".toRequestBody(jsonMediaType))
                    else -> method(method, requestBody)
                }
            }
            .build()

        try {
            val firstResponse = execute(request)
            if (!firstResponse.isUnauthorized()) {
                return@withContext firstResponse.unwrap()
            }

            if (!attachAccessToken) throw firstResponse.unauthorizedException()

            val retryToken = refreshCoordinator.accessTokenForRetry(
                requestAccessToken = requestAccessToken,
                latestAccessToken = { AuthSession.token },
                latestRefreshToken = { AuthSession.refreshToken },
                refresh = ::refreshAccessToken,
                onLoginExpired = AuthSession::expireLogin
            ) ?: throw ApiException("登录状态已过期，请重新登录", 401)

            val retryRequest = request.newBuilder()
                .header("Authorization", "Bearer $retryToken")
                .build()
            val retryResponse = execute(retryRequest)
            if (retryResponse.isUnauthorized()) {
                // 重点：同一请求刷新后仍为 401 时立即结束登录态，禁止再次刷新形成死循环。
                AuthSession.expireLogin()
                throw retryResponse.unauthorizedException()
            }
            retryResponse.unwrap()
        } catch (_: SocketTimeoutException) {
            throw ApiException("请求超时，请稍后重试")
        }
    }

    private fun execute(request: Request): RawResponse = client.newCall(request).execute().use {
        RawResponse(
            status = it.code,
            successful = it.isSuccessful,
            text = it.body?.string().orEmpty()
        )
    }

    private fun refreshAccessToken(refreshToken: String): String? = runCatching {
        val refreshRequest = Request.Builder()
            .url(buildUrl("/user/normal/token/refresh"))
            .header("Content-Type", "application/json")
            .post(JSONObject().put("refreshToken", refreshToken).toString().toRequestBody(jsonMediaType))
            .build()

        refreshClient.newCall(refreshRequest).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val envelope = response.body?.string().orEmpty()
                .takeIf { it.isNotBlank() }
                ?.let { JSONTokener(it).nextValue() as? JSONObject }
                ?: return@use null
            if (envelope.optInt("code") != 200) return@use null
            val data = envelope.optJSONObject("data") ?: return@use null
            val newAccessToken = data.optString("token").takeIf { it.isNotBlank() }
                ?: return@use null
            val newRefreshToken = data.optString("refreshToken").takeIf { it.isNotBlank() }
                ?: return@use null

            // 重点：后端刷新后会立即轮换 refreshToken，两个 Token 必须原子式同步更新。
            AuthSession.updateTokens(newAccessToken, newRefreshToken)
            newAccessToken
        }
    }.getOrNull()

    private fun buildUrl(path: String, query: Map<String, Any?> = emptyMap()): String {
        val base = AppConfig.apiBaseUrl.trimEnd('/')
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val queryText = query
            .filterValues { it != null }
            .map { (key, value) -> "${key.encode()}=${value.toString().encode()}" }
            .joinToString("&")
        return if (queryText.isBlank()) "$base$normalizedPath" else "$base$normalizedPath?$queryText"
    }

    private fun String.encode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

    private fun unwrapResponse(text: String): Any? {
        if (text.isBlank()) return null
        val parsed = JSONTokener(text).nextValue()
        if (parsed is JSONObject && parsed.has("code") && parsed.has("data")) {
            val code = parsed.optInt("code")
            if (code == 401) {
                AuthSession.expireLogin()
                throw ApiException("登录状态已过期，请重新登录", code)
            }
            if (code != 200) throw ApiException(parsed.optString("msg", "请求失败"), code)
            return when (val data = parsed.opt("data")) {
                JSONObject.NULL -> null
                else -> data
            }
        }
        return when (parsed) {
            is JSONObject, is JSONArray -> parsed
            else -> text
        }
    }

    private fun responseErrorMessage(text: String, status: Int): String {
        if (text.isBlank()) return "请求失败：$status"
        return runCatching {
            val json = JSONTokener(text).nextValue() as? JSONObject
            json?.optString("msg")?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: text
    }

    private data class RawResponse(
        val status: Int,
        val successful: Boolean,
        val text: String
    ) {
        fun isUnauthorized(): Boolean {
            if (status == 401) return true
            if (!successful || text.isBlank()) return false
            return runCatching {
                (JSONTokener(text).nextValue() as? JSONObject)?.optInt("code") == 401
            }.getOrDefault(false)
        }

        fun unwrap(): Any? {
            if (!successful) throw ApiException(responseErrorMessage(text, status), status)
            return unwrapResponse(text)
        }

        fun unauthorizedException(): ApiException = ApiException(
            responseErrorMessage(text, status).takeUnless { it == "请求失败：$status" }
                ?: "登录状态已过期，请重新登录",
            401
        )
    }
}
