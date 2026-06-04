package com.wanlv.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanlv.app.network.AuthSession
import com.wanlv.app.pojo.dto.MapInitDto
import com.wanlv.app.pojo.dto.MapRouteDto
import com.wanlv.app.pojo.dto.MapSpotDto
import com.wanlv.app.pojo.dto.ScenicAreaDto
import com.wanlv.app.repository.UserMapRepository
import kotlinx.coroutines.launch

data class MapUiState(
    val loading: Boolean = false,
    val scenicAreas: List<ScenicAreaDto> = emptyList(),
    val selectedScenicArea: ScenicAreaDto? = null,
    val mapInit: MapInitDto? = null,
    val recommendedRoutes: List<MapRouteDto> = emptyList(),
    val visibleRouteIds: Set<String> = emptySet(),
    val selectedSpot: MapSpotDto? = null,
    val message: String? = null
)

class MapViewModel(
    private val mapRepository: UserMapRepository = UserMapRepository()
) : ViewModel() {
    var uiState by mutableStateOf(MapUiState(loading = true, message = "正在加载导游地图..."))
        private set

    fun loadMap() {
        if (uiState.scenicAreas.isNotEmpty() || uiState.loading.not() && uiState.mapInit != null) return
        viewModelScope.launch {
            uiState = uiState.copy(loading = true, message = "正在加载景区列表...")
            runCatching { mapRepository.pageScenicAreas(pageSize = 100) }
                .onSuccess { page ->
                    val first = page.records.firstOrNull()
                    uiState = uiState.copy(
                        scenicAreas = page.records,
                        selectedScenicArea = first,
                        message = if (first == null) "暂无可展示景区" else "正在加载地图数据..."
                    )
                    first?.let { loadScenicArea(it) }
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        loading = false,
                        message = error.message ?: "景区列表接口暂不可用"
                    )
                }
        }
    }

    fun selectScenicArea(area: ScenicAreaDto) {
        if (uiState.selectedScenicArea?.id == area.id) return
        viewModelScope.launch {
            uiState = uiState.copy(
                loading = true,
                selectedScenicArea = area,
                selectedSpot = null,
                message = "正在切换到 ${area.scenicName}..."
            )
            loadScenicArea(area)
        }
    }

    fun toggleRoute(route: MapRouteDto) {
        val nextIds = uiState.visibleRouteIds.toMutableSet()
        if (nextIds.contains(route.id)) {
            nextIds.remove(route.id)
        } else if (route.hasGeoJson) {
            nextIds.add(route.id)
        }
        uiState = uiState.copy(
            visibleRouteIds = nextIds,
            message = if (route.hasGeoJson) null else "该路线暂未配置轨迹"
        )
    }

    fun selectSpot(spot: MapSpotDto?) {
        uiState = uiState.copy(selectedSpot = spot)
    }

    private suspend fun loadScenicArea(area: ScenicAreaDto) {
        runCatching {
            val init = mapRepository.initMap(area.id)
            val agentRoute = AuthSession.userId?.let { userId ->
                runCatching { mapRepository.latestAgentRoute(userId, area.id) }.getOrNull()
            }
            val officialRoutes = init.routes.filter { it.routeType.equals("official", ignoreCase = true) || it.routeType.isNullOrBlank() }
            val recommendedRoutes = buildList {
                agentRoute?.toMapRoute()?.let { add(it) }
                addAll(officialRoutes)
            }
            // 默认只显示前两条有轨迹的路线，和 Web 端导游地图保持一致。
            val visibleIds = recommendedRoutes
                .filter { it.hasGeoJson }
                .take(2)
                .map { it.id }
                .toSet()
            Triple(init, recommendedRoutes, visibleIds)
        }.onSuccess { (init, routes, visibleIds) ->
            uiState = uiState.copy(
                loading = false,
                mapInit = init,
                recommendedRoutes = routes,
                visibleRouteIds = visibleIds,
                message = if (init.spots.isEmpty()) "地图已加载，暂无景点点位" else null
            )
        }.onFailure { error ->
            uiState = uiState.copy(
                loading = false,
                mapInit = null,
                recommendedRoutes = emptyList(),
                visibleRouteIds = emptySet(),
                message = error.message ?: "地图初始化接口暂不可用"
            )
        }
    }
}
