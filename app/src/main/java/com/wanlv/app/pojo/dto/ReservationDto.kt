package com.wanlv.app.pojo.dto

import org.json.JSONArray
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

data class ReservationVisitorPayload(
    val realName: String,
    val idCardNo: String,
    val booker: Boolean
) {
    fun toJson(): JSONObject =
        JSONObject()
            .put("realName", realName)
            .put("idCardNo", idCardNo)
            .put("booker", booker)
}

data class CreateReservationOrderRequest(
    val userId: Long,
    val slotId: Long,
    val visitorCount: Int,
    val contactName: String?,
    val contactPhone: String?,
    val visitors: List<ReservationVisitorPayload>?,
    val sourceType: String = "FRONTEND",
    val clientRequestId: String,
    val remark: String?
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
            .put("userId", userId)
            .put("slotId", slotId)
            .put("visitorCount", visitorCount)
            .put("sourceType", sourceType)
            .put("clientRequestId", clientRequestId)

        contactName?.trim()?.takeIf { it.isNotEmpty() }?.let { json.put("contactName", it) }
        contactPhone?.trim()?.takeIf { it.isNotEmpty() }?.let { json.put("contactPhone", it) }
        remark?.trim()?.takeIf { it.isNotEmpty() }?.let { json.put("remark", it) }
        visitors?.takeIf { it.isNotEmpty() }?.let { items ->
            json.put("visitors", JSONArray(items.map { it.toJson() }))
        }
        return json
    }
}

data class ReservationOrderResult(
    val reservationNo: String
) {
    companion object {
        fun fromJson(json: JSONObject): ReservationOrderResult =
            ReservationOrderResult(reservationNo = json.optString("reservationNo"))
    }
}

data class ReservationVisitorDto(
    val realName: String,
    val idCardMasked: String,
    val booker: Boolean,
    val status: String
) {
    companion object {
        fun fromJson(json: JSONObject) = ReservationVisitorDto(
            realName = json.optString("realName"),
            idCardMasked = json.optString("idCardMasked"),
            booker = json.optBoolean("booker"),
            status = json.optString("status")
        )
    }
}

data class ReservationOrderDto(
    val id: Long,
    val reservationNo: String,
    val scenicName: String?,
    val spotName: String?,
    val visitDate: String,
    val startTime: String,
    val endTime: String,
    val visitorCount: Int,
    val visitors: List<ReservationVisitorDto>,
    val contactName: String?,
    val contactPhone: String?,
    val status: String,
    val remark: String?,
    val cancelReason: String?,
    val createTime: String?
) {
    companion object {
        fun fromJson(json: JSONObject): ReservationOrderDto {
            val visitorArray = json.optJSONArray("visitors") ?: JSONArray()
            return ReservationOrderDto(
                id = json.optLong("id"),
                reservationNo = json.optString("reservationNo"),
                scenicName = json.optNullableString("scenicName"),
                spotName = json.optNullableString("spotName"),
                visitDate = json.optString("visitDate"),
                startTime = json.optString("startTime"),
                endTime = json.optString("endTime"),
                visitorCount = json.optInt("visitorCount"),
                visitors = List(visitorArray.length()) { index ->
                    ReservationVisitorDto.fromJson(visitorArray.getJSONObject(index))
                },
                contactName = json.optNullableString("contactName"),
                contactPhone = json.optNullableString("contactPhone"),
                status = json.optString("status"),
                remark = json.optNullableString("remark"),
                cancelReason = json.optNullableString("cancelReason"),
                createTime = json.optNullableString("createTime")
            )
        }
    }
}
