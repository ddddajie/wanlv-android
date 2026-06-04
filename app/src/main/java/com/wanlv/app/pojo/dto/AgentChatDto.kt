package com.wanlv.app.pojo.dto

import org.json.JSONObject

data class AgentChatDto(
    val answer: String,
    val scenicAreaId: Long?
) {
    companion object {
        fun fromJson(json: JSONObject) = AgentChatDto(
            answer = json.optString("answer", "暂时没有获取到回复"),
            scenicAreaId = if (json.has("scenicAreaId") && !json.isNull("scenicAreaId")) json.optLong("scenicAreaId") else null
        )
    }
}
