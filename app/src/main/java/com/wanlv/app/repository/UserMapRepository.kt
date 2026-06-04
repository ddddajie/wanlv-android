package com.wanlv.app.repository

import com.wanlv.app.network.ApiClient
import com.wanlv.app.pojo.dto.ScenicAreaDto
import com.wanlv.app.pojo.vo.PageVO
import org.json.JSONArray
import org.json.JSONObject

class UserMapRepository {
    suspend fun pageScenicAreas(pageNum: Int = 1, pageSize: Int = 12): PageVO<ScenicAreaDto> {
        // 后端历史上同时出现两套分页参数，这里统一封装并兼容传递。
        val data = ApiClient.get(
            "/map/scenic-areas/page",
            mapOf(
                "pageNum" to pageNum,
                "pageSize" to pageSize,
                "current" to pageNum,
                "size" to pageSize,
                "status" to 1
            )
        ) as JSONObject
        val records = data.optJSONArray("records").toScenicAreas()
        return PageVO(total = data.optLong("total", records.size.toLong()), records = records)
    }

    private fun JSONArray?.toScenicAreas(): List<ScenicAreaDto> {
        if (this == null) return emptyList()
        return List(length()) { index -> ScenicAreaDto.fromJson(getJSONObject(index)) }
    }
}
