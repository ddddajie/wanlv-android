package com.wanlv.app.data

import com.wanlv.app.model.BookingDate
import com.wanlv.app.model.BookingTimeSlot
import com.wanlv.app.model.ChatMessage
import com.wanlv.app.model.ScenicSpot
import com.wanlv.app.model.TicketOption
import com.wanlv.app.model.UserProfile

object MockData {
    val mustPlaySpots = listOf(
        ScenicSpot("灵山大佛", "无锡 · 滨湖", "4.8", "5A景区", "巨型佛像与湖山风景相映，适合半日游览。", "1.2km", "⛰"),
        ScenicSpot("拈花湾", "无锡 · 马山", "4.7", "禅意小镇", "夜景、演艺与街区体验丰富。", "3.5km", "✦"),
        ScenicSpot("鼋头渚", "无锡 · 太湖", "4.6", "湖景", "太湖经典景点，春季赏樱热门。", "5.8km", "◒")
    )

    val routes = listOf(
        "山水禅境之旅 · 3日游",
        "亲子轻松路线 · 半日游",
        "经典打卡路线 · 1日游"
    )

    val bookingDates = listOf(
        BookingDate("今天", "05.20"),
        BookingDate("明天", "05.21"),
        BookingDate("周四", "05.22"),
        BookingDate("周五", "05.23"),
        BookingDate("更多日期", "")
    )

    val timeSlots = listOf(
        BookingTimeSlot("08:00-10:00", "余120"),
        BookingTimeSlot("10:00-12:00", "余86"),
        BookingTimeSlot("12:00-14:00", "余65"),
        BookingTimeSlot("14:00-16:00", "余90")
    )

    val tickets = listOf(
        TicketOption("成人票", 108, 118, "凭有效证件入园"),
        TicketOption("学生/老人优惠票", 54, 68, "入园请携带身份证、学生证或老人证")
    )

    val initialMessages = listOf(
        ChatMessage(1, "你好，我是万旅小助手。可以帮你规划路线、查询开放时间，也能推荐附近服务。", false),
        ChatMessage(2, "灵山胜境适合带小朋友去吗？", true),
        ChatMessage(3, "适合。建议上午参观灵山大佛和九龙灌浴，中午在景区餐厅休息，下午选择拈花湾轻松游览。", false)
    )

    val userProfile = UserProfile("游客001", "普通用户", true)
}
