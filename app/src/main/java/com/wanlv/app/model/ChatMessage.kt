package com.wanlv.app.model

data class ChatMessage(
    val id: Long,
    val content: String,
    val fromUser: Boolean
)
