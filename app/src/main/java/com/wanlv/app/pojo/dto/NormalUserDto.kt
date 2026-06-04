package com.wanlv.app.pojo.dto

import org.json.JSONObject

data class NormalUserDto(
    val id: Long,
    val username: String,
    val displayName: String?,
    val nickname: String?,
    val realName: String?,
    val phone: String?,
    val avatarUrl: String?,
    val userType: String?,
    val realNameStatus: Int?,
    val token: String?
) {
    companion object {
        fun fromJson(json: JSONObject) = NormalUserDto(
            id = json.optLong("id"),
            username = json.optString("username"),
            displayName = json.optNullableString("displayName"),
            nickname = json.optNullableString("nickname"),
            realName = json.optNullableString("realName"),
            phone = json.optNullableString("phone"),
            avatarUrl = json.optNullableString("avatarUrl"),
            userType = json.optNullableString("userType"),
            realNameStatus = json.optNullableInt("realNameStatus"),
            token = json.optNullableString("token")
        )
    }
}
