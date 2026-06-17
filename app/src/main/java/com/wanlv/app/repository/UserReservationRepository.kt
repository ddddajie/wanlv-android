package com.wanlv.app.repository

import com.wanlv.app.network.ApiClient
import com.wanlv.app.pojo.dto.CreateReservationOrderRequest
import com.wanlv.app.pojo.dto.ReservationOrderResult
import com.wanlv.app.pojo.dto.ReservationSlotDto
import com.wanlv.app.pojo.dto.ReservationSpotDto
import org.json.JSONArray
import org.json.JSONObject

class UserReservationRepository {
    suspend fun listEnabledSpots(scenicAreaId: Long, keyword: String? = null): List<ReservationSpotDto> {
        val data = ApiClient.get(
            "/reservation/spots/enabled",
            mapOf("scenicAreaId" to scenicAreaId, "keyword" to keyword)
        ) as JSONArray
        return List(data.length()) { index -> ReservationSpotDto.fromJson(data.getJSONObject(index)) }
    }

    suspend fun listSlots(spotId: Long, visitDate: String): List<ReservationSlotDto> {
        val data = ApiClient.get(
            "/reservation/slots",
            mapOf("spotId" to spotId, "visitDate" to visitDate)
        )
        val slots = when (data) {
            is JSONObject -> data.optJSONArray("slots")
            is JSONArray -> data
            else -> null
        } ?: JSONArray()
        return List(slots.length()) { index -> ReservationSlotDto.fromJson(slots.getJSONObject(index)) }
    }

    suspend fun createOrder(request: CreateReservationOrderRequest): ReservationOrderResult {
        val data = ApiClient.post("/reservation/orders", request.toJson()) as JSONObject
        return ReservationOrderResult.fromJson(data)
    }
}
