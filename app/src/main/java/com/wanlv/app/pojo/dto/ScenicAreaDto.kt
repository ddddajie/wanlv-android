package com.wanlv.app.pojo.dto

import org.json.JSONObject

data class ScenicAreaDto(
    val id: Long,
    val scenicName: String,
    val scenicLevel: String?,
    val province: String?,
    val city: String?,
    val district: String?,
    val address: String?,
    val description: String?,
    val coverImageUrl: String?,
    val recommendedLevel: Int?,
    val status: Int?
) {
    val locationText: String
        get() = listOfNotNull(province, city, district).filter { it.isNotBlank() }.joinToString(" · ")

    companion object {
        fun fromJson(json: JSONObject) = ScenicAreaDto(
            id = json.optLong("id"),
            scenicName = json.optString("scenicName", "未知景区"),
            scenicLevel = json.optNullableString("scenicLevel")
                ?: json.optNullableString("levelName")
                ?: json.optNullableString("grade"),
            province = json.optNullableString("province"),
            city = json.optNullableString("city"),
            district = json.optNullableString("district"),
            address = json.optNullableString("address"),
            description = json.optNullableString("description"),
            coverImageUrl = json.optNullableString("coverImageUrl"),
            recommendedLevel = json.optNullableInt("recommendedLevel"),
            status = json.optNullableInt("status")
        )
    }
}
