package com.wanlv.app.repository

import com.wanlv.app.network.ApiClient
import com.wanlv.app.network.ApiException
import com.wanlv.app.network.AuthSession
import com.wanlv.app.pojo.dto.CreateReservationOrderRequest
import com.wanlv.app.pojo.dto.ReservationOrderResult
import com.wanlv.app.pojo.dto.ReservationOrderDto
import com.wanlv.app.pojo.dto.ReservationSlotDto
import com.wanlv.app.pojo.dto.ReservationSpotDto
import org.json.JSONArray
import org.json.JSONObject

class UserReservationRepository {
    suspend fun listMyOrders(): List<ReservationOrderDto> {
        val userId = AuthSession.userId
            ?: throw ApiException("请先登录普通用户账号")
        // 重点：当前后端的“我的预约”接口仍要求显式传入 userId 查询参数。
        val data = ApiClient.get(
            "/reservation/orders/my",
            mapOf("userId" to userId)
        )
        val orders = when (data) {
            is JSONArray -> data
            is JSONObject -> data.optJSONArray("records")
                ?: data.optJSONArray("list")
                ?: data.optJSONArray("orders")
            else -> null
        } ?: JSONArray()
        return List(orders.length()) { index ->
            ReservationOrderDto.fromJson(orders.getJSONObject(index))
        }
    }

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

    suspend fun enterOrder(reservationNo: String) {
        ApiClient.post("/reservation/admin/orders/$reservationNo/enter")
    }
}
