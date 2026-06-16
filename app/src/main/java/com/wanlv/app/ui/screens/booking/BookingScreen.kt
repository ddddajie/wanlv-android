package com.wanlv.app.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wanlv.app.data.MockData
import com.wanlv.app.ui.components.FloatingBottomBarAvoidance
import com.wanlv.app.ui.components.IOSCard
import com.wanlv.app.ui.components.QuantityStepper
import com.wanlv.app.ui.components.SectionHeader
import com.wanlv.app.ui.theme.WanLvBackground
import com.wanlv.app.ui.theme.WanLvDivider
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvGreenLight
import com.wanlv.app.ui.theme.WanLvMint
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary
import com.wanlv.app.viewmodel.BookingViewModel

@Composable
fun BookingScreen(viewModel: BookingViewModel = viewModel()) {
    LaunchedEffect(Unit) { viewModel.loadReservationData() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WanLvBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = FloatingBottomBarAvoidance + 74.dp)
        ) {
            BookingHeader(viewModel)
            ScenicCard(viewModel)
            SectionHeader("选择日期", showMore = false, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(MockData.bookingDates) { index, item ->
                    SelectPill(
                        title = item.label,
                        subtitle = item.date,
                        selected = index == viewModel.selectedDateIndex.intValue,
                        onClick = { viewModel.selectDate(index) }
                    )
                }
            }
            SectionHeader("选择时段", showMore = false, modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp))
            TimeSlotGrid(viewModel)
            SectionHeader("票务信息", showMore = false, modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp))
            MockData.tickets.forEachIndexed { index, ticket ->
                IOSCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    cornerRadius = 20.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(ticket.name, color = WanLvTextPrimary, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("￥${ticket.price}", color = WanLvGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                ticket.originPrice?.let {
                                    Text(
                                        " ￥$it",
                                        color = WanLvTextSecondary,
                                        fontSize = 12.sp,
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                }
                            }
                            Text(ticket.note, color = WanLvTextSecondary, fontSize = 12.sp)
                        }
                        QuantityStepper(
                            quantity = viewModel.ticketCounts[index] ?: 0,
                            onQuantityChange = { viewModel.updateTicketCount(index, it) }
                        )
                    }
                }
            }
        }
        Button(
            onClick = {},
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // 重点：只抬起底部操作按钮，导航栏本身继续保持悬浮玻璃效果。
                .padding(horizontal = 18.dp)
                .padding(bottom = FloatingBottomBarAvoidance, top = 18.dp)
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WanLvGreen)
        ) {
            Text("立即预约", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BookingHeader(viewModel: BookingViewModel) {
    val area = viewModel.selectedScenicArea.value
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .padding(18.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(WanLvGreenLight.copy(alpha = 0.85f), WanLvGreen.copy(alpha = 0.96f))))
            .padding(20.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(area?.scenicName ?: "灵山胜境", color = androidx.compose.ui.graphics.Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(area?.locationText?.ifBlank { area.address ?: "江苏 · 无锡 · 滨湖区" } ?: "江苏 · 无锡 · 滨湖区", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f), fontSize = 14.sp)
            Text(viewModel.loadMessage.value, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun ScenicCard(viewModel: BookingViewModel) {
    val spot = viewModel.selectedReservationSpot.value
    IOSCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        cornerRadius = 22.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .height(66.dp)
                    .width(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(WanLvMint, WanLvGreenLight.copy(alpha = 0.6f)))),
                contentAlignment = Alignment.Center
            ) {
                Text("⛰", fontSize = 28.sp)
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(spot?.spotName ?: "灵山大佛", color = WanLvTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(spot?.shortIntro ?: "庄严开阔的灵山大佛", color = WanLvTextSecondary, fontSize = 13.sp)
            }
            Text(if (spot == null) "待加载" else "可预约", color = WanLvGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SelectPill(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) WanLvGreen else androidx.compose.ui.graphics.Color.White)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = if (selected) androidx.compose.ui.graphics.Color.White else WanLvTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = if (selected) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f) else WanLvTextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun TimeSlotGrid(viewModel: BookingViewModel) {
    Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val displaySlots = if (viewModel.reservationSlots.isEmpty()) {
            MockData.timeSlots.map { it.time to it.quota }
        } else {
            viewModel.reservationSlots.map { "${it.startTime}-${it.endTime}" to "余${it.remainingCount}" }
        }
        displaySlots.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { slot ->
                    val index = displaySlots.indexOf(slot)
                    val selected = index == viewModel.selectedTimeIndex.intValue
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selected) WanLvGreen.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.White)
                            .clickable { viewModel.selectTime(index) }
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(slot.first, color = if (selected) WanLvGreen else WanLvTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(slot.second, color = if (selected) WanLvGreen else WanLvTextSecondary, fontSize = 12.sp)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f).background(WanLvDivider))
            }
        }
    }
}
