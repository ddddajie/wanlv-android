package com.wanlv.app.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanlv.app.data.MockData
import com.wanlv.app.model.ScenicSpot
import com.wanlv.app.repository.UserMapRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val mapRepository: UserMapRepository = UserMapRepository()
) : ViewModel() {
    val scenicSpots = mutableStateListOf<ScenicSpot>().apply { addAll(MockData.mustPlaySpots) }
    val loadMessage = mutableStateOf("正在加载景区推荐...")

    fun loadScenicAreas() {
        viewModelScope.launch {
            runCatching { mapRepository.pageScenicAreas() }
                .onSuccess { page ->
                    if (page.records.isNotEmpty()) {
                        scenicSpots.clear()
                        scenicSpots.addAll(page.records.map {
                            ScenicSpot(
                                name = it.scenicName,
                                location = it.locationText.ifBlank { it.address ?: "景区" },
                                rating = it.recommendedLevel?.toString() ?: "推荐",
                                tag = it.scenicLevel ?: "可游览",
                                description = it.description ?: "暂无景区简介",
                                distance = "详情",
                                imageEmoji = "景",
                                coverImageUrl = it.coverImageUrl
                            )
                        })
                        loadMessage.value = "已加载 ${page.records.size} 个景区"
                    } else {
                        loadMessage.value = "暂无景区数据"
                    }
                }
                .onFailure { loadMessage.value = "接口暂不可用，已显示本地推荐" }
        }
    }
}
