package com.wanlv.app.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanlv.app.pojo.dto.ReservationSlotDto
import com.wanlv.app.pojo.dto.ReservationSpotDto
import com.wanlv.app.pojo.dto.ScenicAreaDto
import com.wanlv.app.repository.UserMapRepository
import com.wanlv.app.repository.UserReservationRepository
import java.time.LocalDate
import kotlinx.coroutines.launch

class BookingViewModel(
    private val mapRepository: UserMapRepository = UserMapRepository(),
    private val reservationRepository: UserReservationRepository = UserReservationRepository()
) : ViewModel() {
    var selectedDateIndex = mutableIntStateOf(1)
        private set
    var selectedTimeIndex = mutableIntStateOf(1)
        private set

    private val _ticketCounts = mutableStateMapOf(0 to 1, 1 to 0)
    val ticketCounts: Map<Int, Int> = _ticketCounts
    val scenicAreas = mutableStateListOf<ScenicAreaDto>()
    val reservationSpots = mutableStateListOf<ReservationSpotDto>()
    val reservationSlots = mutableStateListOf<ReservationSlotDto>()
    val selectedScenicArea = mutableStateOf<ScenicAreaDto?>(null)
    val selectedReservationSpot = mutableStateOf<ReservationSpotDto?>(null)
    val loadMessage = mutableStateOf("正在加载可预约景点...")

    fun selectDate(index: Int) {
        selectedDateIndex.intValue = index
        loadSlots()
    }

    fun selectTime(index: Int) {
        selectedTimeIndex.intValue = index
    }

    fun updateTicketCount(index: Int, count: Int) {
        _ticketCounts[index] = count.coerceAtLeast(0)
    }

    fun loadReservationData() {
        if (scenicAreas.isNotEmpty()) return
        viewModelScope.launch {
            runCatching { mapRepository.pageScenicAreas(pageSize = 50) }
                .onSuccess { page ->
                    scenicAreas.clear()
                    scenicAreas.addAll(page.records)
                    selectedScenicArea.value = page.records.firstOrNull()
                    loadEnabledSpots()
                }
                .onFailure { loadMessage.value = "景区接口暂不可用，当前显示本地预约样式" }
        }
    }

    fun selectReservationSpot(spot: ReservationSpotDto) {
        selectedReservationSpot.value = spot
        loadSlots()
    }

    private fun loadEnabledSpots() {
        val scenicAreaId = selectedScenicArea.value?.id ?: return
        viewModelScope.launch {
            runCatching { reservationRepository.listEnabledSpots(scenicAreaId) }
                .onSuccess { spots ->
                    reservationSpots.clear()
                    reservationSpots.addAll(spots)
                    selectedReservationSpot.value = spots.firstOrNull()
                    loadMessage.value = if (spots.isEmpty()) "该景区暂无可预约景点" else "已加载 ${spots.size} 个可预约景点"
                    loadSlots()
                }
                .onFailure { loadMessage.value = "可预约景点接口调用失败" }
        }
    }

    private fun loadSlots() {
        val spotId = selectedReservationSpot.value?.spotId ?: return
        val visitDate = LocalDate.now().plusDays(selectedDateIndex.intValue.toLong()).toString()
        viewModelScope.launch {
            runCatching { reservationRepository.listSlots(spotId, visitDate) }
                .onSuccess { slots ->
                    reservationSlots.clear()
                    reservationSlots.addAll(slots)
                    selectedTimeIndex.intValue = 0
                }
                .onFailure { loadMessage.value = "预约时段接口调用失败" }
        }
    }
}
