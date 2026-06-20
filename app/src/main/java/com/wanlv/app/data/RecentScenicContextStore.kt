package com.wanlv.app.data

import android.content.Context

data class RecentScenicContext(
    val scenicAreaId: Long,
    val scenicAreaName: String
)

object RecentScenicContextStore {
    private const val PreferencesName = "recent_scenic_context"
    private const val ScenicAreaIdKey = "scenic_area_id"
    private const val ScenicAreaNameKey = "scenic_area_name"

    fun save(context: Context, scenicAreaId: Long, scenicAreaName: String) {
        if (scenicAreaId <= 0L || scenicAreaName.isBlank()) return
        context.applicationContext
            .getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putLong(ScenicAreaIdKey, scenicAreaId)
            .putString(ScenicAreaNameKey, scenicAreaName)
            .apply()
    }

    fun load(context: Context): RecentScenicContext? {
        val preferences = context.applicationContext
            .getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        val scenicAreaId = preferences.getLong(ScenicAreaIdKey, 0L)
        val scenicAreaName = preferences.getString(ScenicAreaNameKey, null)?.trim().orEmpty()
        return if (scenicAreaId > 0L && scenicAreaName.isNotBlank()) {
            RecentScenicContext(scenicAreaId, scenicAreaName)
        } else {
            null
        }
    }
}
