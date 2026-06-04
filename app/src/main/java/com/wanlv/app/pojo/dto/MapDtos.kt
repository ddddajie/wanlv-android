package com.wanlv.app.pojo.dto

import org.json.JSONArray
import org.json.JSONObject

data class MapBoundsDto(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double
) {
    companion object {
        fun fromJsonText(text: String?): MapBoundsDto? {
            if (text.isNullOrBlank()) return null
            return runCatching {
                val json = JSONObject(text)
                MapBoundsDto(
                    west = json.optDouble("west"),
                    south = json.optDouble("south"),
                    east = json.optDouble("east"),
                    north = json.optDouble("north")
                )
            }.getOrNull()
        }
    }
}

data class MapScenicAreaDto(
    val id: Long,
    val scenicName: String,
    val description: String?,
    val province: String?,
    val city: String?,
    val district: String?,
    val address: String?,
    val openingHours: String?,
    val longitude: Double?,
    val latitude: Double?,
    val mapCenterLng: Double?,
    val mapCenterLat: Double?,
    val defaultZoom: Double?,
    val minZoom: Double?,
    val maxZoom: Double?,
    val mapBounds: MapBoundsDto?,
    val mapBaseImageUrl: String?
) {
    val centerLongitude: Double?
        get() = mapCenterLng ?: longitude

    val centerLatitude: Double?
        get() = mapCenterLat ?: latitude

    companion object {
        fun fromJson(json: JSONObject) = MapScenicAreaDto(
            id = json.optLong("id"),
            scenicName = json.optString("scenicName", "未知景区"),
            description = json.optNullableString("description"),
            province = json.optNullableString("province"),
            city = json.optNullableString("city"),
            district = json.optNullableString("district"),
            address = json.optNullableString("address"),
            openingHours = json.optNullableString("openingHours"),
            longitude = json.optNullableDouble("longitude"),
            latitude = json.optNullableDouble("latitude"),
            mapCenterLng = json.optNullableDouble("mapCenterLng"),
            mapCenterLat = json.optNullableDouble("mapCenterLat"),
            defaultZoom = json.optNullableDouble("defaultZoom"),
            minZoom = json.optNullableDouble("minZoom"),
            maxZoom = json.optNullableDouble("maxZoom"),
            mapBounds = MapBoundsDto.fromJsonText(json.optNullableString("mapBoundsJson")),
            mapBaseImageUrl = json.optNullableString("mapBaseImageUrl")
        )
    }
}

data class MapSpotDto(
    val id: Long,
    val spotName: String,
    val poiType: String?,
    val iconType: String?,
    val longitude: Double?,
    val latitude: Double?,
    val recommendedLevel: Int?,
    val description: String?
) {
    val hasValidCoordinate: Boolean
        get() = longitude != null && latitude != null && longitude in -180.0..180.0 && latitude in -90.0..90.0

    companion object {
        fun fromJson(json: JSONObject) = MapSpotDto(
            id = json.optLong("id"),
            spotName = json.optNullableString("spotName")
                ?: json.optNullableString("name")
                ?: "未命名景点",
            poiType = json.optNullableString("poiType"),
            iconType = json.optNullableString("iconType"),
            longitude = json.optNullableDouble("longitude"),
            latitude = json.optNullableDouble("latitude"),
            recommendedLevel = json.optNullableInt("recommendedLevel"),
            description = json.optNullableString("description")
        )
    }
}

