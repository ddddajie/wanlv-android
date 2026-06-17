package com.wanlv.app.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanlv.app.network.AuthSession
import com.wanlv.app.pojo.dto.CreateReservationOrderRequest
import com.wanlv.app.pojo.dto.NormalUserDto
import com.wanlv.app.pojo.dto.ReservationVisitorPayload
import com.wanlv.app.pojo.dto.ReservationSlotDto
import com.wanlv.app.pojo.dto.ReservationSpotDto
import com.wanlv.app.pojo.dto.ScenicAreaDto
import com.wanlv.app.repository.NormalUserRepository
import com.wanlv.app.repository.UserMapRepository
import com.wanlv.app.repository.UserReservationRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
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

data class ReservationVisitorInput(
    val realName: String,
    val idCardNo: String
)

class BookingViewModel(
    private val mapRepository: UserMapRepository = UserMapRepository(),
    private val userRepository: NormalUserRepository = NormalUserRepository(),
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
    val currentUser = mutableStateOf<NormalUserDto?>(null)
    val loadMessage = mutableStateOf("正在加载可预约景点...")
    val slotsLoading = mutableStateOf(false)
    val slotMessage = mutableStateOf("")
    val currentUserLoading = mutableStateOf(false)
    val submitting = mutableStateOf(false)
    val submitMessage = mutableStateOf("")

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
        loadCurrentUser()
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

    fun loadCurrentUser() {
        val userId = AuthSession.userId
        if (userId == null) {
            currentUser.value = null
            currentUserLoading.value = false
            return
        }
        if (currentUserLoading.value || currentUser.value?.id == userId) return
        currentUserLoading.value = true
        viewModelScope.launch {
            runCatching { userRepository.getCurrentUser() }
                .onSuccess { user ->
                    currentUser.value = user
                    currentUserLoading.value = false
                }
                .onFailure {
                    currentUser.value = null
                    currentUserLoading.value = false
                    submitMessage.value = "获取实名信息失败，请稍后重试"
                }
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
        submitting.value -> "提交中..."
        slotsLoading.value -> "时段加载中"
        selectedReservationSpot.value == null -> "请选择景点"
        selectedSlot == null -> "请选择时段"
        else -> "确认预约"
    }

    fun submitReservation(
        visitorCount: Int,
        contactName: String,
        contactPhone: String,
        remark: String,
        visitors: List<ReservationVisitorInput>,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (submitting.value) return
        val buildResult = buildReservationOrderRequest(
            visitorCount = visitorCount,
            contactName = contactName,
            contactPhone = contactPhone,
            remark = remark,
            visitors = visitors
        )
        val request = buildResult.request
        if (request == null) {
            submitMessage.value = buildResult.error.orEmpty()
            onFailure(buildResult.error.orEmpty())
            return
        }

        submitting.value = true
        submitMessage.value = "正在提交预约..."
        viewModelScope.launch {
            runCatching { reservationRepository.createOrder(request) }
                .onSuccess { result ->
                    submitting.value = false
                    val reservationNo = result.reservationNo.ifBlank { "预约成功" }
                    submitMessage.value = "预约成功：$reservationNo"
                    selectedReservationSpot.value?.let { loadSlots(it) }
                    loadSpotQuotas()
                    onSuccess(reservationNo)
                }
                .onFailure { error ->
                    submitting.value = false
                    val message = error.message ?: "预约提交失败"
                    submitMessage.value = message
                    onFailure(message)
                }
        }
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

    private fun buildReservationOrderRequest(
        visitorCount: Int,
        contactName: String,
        contactPhone: String,
        remark: String,
        visitors: List<ReservationVisitorInput>
    ): ReservationOrderBuildResult {
        val userId = AuthSession.userId
            ?: return ReservationOrderBuildResult(error = "请先登录普通用户账号")
        val user = currentUser.value
            ?: return ReservationOrderBuildResult(error = "正在获取实名信息，请稍后重试")
        if (user.realNameStatus != 1) {
            return ReservationOrderBuildResult(error = "请先完成实名认证后再预约")
        }
        val slot = selectedSlot
            ?: return ReservationOrderBuildResult(error = "请选择可预约时段")
        if (!slot.canReserve) {
            return ReservationOrderBuildResult(error = "该时段暂不可预约")
        }
        if (visitorCount !in 1..5) {
            return ReservationOrderBuildResult(error = "入园人数需为 1-5 人")
        }
        if (visitorCount > slot.remainingCount) {
            return ReservationOrderBuildResult(error = "入园人数不能超过当前时段剩余名额")
        }
        if (visitors.size < visitorCount) {
            return ReservationOrderBuildResult(error = "请补全入园人信息")
        }

        val normalizedVisitors = visitors.take(visitorCount).mapIndexed { index, visitor ->
            ReservationVisitorPayload(
                realName = visitor.realName.trim(),
                idCardNo = visitor.idCardNo.trim().uppercase(),
                booker = index == 0
            )
        }
        if (normalizedVisitors.any { it.realName.isBlank() }) {
            return ReservationOrderBuildResult(error = "入园人必须填写真实姓名")
        }
        if (normalizedVisitors.any { !IdCardRegex.matches(it.idCardNo) }) {
            return ReservationOrderBuildResult(error = "身份证号格式需为 17 位数字加数字或 X")
        }
        val idCards = normalizedVisitors.map { it.idCardNo }
        if (idCards.distinct().size != idCards.size) {
            return ReservationOrderBuildResult(error = "同一个预约单不能重复填写同一身份证号")
        }
        if (normalizedVisitors.count { it.booker } != 1) {
            return ReservationOrderBuildResult(error = "必须且只能有一名预约本人")
        }
        val accountRealName = user.realName?.trim().orEmpty()
        if (accountRealName.isNotEmpty() && normalizedVisitors.first().realName != accountRealName) {
            return ReservationOrderBuildResult(error = "预约本人需与账号实名认证姓名一致")
        }

        // 重点：clientRequestId 使用 frontend-uuid，配合后端做幂等防重复提交。
        val request = CreateReservationOrderRequest(
            userId = userId,
            slotId = slot.slotId,
            visitorCount = visitorCount,
            contactName = contactName.trim().ifBlank { null },
            contactPhone = contactPhone.trim().ifBlank { null },
            visitors = normalizedVisitors,
            clientRequestId = "frontend-${UUID.randomUUID()}",
            remark = remark.trim().ifBlank { null }
        )
        return ReservationOrderBuildResult(request = request)
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

private data class ReservationOrderBuildResult(
    val request: CreateReservationOrderRequest? = null,
    val error: String? = null
)

private val IdCardRegex = Regex("^\\d{17}[\\dX]$")
