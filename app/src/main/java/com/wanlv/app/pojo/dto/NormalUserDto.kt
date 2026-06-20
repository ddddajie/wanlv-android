package com.wanlv.app.pojo.dto

import org.json.JSONObject

data class NormalUserDto(
    val id: Long,
    val username: String,
    val displayName: String?,
    val nickname: String?,
    val realName: String?,
    val phone: String?,
    val email: String?,
    val avatarUrl: String?,
    val gender: Int?,
    val age: Int?,
    val interestTags: String?,
    val userType: String?,
    val role: String?,
    val status: Int?,
    val realNameStatus: Int?,
    val idCardMasked: String?,
    val realNameTime: String?,
    val token: String?,
    val refreshToken: String?,
    val expireSeconds: Long?,
    val refreshExpireSeconds: Long?,
    val lastLoginTime: String?,
    val createTime: String?,
    val updateTime: String?
) {
    companion object {
        fun fromJson(json: JSONObject) = NormalUserDto(
            id = json.optLong("id"),
            username = json.optString("username"),
            displayName = json.optNullableString("displayName"),
            nickname = json.optNullableString("nickname"),
            realName = json.optNullableString("realName"),
            phone = json.optNullableString("phone"),
            email = json.optNullableString("email"),
            avatarUrl = json.optNullableString("avatarUrl"),
            gender = json.optNullableInt("gender"),
            age = json.optNullableInt("age"),
            interestTags = json.optNullableString("interestTags"),
            userType = json.optNullableString("userType"),
            role = json.optNullableString("role"),
            status = json.optNullableInt("status"),
            realNameStatus = json.optNullableInt("realNameStatus"),
            idCardMasked = json.optNullableString("idCardMasked"),
            realNameTime = json.optNullableString("realNameTime"),
            token = json.optNullableString("token"),
            refreshToken = json.optNullableString("refreshToken"),
            expireSeconds = json.optLong("expireSeconds").takeIf { json.has("expireSeconds") },
            refreshExpireSeconds = json.optLong("refreshExpireSeconds")
                .takeIf { json.has("refreshExpireSeconds") },
            lastLoginTime = json.optNullableString("lastLoginTime"),
            createTime = json.optNullableString("createTime"),
            updateTime = json.optNullableString("updateTime")
        )
    }
}

data class NormalUserUpdateRequest(
    val id: Long,
    val username: String?,
    val password: String?,
    val nickname: String?,
    val phone: String?,
    val email: String?,
    val avatarUrl: String?,
    val gender: Int?,
    val age: Int?,
    val interestTags: String?
) {
    fun toJson(): JSONObject = JSONObject().put("id", id).apply {
        username?.trim()?.takeIf { it.isNotEmpty() }?.let { put("username", it) }
        password?.takeIf { it.isNotBlank() }?.let { put("password", it) }
        nickname?.trim()?.takeIf { it.isNotEmpty() }?.let { put("nickname", it) }
        phone?.trim()?.takeIf { it.isNotEmpty() }?.let { put("phone", it) }
        email?.trim()?.takeIf { it.isNotEmpty() }?.let { put("email", it) }
        avatarUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { put("avatarUrl", it) }
        gender?.let { put("gender", it) }
        age?.let { put("age", it) }
        interestTags?.trim()?.takeIf { it.isNotEmpty() }?.let { put("interestTags", it) }
    }
}
