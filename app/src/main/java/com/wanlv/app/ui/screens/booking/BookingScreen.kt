package com.wanlv.app.ui.screens.booking

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wanlv.app.pojo.dto.ReservationSlotDto
import com.wanlv.app.pojo.dto.ReservationSpotDto
import com.wanlv.app.pojo.dto.ScenicAreaDto
import com.wanlv.app.ui.components.FloatingBottomBarAvoidance
import com.wanlv.app.ui.theme.WanLvBackground
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvGreenLight
import com.wanlv.app.ui.theme.WanLvMint
import com.wanlv.app.ui.theme.WanLvSurface
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary
import com.wanlv.app.viewmodel.BookingDateOption
import com.wanlv.app.viewmodel.BookingViewModel
import com.wanlv.app.viewmodel.ReservationQuotaSummary
import com.wanlv.app.viewmodel.ReservationVisitorInput
import com.wanlv.app.viewmodel.canReserve

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    viewModel: BookingViewModel = viewModel(),
    onOverlayVisibilityChange: (Boolean) -> Unit = {}
) {
    LaunchedEffect(Unit) { viewModel.loadReservationData() }

    val context = LocalContext.current
    var showScenicPicker by remember { mutableStateOf(false) }
    var showReservationSheet by remember { mutableStateOf(false) }
    val selectedSpot = viewModel.selectedReservationSpot.value
    val currentUser = viewModel.currentUser.value
    val currentUserName = currentUser?.realName ?: currentUser?.displayName ?: currentUser?.nickname.orEmpty()
    val currentUserPhone = currentUser?.phone.orEmpty()
    val sheetVisible = showScenicPicker || (showReservationSheet && selectedSpot != null)
    val backgroundBlur by animateDpAsState(
        targetValue = if (sheetVisible) 18.dp else 0.dp,
        label = "booking-background-blur"
    )
    val veilAlpha by animateFloatAsState(
        targetValue = if (sheetVisible) 1f else 0f,
        label = "booking-background-veil"
    )

    LaunchedEffect(sheetVisible) {
        onOverlayVisibilityChange(sheetVisible)
    }

    DisposableEffect(Unit) {
        onDispose { onOverlayVisibilityChange(false) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFEFFBF7),
                        WanLvBackground,
                        Color(0xFFF9FAFB)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 重点：弹窗为液态玻璃半透明样式，打开时先把底层内容虚化，避免文字透出来影响可读性。
                .blur(backgroundBlur)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(bottom = FloatingBottomBarAvoidance + 28.dp)
            ) {
                BookingHero(viewModel = viewModel, onSwitchScenic = { showScenicPicker = true })
                DateSelector(
                    options = viewModel.dateOptions,
                    selectedIndex = viewModel.selectedDateIndex.intValue,
                    onSelect = viewModel::selectDate
                )
                SpotQuotaSection(
                    viewModel = viewModel,
                    onSpotClick = { spot ->
                        viewModel.selectReservationSpot(spot)
                        showReservationSheet = true
                    }
                )
            }
        }

        if (sheetVisible || veilAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.24f * veilAlpha),
                                WanLvMint.copy(alpha = 0.18f * veilAlpha),
                                WanLvBackground.copy(alpha = 0.30f * veilAlpha)
                            )
                        )
                    )
            )
        }
    }

    if (showScenicPicker) {
        ModalBottomSheet(
            onDismissRequest = { showScenicPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.Transparent,
            scrimColor = Color.Transparent,
            tonalElevation = 0.dp,
            dragHandle = null
        ) {
            ScenicPickerSheet(
                scenicAreas = viewModel.scenicAreas,
                selected = viewModel.selectedScenicArea.value,
                onSelect = { area ->
                    showScenicPicker = false
                    viewModel.selectScenicArea(area)
                }
            )
        }
    }

    if (showReservationSheet && selectedSpot != null) {
        ModalBottomSheet(
            onDismissRequest = { showReservationSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.Transparent,
            scrimColor = Color.Transparent,
            tonalElevation = 0.dp,
            dragHandle = null
        ) {
            ReservationSpotSheet(
                spot = selectedSpot,
                dateOption = viewModel.selectedDateOption(),
                quota = viewModel.selectedSpotQuota(),
                slots = viewModel.reservationSlots,
                slotsLoading = viewModel.slotsLoading.value,
                slotMessage = viewModel.slotMessage.value,
                selectedTimeIndex = viewModel.selectedTimeIndex.intValue,
                reserveButtonText = viewModel.reserveButtonHint(),
                submitting = viewModel.submitting.value,
                currentUserName = currentUserName,
                currentUserPhone = currentUserPhone,
                onSelectSlot = viewModel::selectTime,
                onClose = { showReservationSheet = false },
                onReserve = { visitorCount, contactName, contactPhone, remark, visitors ->
                    viewModel.submitReservation(
                        visitorCount = visitorCount,
                        contactName = contactName,
                        contactPhone = contactPhone,
                        remark = remark,
                        visitors = visitors,
                        onSuccess = { reservationNo ->
                            showReservationSheet = false
                            Toast.makeText(context, "预约成功：$reservationNo", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { message ->
                            Toast.makeText(context, message.ifBlank { "预约提交失败" }, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun BookingHero(
    viewModel: BookingViewModel,
    onSwitchScenic: () -> Unit
) {
    val area = viewModel.selectedScenicArea.value
    val totalRemaining = viewModel.reservationQuotaSummaries.values
        .filterNot { it.loading || it.failed }
        .sumOf { it.remainingCount }

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        cornerRadius = 28.dp,
        padding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassIcon(Icons.Rounded.CalendarMonth, contentDescription = "预约")
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("景点预约", color = WanLvTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(
                        viewModel.loadMessage.value,
                        color = WanLvTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SwitchScenicButton(onClick = onSwitchScenic)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    area?.scenicName ?: "请选择景区",
                    color = WanLvTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = WanLvGreen, modifier = Modifier.size(16.dp))
                    Text(
                        area?.locationText?.ifBlank { area.address ?: "景区位置待完善" } ?: "切换景区后查看可预约景点",
                        color = WanLvTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderStatCard(
                    label = "景点",
                    value = viewModel.reservationSpots.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                HeaderStatCard(
                    label = "可预约余量",
                    value = totalRemaining.toString(),
                    suffix = "人",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SwitchScenicButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.58f))
            .border(1.dp, Color.White.copy(alpha = 0.80f), RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("切换", color = WanLvGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "切换景区", tint = WanLvGreen, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun HeaderStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    suffix: String = ""
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.46f))
            .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = WanLvTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, color = WanLvGreen, fontSize = 24.sp, fontWeight = FontWeight.Black)
            if (suffix.isNotBlank()) {
                Text(suffix, color = WanLvTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DateSelector(
    options: List<BookingDateOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BookingSectionTitle(
            title = "选择日期",
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(options.size) { index ->
                DateChip(
                    option = options[index],
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun DateChip(
    option: BookingDateOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .width(82.dp)
            .height(86.dp)
            .shadow(
                elevation = if (selected) 16.dp else 8.dp,
                shape = shape,
                ambientColor = WanLvGreen.copy(alpha = if (selected) 0.20f else 0.05f),
                spotColor = Color(0xFF64727F).copy(alpha = if (selected) 0.12f else 0.06f)
            )
            .clip(shape)
            .background(
                if (selected) {
                    Brush.verticalGradient(listOf(WanLvGreenLight, WanLvGreen))
                } else {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.88f), WanLvSurface.copy(alpha = 0.70f)))
                }
            )
            .border(1.dp, Color.White.copy(alpha = 0.78f), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            option.label,
            color = if (selected) Color.White else WanLvTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(7.dp))
        Text(
            option.dateText,
            color = if (selected) Color.White.copy(alpha = 0.90f) else WanLvTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SpotQuotaSection(
    viewModel: BookingViewModel,
    onSpotClick: (ReservationSpotDto) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BookingSectionTitle(
            title = "景点预约名额",
            trailing = "${viewModel.reservationSpots.size} 个景点",
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        if (viewModel.reservationSpots.isEmpty()) {
            EmptySpotCard(message = viewModel.loadMessage.value)
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                viewModel.reservationSpots.forEach { spot ->
                    SpotQuotaCard(
                        spot = spot,
                        quota = viewModel.reservationQuotaSummaries[spot.spotId],
                        onClick = { onSpotClick(spot) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotQuotaCard(
    spot: ReservationSpotDto,
    quota: ReservationQuotaSummary?,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        cornerRadius = 22.dp,
        padding = PaddingValues(horizontal = 15.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassIcon(Icons.Rounded.Place, contentDescription = spot.spotName, size = 48.dp)
            Text(
                text = spot.spotName,
                modifier = Modifier.weight(1f),
                color = WanLvTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            QuotaBadge(quota = quota)
        }
    }
}

@Composable
private fun QuotaBadge(quota: ReservationQuotaSummary?) {
    val loading = quota == null || quota.loading
    val failed = quota?.failed == true
    val primaryText = when {
        loading -> "加载中"
        failed -> "名额未知"
        quota?.availableSlotCount == 0 -> "未开放"
        else -> "余 ${quota?.remainingCount ?: 0}"
    }
    val secondaryText = when {
        loading -> "预约容量"
        failed -> "点击查看"
        else -> "容量 ${quota?.totalCapacity ?: 0}"
    }

    Column(
        modifier = Modifier
            .widthIn(min = 82.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(WanLvMint.copy(alpha = if (loading || failed) 0.44f else 0.70f))
            .border(1.dp, Color.White.copy(alpha = 0.76f), RoundedCornerShape(17.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(15.dp), color = WanLvGreen, strokeWidth = 2.dp)
        } else {
            Text(primaryText, color = WanLvGreen, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
        Text(secondaryText, color = WanLvTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptySpotCard(message: String) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        cornerRadius = 22.dp,
        padding = PaddingValues(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassIcon(Icons.Rounded.Schedule, contentDescription = "暂无景点", size = 44.dp)
            Text(
                message,
                color = WanLvTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReservationSpotSheet(
    spot: ReservationSpotDto,
    dateOption: BookingDateOption,
    quota: ReservationQuotaSummary?,
    slots: List<ReservationSlotDto>,
    slotsLoading: Boolean,
    slotMessage: String,
    selectedTimeIndex: Int,
    reserveButtonText: String,
    submitting: Boolean,
    currentUserName: String,
    currentUserPhone: String,
    onSelectSlot: (Int) -> Unit,
    onClose: () -> Unit,
    onReserve: (Int, String, String, String, List<ReservationVisitorInput>) -> Unit
) {
    val selectedSlot = slots.getOrNull(selectedTimeIndex)
    val maxVisitorCount = maxOf(1, minOf(5, selectedSlot?.remainingCount ?: 5))
    var visitorCount by remember(spot.spotId) { mutableIntStateOf(1) }
    var contactName by remember(spot.spotId) { mutableStateOf(currentUserName) }
    var contactPhone by remember(spot.spotId) { mutableStateOf(currentUserPhone) }
    var remark by remember(spot.spotId) { mutableStateOf("") }
    val visitors = remember(spot.spotId) {
        mutableStateListOf(ReservationVisitorInput(realName = currentUserName, idCardNo = ""))
    }

    fun syncVisitorCount(count: Int) {
        val normalizedCount = count.coerceIn(1, maxVisitorCount)
        visitorCount = normalizedCount
        while (visitors.size < normalizedCount) {
            visitors.add(ReservationVisitorInput(realName = "", idCardNo = ""))
        }
        while (visitors.size > normalizedCount) {
            visitors.removeAt(visitors.lastIndex)
        }
        if (visitors.isNotEmpty() && visitors.first().realName.isBlank() && currentUserName.isNotBlank()) {
            visitors[0] = visitors.first().copy(realName = currentUserName)
        }
    }

    LaunchedEffect(currentUserName, currentUserPhone, spot.spotId) {
        if (contactName.isBlank() && currentUserName.isNotBlank()) contactName = currentUserName
        if (contactPhone.isBlank() && currentUserPhone.isNotBlank()) contactPhone = currentUserPhone
        if (visitors.isNotEmpty() && visitors.first().realName.isBlank() && currentUserName.isNotBlank()) {
            visitors[0] = visitors.first().copy(realName = currentUserName)
        }
    }

    LaunchedEffect(maxVisitorCount, spot.spotId) {
        if (visitorCount > maxVisitorCount) syncVisitorCount(maxVisitorCount)
    }

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp)
            .padding(bottom = 16.dp),
        cornerRadius = 28.dp,
        padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 42.dp, height = 5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WanLvTextSecondary.copy(alpha = 0.22f))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassIcon(Icons.Rounded.Place, contentDescription = spot.spotName, size = 46.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        spot.spotName,
                        color = WanLvTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${dateOption.label} ${dateOption.dateText} · ${quotaLabel(quota)}",
                        color = WanLvTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                SmallGlassCloseButton(onClick = onClose)
            }

            // 重点：主页面不再显示预约时段，时段只在当前景点弹窗里选择。
            if (slots.isEmpty()) {
                EmptySlotPanel(loading = slotsLoading, message = slotMessage)
            } else {
                SlotGrid(
                    slots = slots,
                    selectedTimeIndex = selectedTimeIndex,
                    onSelectSlot = onSelectSlot
                )
            }

            ReservationOrderForm(
                visitorCount = visitorCount,
                maxVisitorCount = maxVisitorCount,
                contactName = contactName,
                contactPhone = contactPhone,
                remark = remark,
                visitors = visitors,
                onVisitorCountChange = ::syncVisitorCount,
                onContactNameChange = { contactName = it },
                onContactPhoneChange = { contactPhone = it },
                onRemarkChange = { remark = it },
                onVisitorChange = { index, visitor ->
                    if (index in visitors.indices) visitors[index] = visitor
                }
            )

            Button(
                onClick = { onReserve(visitorCount, contactName, contactPhone, remark, visitors.toList()) },
                enabled = selectedTimeIndex >= 0 && !slotsLoading && !submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WanLvGreen,
                    disabledContainerColor = WanLvGreenLight.copy(alpha = 0.26f),
                    contentColor = Color.White,
                    disabledContentColor = WanLvTextSecondary.copy(alpha = 0.58f)
                )
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(7.dp))
                Text(reserveButtonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReservationOrderForm(
    visitorCount: Int,
    maxVisitorCount: Int,
    contactName: String,
    contactPhone: String,
    remark: String,
    visitors: List<ReservationVisitorInput>,
    onVisitorCountChange: (Int) -> Unit,
    onContactNameChange: (String) -> Unit,
    onContactPhoneChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onVisitorChange: (Int, ReservationVisitorInput) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassIcon(Icons.Rounded.Person, contentDescription = "预约信息", size = 38.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("预约信息", color = WanLvTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text("预约本人需与账号实名认证信息一致", color = WanLvTextSecondary, fontSize = 11.sp)
            }
        }

        VisitorCountStepper(
            count = visitorCount,
            maxCount = maxVisitorCount,
            onChange = onVisitorCountChange
        )

        FormLabel("联系人")
        BookingTextField(
            value = contactName,
            onValueChange = onContactNameChange,
            placeholder = "联系人姓名"
        )

        FormLabel("联系电话")
        BookingTextField(
            value = contactPhone,
            onValueChange = onContactPhoneChange,
            placeholder = "联系电话",
            keyboardType = KeyboardType.Phone
        )

        FormLabel("备注")
        BookingTextField(
            value = remark,
            onValueChange = onRemarkChange,
            placeholder = "可填写特殊需求，选填",
            singleLine = false
        )

        FormDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                "入园人信息",
                modifier = Modifier.weight(1f),
                color = WanLvTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
            Text("身份证仅用于本次提交", color = WanLvTextSecondary, fontSize = 11.sp)
        }

        visitors.forEachIndexed { index, visitor ->
            VisitorInfoFields(
                index = index,
                visitor = visitor,
                onChange = { onVisitorChange(index, it) }
            )
        }
    }
}

@Composable
private fun VisitorCountStepper(
    count: Int,
    maxCount: Int,
    onChange: (Int) -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.48f))
            .border(1.dp, Color.White.copy(alpha = 0.72f), shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("预约人数", color = WanLvTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("最多 5 人，当前最多可约 $maxCount 人", color = WanLvTextSecondary, fontSize = 11.sp)
        }
        StepCircleButton(
            icon = Icons.Rounded.Remove,
            label = "减少人数",
            enabled = count > 1,
            onClick = { onChange(count - 1) }
        )
        Text(
            count.toString(),
            color = WanLvTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(28.dp)
        )
        StepCircleButton(
            icon = Icons.Rounded.Add,
            label = "增加人数",
            enabled = count < maxCount,
            onClick = { onChange(count + 1) }
        )
    }
}

@Composable
private fun StepCircleButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (enabled) WanLvGreen.copy(alpha = 0.12f) else WanLvTextSecondary.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.74f), CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) WanLvGreen else WanLvTextSecondary.copy(alpha = 0.44f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun VisitorInfoFields(
    index: Int,
    visitor: ReservationVisitorInput,
    onChange: (ReservationVisitorInput) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                if (index == 0) "预约本人" else "同行人 ${index + 1}",
                color = if (index == 0) WanLvGreen else WanLvTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            if (index == 0) {
                Text(
                    "本人",
                    color = WanLvGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(WanLvGreen.copy(alpha = 0.10f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
        BookingTextField(
            value = visitor.realName,
            onValueChange = { onChange(visitor.copy(realName = it)) },
            placeholder = "真实姓名"
        )
        BookingTextField(
            value = visitor.idCardNo,
            onValueChange = { onChange(visitor.copy(idCardNo = it.uppercase())) },
            placeholder = "身份证号",
            keyboardType = KeyboardType.Text
        )
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(text, color = WanLvTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun FormDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(WanLvTextSecondary.copy(alpha = 0.12f))
    )
}

@Composable
private fun BookingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (singleLine) 54.dp else 94.dp),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 4,
        placeholder = {
            Text(
                placeholder,
                color = WanLvTextSecondary.copy(alpha = 0.66f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedTextColor = WanLvTextPrimary,
            unfocusedTextColor = WanLvTextPrimary,
            focusedContainerColor = Color.White.copy(alpha = 0.52f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.44f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = WanLvGreen
        )
    )
}

@Composable
private fun EmptySlotPanel(loading: Boolean, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.48f))
            .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = WanLvGreen, strokeWidth = 3.dp)
            } else {
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = WanLvTextSecondary, modifier = Modifier.size(22.dp))
            }
            Text(
                text = message.ifBlank { "当前日期暂无开放时段" },
                color = WanLvTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SlotGrid(
    slots: List<ReservationSlotDto>,
    selectedTimeIndex: Int,
    onSelectSlot: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        slots.chunked(2).forEach { rowSlots ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowSlots.forEach { slot ->
                    val index = slots.indexOf(slot)
                    SlotChip(
                        slot = slot,
                        selected = index == selectedTimeIndex,
                        onClick = { onSelectSlot(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowSlots.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SlotChip(
    slot: ReservationSlotDto,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = slot.canReserve
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .height(88.dp)
            .clip(shape)
            .background(
                when {
                    selected -> WanLvGreen.copy(alpha = 0.16f)
                    enabled -> Color.White.copy(alpha = 0.62f)
                    else -> Color.White.copy(alpha = 0.34f)
                }
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) WanLvGreen.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.72f),
                shape = shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "${slot.startTime}-${slot.endTime}",
            color = when {
                selected -> WanLvGreen
                enabled -> WanLvTextPrimary
                else -> WanLvTextSecondary.copy(alpha = 0.48f)
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(7.dp))
        Text(
            if (enabled) "余${slot.remainingCount} / ${slot.totalCapacity}" else "不可预约",
            color = if (selected) WanLvGreen else WanLvTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ScenicPickerSheet(
    scenicAreas: List<ScenicAreaDto>,
    selected: ScenicAreaDto?,
    onSelect: (ScenicAreaDto) -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp)
            .padding(bottom = 16.dp),
        cornerRadius = 28.dp,
        padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 42.dp, height = 5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WanLvTextSecondary.copy(alpha = 0.22f))
            )
            Text("选择景区", color = WanLvTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)

            if (scenicAreas.isEmpty()) {
                EmptySpotCard(message = "暂无可切换景区")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 390.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(scenicAreas, key = { it.id }) { area ->
                        ScenicAreaRow(
                            area = area,
                            selected = selected?.id == area.id,
                            onClick = { onSelect(area) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenicAreaRow(
    area: ScenicAreaDto,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) WanLvGreen.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.48f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) WanLvGreen.copy(alpha = 0.48f) else Color.White.copy(alpha = 0.72f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassIcon(Icons.Rounded.LocationOn, contentDescription = area.scenicName, size = 42.dp, active = selected)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                area.scenicName,
                color = WanLvTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                area.locationText.ifBlank { area.address ?: "景区位置待完善" },
                color = WanLvTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = "当前景区", tint = WanLvGreen, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun BookingSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = WanLvTextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Black
        )
        trailing?.let {
            Text(it, color = WanLvTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GlassIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    active: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) WanLvGreen.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.42f))
            .border(1.dp, Color.White.copy(alpha = 0.76f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (active) WanLvGreen else WanLvTextSecondary,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

@Composable
private fun SmallGlassCloseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.54f))
            .border(1.dp, Color.White.copy(alpha = 0.78f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = WanLvTextPrimary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .shadow(
                elevation = 22.dp,
                shape = shape,
                ambientColor = Color(0xFF8FA0AE).copy(alpha = 0.18f),
                spotColor = Color(0xFF64727F).copy(alpha = 0.13f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.88f),
                        WanLvSurface.copy(alpha = 0.68f),
                        WanLvMint.copy(alpha = 0.36f),
                        Color.White.copy(alpha = 0.76f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.80f), shape)
            .padding(padding),
        content = content
    )
}

private fun quotaLabel(quota: ReservationQuotaSummary?): String =
    when {
        quota == null || quota.loading -> "名额加载中"
        quota.failed -> "名额待刷新"
        quota.availableSlotCount == 0 -> "暂无开放时段"
        else -> "余 ${quota.remainingCount} / 容量 ${quota.totalCapacity}"
    }
