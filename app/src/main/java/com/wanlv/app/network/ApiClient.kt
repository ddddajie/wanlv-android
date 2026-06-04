package com.wanlv.app.network

import com.wanlv.app.config.AppConfig
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
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun get(path: String, query: Map<String, Any?> = emptyMap()): Any? =
        request("GET", buildUrl(path, query), null)

    suspend fun post(path: String, body: JSONObject = JSONObject()): Any? =
        request("POST", buildUrl(path), body)

    suspend fun put(path: String, body: JSONObject = JSONObject()): Any? =
        request("PUT", buildUrl(path), body)

    private suspend fun request(method: String, url: String, body: JSONObject?): Any? = withContext(Dispatchers.IO) {
        val requestBody = body?.toString()?.toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .apply {
                AuthSession.token?.takeIf { it.isNotBlank() }?.let {
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

        client.newCall(request).execute().use { response ->
            val status = response.code
            val text = response.body?.string().orEmpty()
            if (status == 401) {
                AuthSession.clear()
                throw ApiException("登录已过期，请重新登录", status)
            }
            if (!response.isSuccessful) throw ApiException(text.ifBlank { "请求失败：$status" }, status)
            unwrapResponse(text)
        }
    }

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
                AuthSession.clear()
                throw ApiException("登录已过期，请重新登录", code)
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
}
