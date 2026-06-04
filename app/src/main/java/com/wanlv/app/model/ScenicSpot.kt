package com.wanlv.app.model

data class ScenicSpot(
    val name: String,
    val location: String,
    val rating: String,
    val tag: String,
    val description: String,
    val distance: String,
    val imageEmoji: String,
    val coverImageUrl: String? = null
)
