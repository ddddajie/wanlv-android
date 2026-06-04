package com.wanlv.app.ui.screens.map

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wanlv.app.config.AppConfig
import com.wanlv.app.pojo.dto.MapBoundsDto
import com.wanlv.app.pojo.dto.MapGeoFeatureDto
import com.wanlv.app.pojo.dto.MapInitDto
import com.wanlv.app.pojo.dto.MapRouteDto
import com.wanlv.app.pojo.dto.MapScenicAreaDto
import com.wanlv.app.pojo.dto.MapSpotDto
import com.wanlv.app.pojo.dto.ScenicAreaDto
import com.wanlv.app.ui.components.IOSCard
import com.wanlv.app.ui.theme.WanLvBackground
import com.wanlv.app.ui.theme.WanLvDivider
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvGreenLight
import com.wanlv.app.ui.theme.WanLvSurface
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary
import com.wanlv.app.viewmodel.MapUiState
import com.wanlv.app.viewmodel.MapViewModel
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    val uiState = viewModel.uiState
    var showScenicPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadMap()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WanLvBackground)
    ) {
        MapLibreGuideMap(
            uiState = uiState,
            onSpotClick = viewModel::selectSpot,
            modifier = Modifier.fillMaxSize()
        )

        MapToolDock(
            onOpenSettings = { showScenicPicker = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 14.dp)
        )

        MapLegend(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 18.dp, start = 14.dp)
        )

        MapBottomOverlay(
            uiState = uiState,
            onRouteClick = viewModel::toggleRoute,
            onCloseSpot = { viewModel.selectSpot(null) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        AnimatedVisibility(
            visible = uiState.loading,
            modifier = Modifier.align(Alignment.Center)
        ) {
            IOSCard(cornerRadius = 22.dp, padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = WanLvGreen, strokeWidth = 3.dp)
                    Text("地图加载中", color = WanLvTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showScenicPicker) {
        ModalBottomSheet(
            onDismissRequest = { showScenicPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = WanLvSurface
        ) {
            ScenicPickerSheet(
                scenicAreas = uiState.scenicAreas,
                selected = uiState.selectedScenicArea,
                onSelect = {
                    showScenicPicker = false
                    viewModel.selectScenicArea(it)
                }
            )
        }
    }
}

@Composable
private fun MapLibreGuideMap(
    uiState: MapUiState,
    onSpotClick: (MapSpotDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember { createMapLibreView(context) }
    val currentSpots by rememberUpdatedState(uiState.mapInit?.spots.orEmpty())
    val currentOnSpotClick by rememberUpdatedState(onSpotClick)
    var centeredScenicAreaId by remember { mutableStateOf<Long?>(null) }
    var lastStyleJson by remember { mutableStateOf<String?>(null) }
    val styleJson = remember(uiState.mapInit, uiState.recommendedRoutes, uiState.visibleRouteIds, uiState.selectedSpot?.id) {
        buildMapStyleJson(uiState)
    }

    BindMapLibreLifecycle(mapView)
    BindMapClick(mapView, currentSpots, currentOnSpotClick)

    Box(modifier = modifier) {
    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.getMapAsync { map ->
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isCompassEnabled = false
                map.uiSettings.isRotateGesturesEnabled = true
                if (lastStyleJson != styleJson) {
                    // 业务图层也写入同一个 style，确保底图、路线、景点保持同一套 WebMercator 坐标体系。
                    map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
                        moveCameraToScenicArea(map, uiState.mapInit?.scenicArea, centeredScenicAreaId)
                        centeredScenicAreaId = uiState.mapInit?.scenicArea?.id ?: centeredScenicAreaId
                    }
                    lastStyleJson = styleJson
                } else {
                    moveCameraToScenicArea(map, uiState.mapInit?.scenicArea, centeredScenicAreaId)
                    centeredScenicAreaId = uiState.mapInit?.scenicArea?.id ?: centeredScenicAreaId
                }
            }
        }
    )
    }
}

@Composable
private fun BindMapLibreLifecycle(mapView: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun BindMapClick(
    mapView: MapView,
    spots: List<MapSpotDto>,
    onSpotClick: (MapSpotDto) -> Unit
) {
    DisposableEffect(mapView) {
        var mapRef: MapLibreMap? = null
        val listener = MapLibreMap.OnMapClickListener { latLng ->
            val nearestSpot = findNearestSpot(spots, latLng)
            if (nearestSpot != null) {
                onSpotClick(nearestSpot)
                true
            } else {
                false
            }
        }
        mapView.getMapAsync { map ->
            mapRef = map
            map.addOnMapClickListener(listener)
        }
        onDispose {
            mapRef?.removeOnMapClickListener(listener)
        }
    }
}

private fun createMapLibreView(context: Context): MapView {
    MapLibre.getInstance(context)
    return MapView(context).apply {
        onCreate(null)
    }
}

private fun moveCameraToScenicArea(map: MapLibreMap, area: MapScenicAreaDto?, centeredScenicAreaId: Long?) {
    if (area == null || centeredScenicAreaId == area.id) return
    val latitude = area.centerLatitude ?: DefaultLatitude
    val longitude = area.centerLongitude ?: DefaultLongitude
    map.setMinZoomPreference(area.minZoom ?: 4.0)
    map.setMaxZoomPreference(area.maxZoom ?: 20.0)
    val camera = CameraPosition.Builder()
        .target(LatLng(latitude, longitude))
        .zoom(area.defaultZoom ?: DefaultZoom)
        .build()
    map.moveCamera(CameraUpdateFactory.newCameraPosition(camera))
}

private fun buildMapStyleJson(uiState: MapUiState): String {
    val scenicArea = uiState.mapInit?.scenicArea
    val sources = JSONObject()
        .put(
            BaseSourceId,
            JSONObject()
                .put("type", "vector")
                .put("url", AppConfig.mapVectorSourceUrl)
                .put("attribution", AppConfig.mapTileAttribution)
        )
        .apply {
            imageSource(scenicArea?.mapBaseImageUrl, scenicArea?.mapBounds)?.let {
                put(MapBaseImageSourceId, it)
            }
        }
        .put(BoundsSourceId, featureCollectionSource(buildBoundsFeatureCollection(scenicArea?.mapBounds)))
        .put(GeoFeatureSourceId, featureCollectionSource(buildGeoFeatureCollection(uiState.mapInit?.geoFeatures.orEmpty())))
        .put(RouteSourceId, featureCollectionSource(buildRouteFeatureCollection(uiState.recommendedRoutes, uiState.visibleRouteIds)))
        .put(SpotSourceId, featureCollectionSource(buildSpotFeatureCollection(uiState.mapInit?.spots.orEmpty(), uiState.selectedSpot?.id)))

    val layers = JSONArray()
        .put(backgroundLayer("#EEF2F0"))
        .put(fillLayer("landcover", "landcover", "#D2EBC8", 0.78))
        .put(fillLayer("landuse", "landuse", "#E7EFD5", 0.66))
        .put(fillLayer("park", "park", "#CDEBC8", 0.82))
        .put(fillLayer("water", "water", "#B8DCF4", 0.9))
        .put(lineLayer("waterway", "waterway", "#90C8EC", 1.2, 3))
        .put(lineLayer("road-case", "transportation", "#D7DEE8", 6.0, 6))
        .put(lineLayer("road", "transportation", "#FFFFFF", 3.2, 6))
        .put(lineLayer("boundary", "boundary", "#AEB8C6", 1.0, 4, JSONArray().put(2).put(2)))
        .put(fillLayer("building", "building", "#DADFE6", 0.72, 13))
        .apply {
            if (!scenicArea?.mapBaseImageUrl.isNullOrBlank() && scenicArea?.mapBounds != null) {
                put(rasterLayer("scenic-base-image", MapBaseImageSourceId, 0.92))
            }
        }
        .put(fillLayerFromGeoJson("geo-feature-fill", GeoFeatureSourceId))
        .put(lineLayerFromGeoJson("geo-feature-line", GeoFeatureSourceId))
        .put(circleLayerFromGeoJson("geo-feature-point", GeoFeatureSourceId))
        .put(fillLayerFromGeoJson("bounds-fill", BoundsSourceId))
        .put(lineLayerFromGeoJson("bounds-line", BoundsSourceId, JSONArray().put(2).put(1.2)))
        .put(lineLayerFromGeoJson("route-line", RouteSourceId))
        .put(spotCircleLayer("spot-marker", SpotSourceId))
        .put(symbolLayer("spot-label", SpotSourceId))

    return JSONObject()
        .put("version", 8)
        .put("name", "WanLv Local Vector Map")
        .put("glyphs", GlyphsUrl)
        .put("sources", sources)
        .put("layers", layers)
        .toString()
}

private fun featureCollectionSource(collection: JSONObject): JSONObject =
    JSONObject()
        .put("type", "geojson")
        .put("data", collection)

private fun imageSource(imageUrl: String?, bounds: MapBoundsDto?): JSONObject? {
    if (imageUrl.isNullOrBlank() || bounds == null) return null
    return JSONObject()
        .put("type", "image")
        .put("url", imageUrl.androidReachableMapUrl())
        .put(
            "coordinates",
            JSONArray()
                .put(JSONArray().put(bounds.west).put(bounds.north))
                .put(JSONArray().put(bounds.east).put(bounds.north))
                .put(JSONArray().put(bounds.east).put(bounds.south))
                .put(JSONArray().put(bounds.west).put(bounds.south))
        )
}

private fun backgroundLayer(color: String): JSONObject =
    JSONObject()
        .put("id", "background")
        .put("type", "background")
        .put("paint", JSONObject().put("background-color", color))

private fun fillLayer(id: String, sourceLayer: String, color: String, opacity: Double, minZoom: Int? = null): JSONObject =
    JSONObject()
        .put("id", id)
        .put("type", "fill")
        .put("source", BaseSourceId)
        .put("source-layer", sourceLayer)
        .put("paint", JSONObject().put("fill-color", color).put("fill-opacity", opacity))
        .apply { minZoom?.let { put("minzoom", it) } }

private fun lineLayer(
    id: String,
    sourceLayer: String,
    color: String,
    width: Double,
    minZoom: Int? = null,
    dashArray: JSONArray? = null
): JSONObject =
    JSONObject()
        .put("id", id)
        .put("type", "line")
        .put("source", BaseSourceId)
        .put("source-layer", sourceLayer)
        .put(
            "paint",
            JSONObject()
                .put("line-color", color)
                .put("line-width", width)
                .apply { dashArray?.let { put("line-dasharray", it) } }
        )
        .apply { minZoom?.let { put("minzoom", it) } }

private fun rasterLayer(id: String, sourceId: String, opacity: Double): JSONObject =
    JSONObject()
        .put("id", id)
        .put("type", "raster")
        .put("source", sourceId)
        .put("paint", JSONObject().put("raster-opacity", opacity))

private fun fillLayerFromGeoJson(id: String, sourceId: String): JSONObject =
    JSONObject()
        .put("id", id)
        .put("type", "fill")
        .put("source", sourceId)
        .put(
            "filter",
            JSONArray()
                .put("all")
                .put(JSONArray().put("==").put(JSONArray().put("geometry-type")).put("Polygon"))
                .put(JSONArray().put("!=").put(JSONArray().put("get").put("featureType")).put("ROAD"))
        )
        .put(
            "paint",
            JSONObject()
                .put("fill-color", propertyExpression("fillColor"))
                .put("fill-opacity", propertyExpression("fillOpacity"))
        )

private fun lineLayerFromGeoJson(
    id: String,
    sourceId: String,
    dashArray: JSONArray? = null
): JSONObject =
    JSONObject()
        .put("id", id)
        .put("type", "line")
        .put("source", sourceId)
        .put(
            "filter",
            JSONArray()
                .put("any")
                .put(JSONArray().put("==").put(JSONArray().put("geometry-type")).put("LineString"))
                .put(JSONArray().put("==").put(JSONArray().put("geometry-type")).put("MultiLineString"))
                .put(JSONArray().put("==").put(JSONArray().put("geometry-type")).put("Polygon"))
                .put(JSONArray().put("==").put(JSONArray().put("geometry-type")).put("MultiPolygon"))
        )
        .put("layout", JSONObject().put("line-cap", "round").put("line-join", "round"))
        .put(
            "paint",
            JSONObject()
                .put("line-color", propertyExpression("lineColor"))
                .put("line-width", propertyExpression("lineWidth"))
                .put("line-opacity", propertyExpression("lineOpacity"))
                .apply { dashArray?.let { put("line-dasharray", it) } }
        )

private fun circleLayerFromGeoJson(id: String, sourceId: String): JSONObject =
    JSONObject()
        .put("id", id)
        .put("type", "circle")
        .put("source", sourceId)
        .put("filter", JSONArray().put("==").put(JSONArray().put("geometry-type")).put("Point"))
        .put(
            "paint",
            JSONObject()
                .put("circle-radius", 5)
                .put("circle-color", propertyExpression("pointColor"))
                .put("circle-opacity", 0.82)
                .put("circle-stroke-color", "#ffffff")
                .put("circle-stroke-width", 2)
        )

private fun spotCircleLayer(id: String, sourceId: String): JSONObject =
    JSONObject()
        .put("id", id)
        .put("type", "circle")
        .put("source", sourceId)
        .put(
            "paint",
            JSONObject()
                .put("circle-radius", propertyExpression("markerRadius"))
                .put("circle-color", propertyExpression("markerColor"))
                .put("circle-opacity", 0.95)
                .put("circle-stroke-color", "#FFFFFF")
                .put("circle-stroke-width", propertyExpression("markerStrokeWidth"))
        )

private fun symbolLayer(id: String, sourceId: String): JSONObject =
    JSONObject()
        .put("id", id)
        .put("type", "symbol")
        .put("source", sourceId)
        .put(
            "layout",
            JSONObject()
                .put("text-field", JSONArray().put("get").put("name"))
                .put("text-size", 12)
                .put("text-offset", JSONArray().put(0).put(0.45))
                .put("text-anchor", "top")
                .put("text-font", JSONArray().put("Noto Sans Regular"))
        )
        .put(
            "paint",
            JSONObject()
                .put("text-color", propertyExpression("textColor"))
                .put("text-halo-color", "#FFFFFF")
                .put("text-halo-width", 1.2)
        )

private fun propertyExpression(name: String): JSONArray =
    JSONArray().put("get").put(name)

private fun buildBoundsFeatureCollection(bounds: MapBoundsDto?): JSONObject {
    if (bounds == null) return emptyFeatureCollection()
    val coordinates = JSONArray()
        .put(JSONArray().put(bounds.west).put(bounds.south))
        .put(JSONArray().put(bounds.east).put(bounds.south))
        .put(JSONArray().put(bounds.east).put(bounds.north))
        .put(JSONArray().put(bounds.west).put(bounds.north))
        .put(JSONArray().put(bounds.west).put(bounds.south))
    val geometry = JSONObject()
        .put("type", "Polygon")
        .put("coordinates", JSONArray().put(coordinates))
    val properties = JSONObject()
        .put("fillColor", MapColorRules.ScenicBoundsFill)
        .put("fillOpacity", 0.08)
        .put("lineColor", MapColorRules.ScenicBoundsLine)
        .put("lineWidth", 3)
        .put("lineOpacity", 0.95)
    return featureCollection(JSONArray().put(feature(geometry, properties)))
}

private fun buildSpotFeatureCollection(spots: List<MapSpotDto>, selectedSpotId: Long?): JSONObject {
    val features = JSONArray()
    spots.filter { it.hasValidCoordinate }.forEach { spot ->
        val geometry = JSONObject()
            .put("type", "Point")
            .put("coordinates", JSONArray().put(spot.longitude).put(spot.latitude))
        val properties = JSONObject()
            // 景点点位放进 MapLibre 原生图层，拖拽时和底图同帧渲染，避免 Compose 叠层追随相机产生晃动。
            .put("id", spot.id)
            .put("name", spot.spotName)
            .put("type", spot.poiType ?: "spot")
            .put("textColor", MapColorRules.SpotText)
            .put("markerColor", MapColorRules.spotMarkerColor(spot.iconType))
            .put("markerRadius", if (spot.id == selectedSpotId) 10 else 7)
            .put("markerStrokeWidth", if (spot.id == selectedSpotId) 3 else 2)
        features.put(feature(geometry, properties))
    }
    return featureCollection(features)
}

private fun buildRouteFeatureCollection(routes: List<MapRouteDto>, visibleRouteIds: Set<String>): JSONObject {
    val features = JSONArray()
    routes.filter { visibleRouteIds.contains(it.id) }.forEach { route ->
        parseGeoJsonFeatures(route.geojson).forEach { item ->
            val properties = item.optJSONObject("properties") ?: JSONObject()
            properties.put("routeId", route.id)
            properties.put("name", route.routeName)
            properties.put("lineColor", MapColorRules.routeColor(route))
            properties.put("lineWidth", 4)
            properties.put("lineOpacity", 0.92)
            item.put("properties", properties)
            features.put(item)
        }
    }
    return featureCollection(features)
}

private fun buildGeoFeatureCollection(features: List<MapGeoFeatureDto>): JSONObject {
    val result = JSONArray()
    features.forEach { geoFeature ->
        parseGeoJsonFeatures(geoFeature.geojson).forEach { item ->
            val properties = item.optJSONObject("properties") ?: JSONObject()
            val featureType = geoFeature.featureType.orEmpty().uppercase()
            val featureSubType = geoFeature.featureSubType.orEmpty().uppercase()
            properties.put("name", geoFeature.featureName)
            properties.put("featureType", featureType)
            properties.put("featureSubType", featureSubType)
            properties.put("fillColor", MapColorRules.featureFillColor(featureType))
            properties.put("fillOpacity", 0.14)
            properties.put("lineColor", MapColorRules.featureLineColor(featureType, featureSubType))
            properties.put("lineWidth", 2)
            properties.put("lineOpacity", if (featureType == "ROAD") 0.68 else 0.82)
            properties.put("pointColor", MapColorRules.featurePointColor(featureType))
            item.put("properties", properties)
            result.put(item)
        }
    }
    return featureCollection(result)
}

private fun parseGeoJsonFeatures(geojson: String?): List<JSONObject> {
    if (geojson.isNullOrBlank()) return emptyList()
    return runCatching {
        when (val value = JSONTokener(geojson).nextValue()) {
            is JSONObject -> normalizeGeoJson(value)
            else -> emptyList()
        }
    }.getOrDefault(emptyList())
}

private fun normalizeGeoJson(json: JSONObject): List<JSONObject> =
    when (json.optString("type")) {
        "FeatureCollection" -> json.optJSONArray("features").asJsonObjects()
        "Feature" -> listOf(json)
        "GeometryCollection" -> json.optJSONArray("geometries").asJsonObjects().map { geometry ->
            feature(geometry, JSONObject())
        }
        else -> listOf(feature(json, JSONObject()))
    }

private fun JSONArray?.asJsonObjects(): List<JSONObject> {
    if (this == null) return emptyList()
    return List(length()) { index -> optJSONObject(index) }.filterNotNull()
}

private fun feature(geometry: JSONObject, properties: JSONObject): JSONObject =
    JSONObject()
        .put("type", "Feature")
        .put("properties", properties)
        .put("geometry", geometry)

private fun featureCollection(features: JSONArray): JSONObject =
    JSONObject()
        .put("type", "FeatureCollection")
        .put("features", features)

private fun emptyFeatureCollection(): JSONObject = featureCollection(JSONArray())

private fun findNearestSpot(spots: List<MapSpotDto>, latLng: LatLng): MapSpotDto? {
    val latitude = latLng.latitude
    val longitude = latLng.longitude
    return spots
        .filter { it.hasValidCoordinate }
        .minByOrNull { spot ->
            val latDelta = (spot.latitude ?: latitude) - latitude
            val lngDelta = ((spot.longitude ?: longitude) - longitude) * cos(Math.toRadians(latitude))
            sqrt(latDelta.pow(2) + lngDelta.pow(2))
        }
        ?.takeIf { spot ->
            val latDelta = (spot.latitude ?: latitude) - latitude
            val lngDelta = ((spot.longitude ?: longitude) - longitude) * cos(Math.toRadians(latitude))
            sqrt(latDelta.pow(2) + lngDelta.pow(2)) < 0.0016
        }
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    val items = listOf(
        "步行道路" to Color(0xFFCBD5E1),
        "车行道路" to Color(0xFFFDBA74),
        "游览步道" to Color(0xFF86EFAC),
        "服务通道" to Color(0xFF93C5FD)
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = WanLvSurface.copy(alpha = 0.84f),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items.forEach { (label, color) ->
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(width = 18.dp, height = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                    Text(label, color = WanLvTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MapToolDock(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = WanLvSurface.copy(alpha = 0.86f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MapIconButton(Icons.Rounded.Settings, "设置", onOpenSettings)
            MapIconButton(Icons.Rounded.Layers, "图层") {}
            MapIconButton(Icons.Rounded.Route, "路线") {}
            MapIconButton(Icons.Rounded.MyLocation, "定位") {}
        }
    }
}

@Composable
private fun MapIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = WanLvTextPrimary, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun MapBottomOverlay(
    uiState: MapUiState,
    onRouteClick: (MapRouteDto) -> Unit,
    onCloseSpot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        uiState.selectedSpot?.let { spot ->
            SpotInfoCard(spot = spot, onClose = onCloseSpot)
        }
        RouteStrip(
            routes = uiState.recommendedRoutes,
            visibleRouteIds = uiState.visibleRouteIds,
            message = uiState.message,
            onRouteClick = onRouteClick
        )
    }
}

@Composable
private fun SpotInfoCard(spot: MapSpotDto, onClose: () -> Unit) {
    IOSCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, padding = PaddingValues(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(WanLvGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text("景", color = WanLvGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(spot.spotName, color = WanLvTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    spot.description ?: spot.poiType ?: "暂无景点介绍",
                    color = WanLvTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2
                )
            }
            Text(
                text = "关闭",
                color = WanLvGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onClose)
            )
        }
    }
}

@Composable
private fun RouteStrip(
    routes: List<MapRouteDto>,
    visibleRouteIds: Set<String>,
    message: String?,
    onRouteClick: (MapRouteDto) -> Unit
) {
    if (routes.isEmpty() && message.isNullOrBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!message.isNullOrBlank()) {
            Surface(shape = RoundedCornerShape(18.dp), color = WanLvSurface.copy(alpha = 0.88f), shadowElevation = 5.dp) {
                Text(
                    text = message,
                    color = WanLvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(routes, key = { it.id }) { route ->
                RouteCard(
                    route = route,
                    selected = visibleRouteIds.contains(route.id),
                    onClick = { onRouteClick(route) }
                )
            }
        }
    }
}

@Composable
private fun RouteCard(route: MapRouteDto, selected: Boolean, onClick: () -> Unit) {
    val accent = if (route.isAgentRoute || route.routeType.equals("agent_custom", ignoreCase = true)) {
        Color(0xFF7C3AED)
    } else {
        Color(0xFFF97316)
    }
    Surface(
        modifier = Modifier
            .size(width = 214.dp, height = 94.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = WanLvSurface.copy(alpha = 0.93f),
        shadowElevation = if (selected) 10.dp else 5.dp,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent.copy(alpha = 0.72f) else WanLvDivider
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (route.hasGeoJson) accent else WanLvTextSecondary.copy(alpha = 0.45f))
                )
                Text(route.routeName, color = WanLvTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Text(
                text = route.recommendedReason ?: route.description ?: if (route.hasGeoJson) "点击切换路线轨迹" else "暂未配置轨迹",
                color = WanLvTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ScenicPickerSheet(
    scenicAreas: List<ScenicAreaDto>,
    selected: ScenicAreaDto?,
    onSelect: (ScenicAreaDto) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("选择景区", color = WanLvTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
        if (scenicAreas.isEmpty()) {
            Text("暂无可切换景区", color = WanLvTextSecondary, fontSize = 14.sp)
        } else {
            LazyColumn(
                modifier = Modifier.height(360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scenicAreas, key = { it.id }) { area ->
                    ScenicAreaRow(
                        area = area,
                        selected = selected?.id == area.id,
                        onClick = { onSelect(area) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScenicAreaRow(area: ScenicAreaDto, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) WanLvGreen.copy(alpha = 0.12f) else WanLvBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected) WanLvGreen else WanLvGreenLight.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text("游", color = if (selected) WanLvSurface else WanLvGreen, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(area.scenicName, color = WanLvTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(scenicAreaLocation(area), color = WanLvTextSecondary, fontSize = 12.sp, maxLines = 1)
            }
            if (selected) {
                Text("当前", color = WanLvGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun scenicAreaLocation(area: ScenicAreaDto): String =
    listOfNotNull(area.province, area.city, area.district)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { area.address ?: "景区地图" }

private object MapColorRules {
    const val ScenicBoundsFill = "#2563eb"
    const val ScenicBoundsLine = "#dc2626"
    const val SpotMarker = "#0f766e"
    const val TempMarker = "#dc2626"
    const val SpotText = "#0f172a"

    fun featureFillColor(featureType: String): String =
        when (featureType) {
            "BOUNDARY" -> "#2563eb"
            "RESTRICTED" -> "#ef4444"
            "ZONE" -> "#0f766e"
            "ENTRANCE_AREA" -> "#f59e0b"
            "ROAD" -> "#86efac"
            else -> "#475569"
        }

    fun featureLineColor(featureType: String, featureSubType: String): String =
        if (featureType == "ROAD") {
            when (featureSubType) {
                "WALK" -> "#cbd5e1"
                "DRIVE" -> "#fdba74"
                "TOUR" -> "#86efac"
                "SERVICE" -> "#93c5fd"
                else -> "#86efac"
            }
        } else {
            when (featureType) {
                "BOUNDARY" -> "#1d4ed8"
                "RESTRICTED" -> "#dc2626"
                "ZONE" -> "#0f766e"
                "ENTRANCE_AREA" -> "#d97706"
                else -> "#334155"
            }
        }

    fun featurePointColor(featureType: String): String =
        when (featureType) {
            "BOUNDARY" -> "#1d4ed8"
            "RESTRICTED" -> "#dc2626"
            "ZONE" -> "#0f766e"
            "ENTRANCE_AREA" -> "#d97706"
            "ROAD" -> "#86efac"
            else -> "#334155"
        }

    fun spotMarkerColor(iconType: String?): String =
        when (iconType.orEmpty().uppercase()) {
            "TRAFFIC" -> "#2563eb"
            "LOCATION" -> "#0f766e"
            "ENTRANCE" -> "#d97706"
            "SERVICE" -> "#0891b2"
            "DINING" -> "#f97316"
            "BUILDING" -> "#64748b"
            "PARKING" -> "#475569"
            "SCENIC_SPOT" -> "#16a34a"
            "SERVICE_CENTER" -> "#0284c7"
            "RESTROOM" -> "#7c3aed"
            "RESTAURANT" -> "#dc2626"
            "SHOP" -> "#db2777"
            "TRANSPORT" -> "#0ea5e9"
            else -> SpotMarker
        }

    fun routeColor(route: MapRouteDto): String =
        if (route.isAgentRoute || route.routeType.equals("agent_custom", ignoreCase = true)) {
            "#7c3aed"
        } else {
            "#f97316"
        }
}

private fun String.androidReachableMapUrl(): String =
    replace("://localhost", "://10.0.2.2")
        .replace("://127.0.0.1", "://10.0.2.2")

private const val BaseSourceId = "china"
private const val MapBaseImageSourceId = "wanlv-map-base-image"
private const val BoundsSourceId = "wanlv-bounds"
private const val GeoFeatureSourceId = "wanlv-geo-features"
private const val RouteSourceId = "wanlv-routes"
private const val SpotSourceId = "wanlv-spots"
private const val GlyphsUrl = "https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf"
private const val DefaultLatitude = 30.236581
private const val DefaultLongitude = 120.155161
private const val DefaultZoom = 14.0
