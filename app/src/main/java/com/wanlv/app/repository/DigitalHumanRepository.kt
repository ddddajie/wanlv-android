package com.wanlv.app.repository

import com.wanlv.app.network.ApiException
import com.wanlv.app.pojo.dto.DigitalHumanOfferDto
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONTokener

class DigitalHumanRepository {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun offer(baseUrl: String, sdp: String, type: String = "offer"): DigitalHumanOfferDto {
        val body = JSONObject()
            .put("sdp", sdp)
            .put("type", type)
        return DigitalHumanOfferDto.fromJson(post(baseUrl, "/offer", body))
    }

    suspend fun speak(baseUrl: String, text: String, sessionId: Long) {
        val body = JSONObject()
            .put("type", "echo")
            .put("text", text)
            .put("sessionid", sessionId)
        val response = post(baseUrl, "/human", body)
        if (response.optInt("code", -1) != 0) {
            throw ApiException(response.optString("msg", "数字人播报失败"), response.optInt("code"))
        }
    }

    suspend fun interrupt(baseUrl: String, sessionId: Long) {
        val body = JSONObject().put("sessionid", sessionId)
        post(baseUrl, "/interrupt_talk", body)
    }

    private suspend fun post(baseUrl: String, path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        if (normalizedBaseUrl.isBlank()) throw ApiException("数字人服务地址未配置")
        val request = Request.Builder()
            .url("$normalizedBaseUrl$path")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val status = response.code
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException(text.ifBlank { "数字人服务请求失败：$status" }, status)
            when (val parsed = JSONTokener(text.ifBlank { "{}" }).nextValue()) {
                is JSONObject -> parsed
                else -> throw ApiException("数字人服务返回格式异常")
            }
        }
    }
}

