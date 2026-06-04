package com.wanlv.app.repository

import com.wanlv.app.network.ApiClient
import com.wanlv.app.network.ApiException
import com.wanlv.app.network.AuthSession
import com.wanlv.app.pojo.dto.AgentChatDto
import org.json.JSONObject

class UserAgentRepository {
    suspend fun chat(content: String, scenicAreaId: Long? = null): AgentChatDto {
        val userId = AuthSession.userId ?: throw ApiException("请先登录后再使用智能问答")
        val body = JSONObject()
            .put("userId", userId)
            .put("content", content)
            .put("messageType", "text")
            .put("sourceType", if (scenicAreaId == null) "GLOBAL_CHAT" else "SCENIC_DETAIL")
            .put("sourceId", scenicAreaId?.toString())
        scenicAreaId?.let {
            body.put("scenicAreaId", it)
            body.put("scenicAreaSource", "FRONTEND")
        }
        return AgentChatDto.fromJson(ApiClient.post("/agent/chat", body) as JSONObject)
    }
}
