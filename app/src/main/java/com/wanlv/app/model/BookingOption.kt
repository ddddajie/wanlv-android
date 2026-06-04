package com.wanlv.app.model

data class BookingDate(
    val label: String,
    val date: String
)

data class BookingTimeSlot(
    val time: String,
    val quota: String
)

data class TicketOption(
    val name: String,
    val price: Int,
    val originPrice: Int? = null,
    val note: String
)