data class MapRouteDto(
    val id: String,
    val routeName: String,
    val routeType: String?,
    val durationMinutes: Int?,
    val distanceMeters: Double?,
    val description: String?,
    val recommendedReason: String?,
    val suitableCrowd: String?,
    val geojson: String?,
    val isAgentRoute: Boolean = false
) {
    val hasGeoJson: Boolean
        get() = !geojson.isNullOrBlank()

    companion object {
        fun fromJson(json: JSONObject) = MapRouteDto(
            id = json.optNullableLong("id")?.toString() ?: json.optNullableString("id") ?: "route-${json.hashCode()}",
            routeName = json.optNullableString("routeName") ?: "未命名路线",
            routeType = json.optNullableString("routeType"),
            durationMinutes = json.optNullableInt("durationMinutes"),
            distanceMeters = json.optNullableDouble("distanceMeters"),
            description = json.optNullableString("description"),
            recommendedReason = json.optNullableString("recommendedReason"),
            suitableCrowd = json.optNullableString("suitableCrowd"),
            geojson = json.optNullableString("geojson")
        )
    }
}

data class MapGeoFeatureDto(
    val id: String,
    val featureName: String,
    val featureType: String?,
    val geometryType: String?,
    val featureSubType: String?,
    val geojson: String?,
    val propertiesJson: String?
) {
    companion object {
        fun fromJson(json: JSONObject) = MapGeoFeatureDto(
            id = json.optNullableLong("id")?.toString() ?: json.optNullableString("id") ?: "feature-${json.hashCode()}",
            featureName = json.optNullableString("featureName") ?: "空间要素",
            featureType = json.optNullableString("featureType"),
            geometryType = json.optNullableString("geometryType"),
            featureSubType = json.optNullableString("featureSubType"),
            geojson = json.optNullableString("geojson"),
            propertiesJson = json.optNullableString("propertiesJson")
        )
    }
}

data class AgentRouteGeoDto(
    val id: Long,
    val routeName: String,
    val geojson: String?,
    val spotNamesJson: String?
) {
    fun toMapRoute(): MapRouteDto = MapRouteDto(
        id = "agent-$id",
        routeName = routeName,
        routeType = "agent_custom",
        durationMinutes = null,
        distanceMeters = null,
        description = decodeSpotNames().takeIf { it.isNotBlank() },
        recommendedReason = "根据你的智能问答生成",
        suitableCrowd = "专属推荐",
        geojson = geojson,
        isAgentRoute = true
    )

    private fun decodeSpotNames(): String {
        if (spotNamesJson.isNullOrBlank()) return ""
        return runCatching {
            val array = JSONArray(spotNamesJson)
            List(array.length()) { index -> array.optString(index) }
                .filter { it.isNotBlank() }
                .joinToString(" -> ")
        }.getOrDefault("")
    }

    companion object {
        fun fromJson(json: JSONObject) = AgentRouteGeoDto(
            id = json.optLong("id"),
            routeName = json.optNullableString("routeName") ?: "我的专属路线",
            geojson = json.optNullableString("geojson"),
            spotNamesJson = json.optNullableString("spotNamesJson")
        )
    }
}

data class MapInitDto(
    val scenicArea: MapScenicAreaDto?,
    val spots: List<MapSpotDto>,
    val routes: List<MapRouteDto>,
    val geoFeatures: List<MapGeoFeatureDto>
) {
    companion object {
        fun fromJson(json: JSONObject) = MapInitDto(
            scenicArea = json.optJSONObject("scenicArea")?.let(MapScenicAreaDto::fromJson),
            spots = json.optJSONArray("spots").toMapSpots(),
            routes = json.optJSONArray("routes").toMapRoutes(),
            geoFeatures = json.optJSONArray("geoFeatures").toGeoFeatures()
        )
    }
}

private fun JSONArray?.toMapSpots(): List<MapSpotDto> {
    if (this == null) return emptyList()
    return List(length()) { index -> MapSpotDto.fromJson(getJSONObject(index)) }
}

private fun JSONArray?.toMapRoutes(): List<MapRouteDto> {
    if (this == null) return emptyList()
    return List(length()) { index -> MapRouteDto.fromJson(getJSONObject(index)) }
}

private fun JSONArray?.toGeoFeatures(): List<MapGeoFeatureDto> {
    if (this == null) return emptyList()
    return List(length()) { index -> MapGeoFeatureDto.fromJson(getJSONObject(index)) }
}
