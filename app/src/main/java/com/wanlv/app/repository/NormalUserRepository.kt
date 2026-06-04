package com.wanlv.app.repository

import com.wanlv.app.network.ApiClient
import com.wanlv.app.network.AuthSession
import com.wanlv.app.pojo.dto.NormalUserDto
import org.json.JSONObject

class NormalUserRepository {
    suspend fun login(username: String, password: String): NormalUserDto {
        val data = ApiClient.post(
            "/user/normal/login",
            JSONObject().put("username", username).put("password", password)
        ) as JSONObject
        val user = NormalUserDto.fromJson(data)
        user.token?.let { AuthSession.setLogin(user.id, it) }
        return user
    }

    suspend fun getCurrentUser(): NormalUserDto? {
        val userId = AuthSession.userId ?: return null
        val data = ApiClient.get("/user/normal/$userId") as JSONObject
        return NormalUserDto.fromJson(data)
    }
}
