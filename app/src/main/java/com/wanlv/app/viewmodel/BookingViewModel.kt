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
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class BookingDateOption(
    val label: String,
    val dateText: String,
    val value: LocalDate
)

data class ReservationQuotaSummary(
    val totalCapacity: Int,
    val remainingCount: Int,
    val availableSlotCount: Int,
    val loading: Boolean = false,
    val failed: Boolean = false
)

class BookingViewModel(
    private val mapRepository: UserMapRepository = UserMapRepository(),
    private val reservationRepository: UserReservationRepository = UserReservationRepository()
) : ViewModel() {
    val dateOptions: List<BookingDateOption> = buildBookingDateOptions()

    var selectedDateIndex = mutableIntStateOf(0)
        private set
    var selectedTimeIndex = mutableIntStateOf(-1)
        private set

    private val _ticketCounts = mutableStateMapOf(0 to 1, 1 to 0)
    val ticketCounts: Map<Int, Int> = _ticketCounts
    val scenicAreas = mutableStateListOf<ScenicAreaDto>()
    val reservationSpots = mutableStateListOf<ReservationSpotDto>()
    val reservationSlots = mutableStateListOf<ReservationSlotDto>()
    val reservationQuotaSummaries = mutableStateMapOf<Long, ReservationQuotaSummary>()
    val selectedScenicArea = mutableStateOf<ScenicAreaDto?>(null)
    val selectedReservationSpot = mutableStateOf<ReservationSpotDto?>(null)
    val loadMessage = mutableStateOf("正在加载可预约景点...")
    val slotsLoading = mutableStateOf(false)
    val slotMessage = mutableStateOf("")

    val selectedSlot: ReservationSlotDto?
        get() = reservationSlots.getOrNull(selectedTimeIndex.intValue)

    private var quotaLoadVersion = 0
    private var slotLoadVersion = 0

    fun selectDate(index: Int) {
        if (index !in dateOptions.indices) return
        selectedDateIndex.intValue = index
        loadSpotQuotas()
        selectedReservationSpot.value?.let { loadSlots(it) }
    }

    fun selectTime(index: Int) {
        val slot = reservationSlots.getOrNull(index) ?: return
        if (!slot.canReserve) return
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
                    if (page.records.isEmpty()) {
                        loadMessage.value = "暂无可切换景区"
                        return@onSuccess
                    }
                    loadEnabledSpots()
                }
                .onFailure { loadMessage.value = "景区接口暂不可用，请稍后重试" }
        }
    }

    fun selectScenicArea(area: ScenicAreaDto) {
        if (selectedScenicArea.value?.id == area.id) return
        selectedScenicArea.value = area
        selectedReservationSpot.value = null
        reservationSpots.clear()
        reservationSlots.clear()
        reservationQuotaSummaries.clear()
        slotsLoading.value = false
        slotMessage.value = ""
        slotLoadVersion++
        selectedTimeIndex.intValue = -1
        loadMessage.value = "正在加载 ${area.scenicName} 可预约景点..."
        loadEnabledSpots()
    }

    fun selectReservationSpot(spot: ReservationSpotDto) {
        selectedReservationSpot.value = spot
        loadSlots(spot)
    }

    fun currentVisitDate(): LocalDate =
        dateOptions.getOrElse(selectedDateIndex.intValue) { dateOptions.first() }.value

    fun selectedDateOption(): BookingDateOption =
        dateOptions.getOrElse(selectedDateIndex.intValue) { dateOptions.first() }

    fun formatVisitDateForApi(): String = currentVisitDate().toString()

    fun selectedSpotQuota(): ReservationQuotaSummary? =
        selectedReservationSpot.value?.let { reservationQuotaSummaries[it.spotId] }

    fun reserveButtonHint(): String = when {
        slotsLoading.value -> "时段加载中"
        selectedReservationSpot.value == null -> "请选择景点"
        selectedSlot == null -> "请选择时段"
        else -> "确认预约"
    }

    private fun loadEnabledSpots() {
        val scenicAreaId = selectedScenicArea.value?.id ?: return
        viewModelScope.launch {
            runCatching { reservationRepository.listEnabledSpots(scenicAreaId) }
                .onSuccess { spots ->
                    reservationSpots.clear()
                    reservationSpots.addAll(spots)
                    selectedReservationSpot.value = null
                    reservationSlots.clear()
                    slotsLoading.value = false
                    slotMessage.value = ""
                    selectedTimeIndex.intValue = -1
                    loadMessage.value = if (spots.isEmpty()) "该景区暂无可预约景点" else "已加载 ${spots.size} 个可预约景点"
                    loadSpotQuotas()
                }
                .onFailure { loadMessage.value = "可预约景点接口调用失败" }
        }
    }

    private fun loadSlots(spot: ReservationSpotDto) {
        val visitDate = formatVisitDateForApi()
        val requestVersion = ++slotLoadVersion
        reservationSlots.clear()
        slotsLoading.value = true
        slotMessage.value = "正在加载预约时段..."
        selectedTimeIndex.intValue = -1
        viewModelScope.launch {
            runCatching { reservationRepository.listSlots(spot.spotId, visitDate) }
                .onSuccess { slots ->
                    if (requestVersion != slotLoadVersion || selectedReservationSpot.value?.spotId != spot.spotId) return@onSuccess
                    slotsLoading.value = false
                    slotMessage.value = if (slots.isEmpty()) "当前日期暂无开放时段" else ""
                    reservationSlots.clear()
                    reservationSlots.addAll(slots)
                    selectedTimeIndex.intValue = slots.indexOfFirst { it.canReserve }
                }
                .onFailure {
                    if (requestVersion != slotLoadVersion) return@onFailure
                    slotsLoading.value = false
                    slotMessage.value = "预约时段接口调用失败"
                    loadMessage.value = "预约时段接口调用失败"
                }
        }
    }

    private fun loadSpotQuotas() {
        val spots = reservationSpots.toList()
        val visitDate = formatVisitDateForApi()
        val requestVersion = ++quotaLoadVersion
        reservationQuotaSummaries.clear()

        if (spots.isEmpty()) return
        spots.forEach { spot ->
            reservationQuotaSummaries[spot.spotId] = ReservationQuotaSummary(
                totalCapacity = 0,
                remainingCount = 0,
                availableSlotCount = 0,
                loading = true
            )
        }

        viewModelScope.launch {
            // 重点：主页面的景点卡片只展示名称和名额，名额统一由已存在的时段接口汇总，避免猜测新的后端字段。
            val summaries = coroutineScope {
                spots.map { spot ->
                    async {
                        val result = runCatching { reservationRepository.listSlots(spot.spotId, visitDate) }
                        spot.spotId to result.fold(
                            onSuccess = { slots -> slots.toQuotaSummary() },
                            onFailure = {
                                ReservationQuotaSummary(
                                    totalCapacity = 0,
                                    remainingCount = 0,
                                    availableSlotCount = 0,
                                    failed = true
                                )
                            }
                        )
                    }
                }.awaitAll()
            }
            if (requestVersion != quotaLoadVersion) return@launch
            summaries.forEach { (spotId, summary) ->
                reservationQuotaSummaries[spotId] = summary
            }
        }
    }
}

val ReservationSlotDto.canReserve: Boolean
    get() = available && remainingCount > 0

private fun List<ReservationSlotDto>.toQuotaSummary(): ReservationQuotaSummary =
    ReservationQuotaSummary(
        totalCapacity = sumOf { it.totalCapacity },
        remainingCount = sumOf { it.remainingCount },
        availableSlotCount = count { it.canReserve }
    )

private fun buildBookingDateOptions(): List<BookingDateOption> {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("MM.dd")
    return List(7) { offset ->
        val date = today.plusDays(offset.toLong())
        BookingDateOption(
            label = when (offset) {
                0 -> "今天"
                1 -> "明天"
                else -> weekLabel(date)
            },
            dateText = date.format(formatter),
            value = date
        )
    }
}

private fun weekLabel(date: LocalDate): String =
    when (date.dayOfWeek.value) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        else -> "周日"
    }
