package com.wanlv.app.pojo.dto

import org.json.JSONObject

data class DigitalHumanOfferDto(
    val sdp: String,
    val type: String,
    val sessionId: Long
) {
    companion object {
        fun fromJson(json: JSONObject) = DigitalHumanOfferDto(
            sdp = json.optString("sdp"),
            type = json.optString("type", "answer"),
            sessionId = json.optLong("sessionid")
        )
    }
}

