package com.wanlv.app.pojo.dto

import org.json.JSONObject

data class ReservationSpotDto(
    val spotId: Long,
    val scenicAreaId: Long,
    val scenicAreaName: String?,
    val spotName: String,
    val shortIntro: String?,
    val reservationNotice: String?,
    val advanceReservationDays: Int?,
    val minAdvanceMinutes: Int?
) {
    companion object {
        fun fromJson(json: JSONObject) = ReservationSpotDto(
            spotId = if (json.has("spotId") && !json.isNull("spotId")) json.optLong("spotId") else json.optLong("id"),
            scenicAreaId = json.optLong("scenicAreaId"),
            scenicAreaName = json.optNullableString("scenicAreaName"),
            spotName = json.optString("spotName", "未知景点"),
            shortIntro = json.optNullableString("shortIntro"),
            reservationNotice = json.optNullableString("reservationNotice"),
            advanceReservationDays = json.optNullableInt("advanceReservationDays"),
            minAdvanceMinutes = json.optNullableInt("minAdvanceMinutes")
        )
    }
}

data class ReservationSlotDto(
    val slotId: Long,
    val spotId: Long,
    val visitDate: String,
    val startTime: String,
    val endTime: String,
    val totalCapacity: Int,
    val remainingCount: Int,
    val available: Boolean
) {
    companion object {
        fun fromJson(json: JSONObject) = ReservationSlotDto(
            slotId = if (json.has("slotId") && !json.isNull("slotId")) json.optLong("slotId") else json.optLong("id"),
            spotId = json.optLong("spotId"),
            visitDate = json.optString("visitDate"),
            startTime = json.optString("startTime"),
            endTime = json.optString("endTime"),
            totalCapacity = json.optInt("totalCapacity"),
            remainingCount = json.optInt("remainingCount"),
            available = json.optBoolean("available", true)
        )
    }
}
