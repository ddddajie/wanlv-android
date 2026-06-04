package com.wanlv.app.pojo.dto

import org.json.JSONObject

fun JSONObject.optNullableString(name: String): String? =
    if (has(name) && !isNull(name)) optString(name).takeIf { it.isNotBlank() } else null

fun JSONObject.optNullableInt(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name) else null
