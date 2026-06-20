package com.wanlv.app.repository

import com.wanlv.app.network.ApiClient
import com.wanlv.app.network.ApiException
import com.wanlv.app.network.AuthSession
import com.wanlv.app.pojo.dto.NormalUserDto
import com.wanlv.app.pojo.dto.NormalUserUpdateRequest
import com.wanlv.app.pojo.dto.PhoneCodeResult
import org.json.JSONObject

class NormalUserRepository {
    suspend fun login(username: String, password: String): NormalUserDto {
        val data = ApiClient.postWithoutAccessToken(
            "/user/normal/login",
            JSONObject().put("username", username).put("password", password)
        ) as JSONObject
        return saveLogin(data)
    }

    suspend fun sendPhoneCode(phone: String): PhoneCodeResult {
        val data = ApiClient.postWithoutAccessToken(
            "/user/normal/code/send",
            JSONObject().put("phone", phone)
        ) as? JSONObject ?: JSONObject()
        return PhoneCodeResult(
            code = data.optString("code").takeIf { it.isNotBlank() },
            expireSeconds = data.optInt("expireSeconds", DEFAULT_CODE_EXPIRE_SECONDS)
        )
    }

    suspend fun loginWithPhoneCode(phone: String, code: String): NormalUserDto {
        val data = ApiClient.postWithoutAccessToken(
            "/user/normal/code/login",
            JSONObject().put("phone", phone).put("code", code)
        ) as JSONObject
        return saveLogin(data)
    }

    suspend fun getCurrentUser(): NormalUserDto? {
        val userId = AuthSession.userId ?: return null
        val data = ApiClient.get("/user/normal/$userId") as JSONObject
        AuthSession.updateUser(data)
        return NormalUserDto.fromJson(data)
    }

    suspend fun updateUser(request: NormalUserUpdateRequest): NormalUserDto {
        val data = ApiClient.put("/user/normal/update", request.toJson()) as JSONObject
        val mergedUserData = mergeWithCachedUser(data)
        AuthSession.updateUser(mergedUserData)
        // 重点：更新响应可能不含实名字段，成功后重新拉取完整资料，避免实名状态被部分响应覆盖。
        return runCatching { getCurrentUser() }.getOrNull()
            ?: NormalUserDto.fromJson(mergedUserData)
    }

    suspend fun verifyRealName(realName: String, idCardNo: String): NormalUserDto {
        val userId = AuthSession.userId ?: throw ApiException("请先登录普通用户账号")
        val data = ApiClient.post(
            "/user/normal/real-name/verify",
            JSONObject()
                .put("userId", userId)
                .put("realName", realName)
                .put("idCardNo", idCardNo)
        ) as JSONObject
        val mergedUserData = mergeWithCachedUser(data)
        AuthSession.updateUser(mergedUserData)
        return runCatching { getCurrentUser() }.getOrNull()
            ?: NormalUserDto.fromJson(mergedUserData)
    }

    suspend fun logout() {
        val refreshToken = AuthSession.refreshToken
        try {
            if (!refreshToken.isNullOrBlank()) {
                ApiClient.postWithoutAccessToken(
                    "/user/normal/logout",
                    JSONObject().put("refreshToken", refreshToken)
                )
            }
        } finally {
            // 退出接口幂等；无论网络结果如何，本机都必须立即丢弃 accessToken 和 refreshToken。
            AuthSession.clear()
        }
    }

    private fun saveLogin(data: JSONObject): NormalUserDto {
        val user = NormalUserDto.fromJson(data)
        val token = user.token?.takeIf { it.isNotBlank() }
            ?: throw ApiException("登录响应缺少 token")
        val refreshToken = user.refreshToken?.takeIf { it.isNotBlank() }
            ?: throw ApiException("登录响应缺少 refreshToken")
        if (user.id <= 0L) throw ApiException("登录响应缺少用户 ID")
        // 重点：两种登录方式统一保存登录凭证与用户资料，后续鉴权逻辑完全一致。
        AuthSession.setLogin(user.id, token, refreshToken, data)
        return user
    }

    private fun mergeWithCachedUser(update: JSONObject): JSONObject {
        val merged = AuthSession.cachedUserData()?.let { JSONObject(it.toString()) } ?: JSONObject()
        update.keys().forEach { key -> merged.put(key, update.opt(key)) }
        return merged
    }

    private companion object {
        const val DEFAULT_CODE_EXPIRE_SECONDS = 300
    }
}
