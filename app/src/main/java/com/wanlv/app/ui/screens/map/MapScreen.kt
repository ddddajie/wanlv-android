package com.wanlv.app.ui.screens.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wanlv.app.config.AppConfig
import com.wanlv.app.network.AuthSession
import com.wanlv.app.pojo.dto.MapBoundsDto
import com.wanlv.app.pojo.dto.MapGeoFeatureDto
import com.wanlv.app.pojo.dto.MapInitDto
import com.wanlv.app.pojo.dto.MapRouteDto
import com.wanlv.app.pojo.dto.MapScenicAreaDto
import com.wanlv.app.pojo.dto.MapSpotDto
import com.wanlv.app.pojo.dto.ScenicAreaDto
import com.wanlv.app.ui.components.FloatingBottomBarAvoidance
import com.wanlv.app.ui.components.IOSCard
import com.wanlv.app.ui.theme.WanLvBackground
import com.wanlv.app.ui.theme.WanLvDivider
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvGreenLight
import com.wanlv.app.ui.theme.WanLvSurface
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary
import com.wanlv.app.viewmodel.MapDigitalHumanUiState
import com.wanlv.app.viewmodel.MapDigitalHumanViewModel
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
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    digitalHumanViewModel: MapDigitalHumanViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val digitalHumanState = digitalHumanViewModel.uiState
    val isLoggedIn = AuthSession.userId != null && !AuthSession.token.isNullOrBlank()
    var showScenicPicker by remember { mutableStateOf(false) }
    var showScenicDetail by remember { mutableStateOf(false) }
    var showRoutePanel by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadMap()
    }

    LaunchedEffect(uiState.selectedScenicArea?.id) {
        showScenicDetail = false
        showRoutePanel = false
    }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) digitalHumanViewModel.close()
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
            showAiChat = isLoggedIn,
            onSwitchMap = {
                showScenicPicker = true
                showScenicDetail = false
                showRoutePanel = false
            },
            onOpenAiChat = {
                showScenicDetail = false
                showRoutePanel = false
                digitalHumanViewModel.toggleVisible()
            },
            onOpenScenicDetail = {
                showScenicDetail = !showScenicDetail
                showRoutePanel = false
                if (showScenicDetail) digitalHumanViewModel.close()
            },
            onOpenRoutePanel = {
                showRoutePanel = !showRoutePanel
                showScenicDetail = false
                if (showRoutePanel) digitalHumanViewModel.close()
            },
            onLocateSelf = {},
            onRefresh = {
                showScenicPicker = false
                showScenicDetail = false
                showRoutePanel = false
                digitalHumanViewModel.close()
                viewModel.refreshMap()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 14.dp)
        )

        AnimatedVisibility(
            visible = showScenicDetail,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 14.dp, start = 14.dp, end = 78.dp)
        ) {
            ScenicDetailPanel(
                area = uiState.mapInit?.scenicArea,
                selectedArea = uiState.selectedScenicArea,
                spotCount = uiState.mapInit?.spots.orEmpty().size,
                routeCount = uiState.recommendedRoutes.size,
                onClose = { showScenicDetail = false }
            )
        }

        AnimatedVisibility(
            visible = digitalHumanState.visible,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 58.dp, start = 18.dp)
        ) {
            DigitalHumanWindow(
                state = digitalHumanState,
                previewBitmap = digitalHumanViewModel.previewBitmap,
                onClose = digitalHumanViewModel::close
            )
        }

        AnimatedVisibility(
            visible = uiState.selectedSpot != null && !showRoutePanel,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            MapBottomOverlay(
                uiState = uiState,
                onCloseSpot = { viewModel.selectSpot(null) }
            )
        }

        AnimatedVisibility(
            visible = showRoutePanel,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 14.dp)
                .padding(bottom = FloatingBottomBarAvoidance)
        ) {
            RouteRecommendationPanel(
                routes = uiState.recommendedRoutes,
                visibleRouteIds = uiState.visibleRouteIds,
                message = uiState.message,
                onRouteClick = viewModel::toggleRoute,
                onClose = { showRoutePanel = false }
            )
        }

        AnimatedVisibility(
            visible = digitalHumanState.visible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 22.dp)
                .padding(bottom = FloatingBottomBarAvoidance + 12.dp)
        ) {
            DigitalHumanInputBar(
                value = digitalHumanState.input,
                sending = digitalHumanState.sending || digitalHumanState.connecting,
                onValueChange = digitalHumanViewModel::updateInput,
                onSend = { digitalHumanViewModel.sendQuestion(uiState.selectedScenicArea?.id) }
            )
        }

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
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            dragHandle = null
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
                        registerSpotIconImages(context, style)
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
    val latestSpots by rememberUpdatedState(spots)
    val latestOnSpotClick by rememberUpdatedState(onSpotClick)
    DisposableEffect(mapView) {
        var mapRef: MapLibreMap? = null
        val listener = MapLibreMap.OnMapClickListener { latLng ->
            val map = mapRef
            val currentSpots = latestSpots
            // 重点：监听只安装一次，但点击时必须读取最新点位；否则首次空列表会导致图标永远点不开。
            val clickedSpot = map?.findClickedSpot(currentSpots, latLng)
                ?: map?.findNearestSpotByScreenDistance(currentSpots, latLng)
                ?: findNearestSpot(currentSpots, latLng)
            if (clickedSpot != null) {
                latestOnSpotClick(clickedSpot)
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

private fun MapLibreMap.findClickedSpot(spots: List<MapSpotDto>, latLng: LatLng): MapSpotDto? {
    val point = projection.toScreenLocation(latLng)
    val queryArea = RectF(point.x - 24f, point.y - 24f, point.x + 24f, point.y + 24f)
    val features = queryRenderedFeatures(queryArea, "spot-icon", "spot-label")
    val id = features
        .asSequence()
        .mapNotNull { feature -> feature.getNumberProperty("id")?.toLong() }
        .firstOrNull()
    return id?.let { spotId -> spots.firstOrNull { it.id == spotId } }
}

private fun MapLibreMap.findNearestSpotByScreenDistance(spots: List<MapSpotDto>, latLng: LatLng): MapSpotDto? {
    val clickedPoint = projection.toScreenLocation(latLng)
    return spots
        .asSequence()
        .filter { it.hasValidCoordinate }
        .map { spot ->
            val spotPoint = projection.toScreenLocation(LatLng(spot.latitude ?: 0.0, spot.longitude ?: 0.0))
            val dx = spotPoint.x - clickedPoint.x
            val dy = spotPoint.y - clickedPoint.y
            spot to dx * dx + dy * dy
        }
        .minByOrNull { it.second }
        ?.takeIf { (_, distanceSquared) -> distanceSquared <= 42f * 42f }
        ?.first
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
        .put(spotIconLayer("spot-icon", SpotSourceId))
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

private fun registerSpotIconImages(context: Context, style: Style) {
    val density = context.resources.displayMetrics.density
    val images = HashMap<String, Bitmap>()
    MapSpotIconRules.allStyles.forEach { icon ->
        images[icon.imageId] = createSpotIconBitmap(icon, density)
    }
    // 重点：景点标注使用 MapLibre 原生 bitmap icon，避免文字占位图标显得粗糙。
    style.addImages(images)
}

private fun createSpotIconBitmap(icon: MapSpotIconStyle, density: Float): Bitmap {
    val size = (30f * density).toInt().coerceAtLeast(30)
    val padding = 5f * density
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val color = AndroidColor.parseColor(icon.color)
    val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.95f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        this.color = color
    }
    val iconFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    val bounds = RectF(
        padding,
        padding,
        size - padding,
        size - padding
    )
    drawSpotIcon(canvas, icon.type, bounds, iconPaint, iconFillPaint)
    return bitmap
}

private fun drawSpotIcon(canvas: Canvas, type: String, bounds: RectF, paint: Paint, fillPaint: Paint) {
    when (type) {
        "TRAFFIC" -> drawTrafficIcon(canvas, bounds, paint)
        "LOCATION" -> drawLocationIcon(canvas, bounds, paint)
        "ENTRANCE" -> drawEntranceIcon(canvas, bounds, paint)
        "SERVICE" -> drawServiceIcon(canvas, bounds, paint)
        "DINING" -> drawDiningIcon(canvas, bounds, paint)
        "BUILDING" -> drawBuildingIcon(canvas, bounds, paint)
        "PARKING" -> drawParkingIcon(canvas, bounds, paint)
        "SCENIC_SPOT" -> drawScenicIcon(canvas, bounds, paint)
        "SERVICE_CENTER" -> drawServiceCenterIcon(canvas, bounds, paint, fillPaint)
        "RESTROOM" -> drawRestroomIcon(canvas, bounds, paint, fillPaint)
        "RESTAURANT" -> drawRestaurantIcon(canvas, bounds, paint)
        "SHOP" -> drawShopIcon(canvas, bounds, paint)
        "TRANSPORT" -> drawTransportIcon(canvas, bounds, paint, fillPaint)
        else -> drawScenicIcon(canvas, bounds, paint)
    }
}

private fun drawTrafficIcon(canvas: Canvas, r: RectF, paint: Paint) {
    val poleX = r.left + r.width() * 0.36f
    canvas.drawLine(poleX, r.top + r.height() * 0.2f, poleX, r.bottom, paint)
    canvas.drawRoundRect(RectF(poleX, r.top + r.height() * 0.2f, r.right, r.top + r.height() * 0.48f), 3f, 3f, paint)
    canvas.drawLine(poleX, r.top + r.height() * 0.62f, r.right - r.width() * 0.1f, r.top + r.height() * 0.62f, paint)
    canvas.drawLine(r.right - r.width() * 0.1f, r.top + r.height() * 0.62f, r.right - r.width() * 0.25f, r.top + r.height() * 0.5f, paint)
}

private fun drawLocationIcon(canvas: Canvas, r: RectF, paint: Paint) {
    val path = Path().apply {
        moveTo(r.centerX(), r.bottom)
        cubicTo(r.left, r.top + r.height() * 0.58f, r.left + r.width() * 0.08f, r.top, r.centerX(), r.top)
        cubicTo(r.right - r.width() * 0.08f, r.top, r.right, r.top + r.height() * 0.58f, r.centerX(), r.bottom)
    }
    canvas.drawPath(path, paint)
    canvas.drawCircle(r.centerX(), r.top + r.height() * 0.38f, r.width() * 0.14f, paint)
}

private fun drawEntranceIcon(canvas: Canvas, r: RectF, paint: Paint) {
    val door = RectF(r.left + r.width() * 0.18f, r.top + r.height() * 0.16f, r.right - r.width() * 0.18f, r.bottom)
    canvas.drawRoundRect(door, 3f, 3f, paint)
    canvas.drawLine(r.centerX(), door.top + door.height() * 0.12f, r.centerX(), door.bottom, paint)
    canvas.drawLine(r.left, r.centerY(), r.centerX() + r.width() * 0.16f, r.centerY(), paint)
    canvas.drawLine(r.centerX() + r.width() * 0.16f, r.centerY(), r.centerX(), r.centerY() - r.height() * 0.13f, paint)
    canvas.drawLine(r.centerX() + r.width() * 0.16f, r.centerY(), r.centerX(), r.centerY() + r.height() * 0.13f, paint)
}

private fun drawServiceIcon(canvas: Canvas, r: RectF, paint: Paint) {
    canvas.drawArc(RectF(r.left, r.top + r.height() * 0.12f, r.right, r.bottom), 200f, 140f, false, paint)
    canvas.drawRoundRect(RectF(r.left, r.centerY(), r.left + r.width() * 0.18f, r.bottom - r.height() * 0.2f), 3f, 3f, paint)
    canvas.drawRoundRect(RectF(r.right - r.width() * 0.18f, r.centerY(), r.right, r.bottom - r.height() * 0.2f), 3f, 3f, paint)
    canvas.drawLine(r.right - r.width() * 0.1f, r.bottom - r.height() * 0.22f, r.centerX() + r.width() * 0.08f, r.bottom - r.height() * 0.08f, paint)
}

private fun drawDiningIcon(canvas: Canvas, r: RectF, paint: Paint) {
    val forkX = r.left + r.width() * 0.28f
    canvas.drawLine(forkX, r.top, forkX, r.bottom, paint)
    canvas.drawLine(forkX - r.width() * 0.12f, r.top, forkX - r.width() * 0.12f, r.top + r.height() * 0.32f, paint)
    canvas.drawLine(forkX, r.top, forkX, r.top + r.height() * 0.34f, paint)
    canvas.drawLine(forkX + r.width() * 0.12f, r.top, forkX + r.width() * 0.12f, r.top + r.height() * 0.32f, paint)
    val spoonX = r.right - r.width() * 0.25f
    canvas.drawOval(RectF(spoonX - r.width() * 0.13f, r.top, spoonX + r.width() * 0.13f, r.top + r.height() * 0.32f), paint)
    canvas.drawLine(spoonX, r.top + r.height() * 0.32f, spoonX, r.bottom, paint)
}

private fun drawBuildingIcon(canvas: Canvas, r: RectF, paint: Paint) {
    val path = Path().apply {
        moveTo(r.left, r.centerY() - r.height() * 0.05f)
        lineTo(r.centerX(), r.top)
        lineTo(r.right, r.centerY() - r.height() * 0.05f)
        lineTo(r.right, r.bottom)
        lineTo(r.left, r.bottom)
        close()
    }
    canvas.drawPath(path, paint)
    canvas.drawLine(r.centerX(), r.bottom, r.centerX(), r.centerY() + r.height() * 0.1f, paint)
}

private fun drawParkingIcon(canvas: Canvas, r: RectF, paint: Paint) {
    val car = RectF(r.left + r.width() * 0.08f, r.centerY() - r.height() * 0.05f, r.right - r.width() * 0.08f, r.bottom - r.height() * 0.18f)
    val roof = Path().apply {
        moveTo(r.left + r.width() * 0.25f, car.top)
        lineTo(r.left + r.width() * 0.38f, r.top + r.height() * 0.22f)
        lineTo(r.right - r.width() * 0.32f, r.top + r.height() * 0.22f)
        lineTo(r.right - r.width() * 0.18f, car.top)
    }
    canvas.drawPath(roof, paint)
    canvas.drawRoundRect(car, 4f, 4f, paint)
    canvas.drawCircle(r.left + r.width() * 0.28f, r.bottom - r.height() * 0.15f, r.width() * 0.08f, paint)
    canvas.drawCircle(r.right - r.width() * 0.28f, r.bottom - r.height() * 0.15f, r.width() * 0.08f, paint)
}

private fun drawScenicIcon(canvas: Canvas, r: RectF, paint: Paint) {
    val path = Path().apply {
        moveTo(r.left, r.bottom)
        lineTo(r.left + r.width() * 0.34f, r.centerY())
        lineTo(r.left + r.width() * 0.48f, r.centerY() + r.height() * 0.16f)
        lineTo(r.right - r.width() * 0.18f, r.top + r.height() * 0.18f)
        lineTo(r.right, r.bottom)
    }
    canvas.drawPath(path, paint)
    canvas.drawCircle(r.right - r.width() * 0.14f, r.top + r.height() * 0.16f, r.width() * 0.09f, paint)
}

private fun drawServiceCenterIcon(canvas: Canvas, r: RectF, paint: Paint, fillPaint: Paint) {
    canvas.drawCircle(r.centerX(), r.top + r.height() * 0.22f, r.width() * 0.16f, paint)
    canvas.drawRoundRect(RectF(r.left + r.width() * 0.2f, r.centerY(), r.right - r.width() * 0.2f, r.bottom), 8f, 8f, paint)
    canvas.drawLine(r.right - r.width() * 0.05f, r.top, r.right - r.width() * 0.05f, r.bottom - r.height() * 0.2f, paint)
    canvas.drawCircle(r.right - r.width() * 0.05f, r.top + r.height() * 0.08f, r.width() * 0.04f, fillPaint)
}

private fun drawRestroomIcon(canvas: Canvas, r: RectF, paint: Paint, fillPaint: Paint) {
    canvas.drawCircle(r.left + r.width() * 0.32f, r.top + r.height() * 0.16f, r.width() * 0.08f, fillPaint)
    canvas.drawCircle(r.right - r.width() * 0.32f, r.top + r.height() * 0.16f, r.width() * 0.08f, fillPaint)
    canvas.drawLine(r.left + r.width() * 0.32f, r.top + r.height() * 0.3f, r.left + r.width() * 0.32f, r.bottom, paint)
    val skirt = Path().apply {
        moveTo(r.right - r.width() * 0.32f, r.top + r.height() * 0.3f)
        lineTo(r.right - r.width() * 0.5f, r.bottom - r.height() * 0.16f)
        lineTo(r.right - r.width() * 0.14f, r.bottom - r.height() * 0.16f)
        close()
    }
    canvas.drawPath(skirt, paint)
}

private fun drawRestaurantIcon(canvas: Canvas, r: RectF, paint: Paint) {
    canvas.drawArc(RectF(r.left + r.width() * 0.1f, r.centerY() - r.height() * 0.1f, r.right - r.width() * 0.1f, r.bottom + r.height() * 0.35f), 190f, 160f, false, paint)
    canvas.drawLine(r.left + r.width() * 0.12f, r.bottom - r.height() * 0.12f, r.right - r.width() * 0.12f, r.bottom - r.height() * 0.12f, paint)
    canvas.drawLine(r.centerX(), r.top + r.height() * 0.18f, r.centerX(), r.top, paint)
    canvas.drawArc(RectF(r.left + r.width() * 0.1f, r.top + r.height() * 0.08f, r.right - r.width() * 0.1f, r.centerY() + r.height() * 0.2f), 200f, 140f, false, paint)
}

private fun drawShopIcon(canvas: Canvas, r: RectF, paint: Paint) {
    val bag = RectF(r.left + r.width() * 0.12f, r.top + r.height() * 0.28f, r.right - r.width() * 0.12f, r.bottom)
    canvas.drawRoundRect(bag, 4f, 4f, paint)
    canvas.drawArc(RectF(r.left + r.width() * 0.32f, r.top, r.right - r.width() * 0.32f, r.top + r.height() * 0.42f), 180f, 180f, false, paint)
}

private fun drawTransportIcon(canvas: Canvas, r: RectF, paint: Paint, fillPaint: Paint) {
    val bus = RectF(r.left + r.width() * 0.1f, r.top + r.height() * 0.1f, r.right - r.width() * 0.1f, r.bottom - r.height() * 0.18f)
    canvas.drawRoundRect(bus, 5f, 5f, paint)
    canvas.drawLine(bus.left + r.width() * 0.1f, r.centerY(), bus.right - r.width() * 0.1f, r.centerY(), paint)
    canvas.drawCircle(bus.left + r.width() * 0.18f, r.bottom - r.height() * 0.14f, r.width() * 0.07f, fillPaint)
    canvas.drawCircle(bus.right - r.width() * 0.18f, r.bottom - r.height() * 0.14f, r.width() * 0.07f, fillPaint)
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
                .put("circle-color", propertyExpression("markerBackground"))
                .put("circle-opacity", 0.96)
                .put("circle-stroke-color", propertyExpression("markerColor"))
                .put("circle-stroke-width", propertyExpression("markerStrokeWidth"))
        )

private fun spotIconLayer(id: String, sourceId: String): JSONObject =
    JSONObject()
        .put("id", id)
        .put("type", "symbol")
        .put("source", sourceId)
        .put(
            "layout",
            JSONObject()
                .put("icon-image", propertyExpression("iconImage"))
                .put("icon-size", propertyExpression("iconScale"))
                .put("icon-anchor", "center")
                .put("icon-allow-overlap", true)
                .put("icon-ignore-placement", true)
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
                .put("text-offset", JSONArray().put(0).put(0.95))
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
        val selected = spot.id == selectedSpotId
        val icon = MapSpotIconRules.resolve(spot.iconType, spot.poiType)
        val geometry = JSONObject()
            .put("type", "Point")
            .put("coordinates", JSONArray().put(spot.longitude).put(spot.latitude))
        val properties = JSONObject()
            // 景点点位放进 MapLibre 原生图层，拖拽时和底图同帧渲染，避免 Compose 叠层追随相机产生晃动。
            .put("id", spot.id)
            .put("name", spot.spotName)
            .put("type", spot.poiType ?: icon.type)
            .put("iconType", icon.type)
            .put("iconImage", icon.imageId)
            .put("textColor", MapColorRules.SpotText)
            .put("markerColor", icon.color)
            .put("markerBackground", if (selected) icon.selectedBackground else icon.background)
            .put("markerRadius", if (selected) 17 else 0)
            .put("markerStrokeWidth", if (selected) 2.2 else 0)
            .put("iconScale", if (selected) 1.08 else 1.0)
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
private fun MapToolDock(
    showAiChat: Boolean,
    onSwitchMap: () -> Unit,
    onOpenAiChat: () -> Unit,
    onOpenScenicDetail: () -> Unit,
    onOpenRoutePanel: () -> Unit,
    onLocateSelf: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LiquidGlassIconButton(Icons.Rounded.Map, "切换地图", onClick = onSwitchMap)
        // 重点：数字人问答依赖登录态，未登录时隐藏入口，避免用户进入后才收到接口失败。
        if (showAiChat) {
            LiquidGlassIconButton(Icons.Rounded.AutoAwesome, "AI对话", onClick = onOpenAiChat)
        }
        LiquidGlassIconButton(Icons.Rounded.Info, "景区详细信息", onClick = onOpenScenicDetail)
        LiquidGlassIconButton(Icons.Rounded.Route, "景区路线推荐", onClick = onOpenRoutePanel)
        LiquidGlassIconButton(Icons.Rounded.MyLocation, "定位自己位置", enabled = false, onClick = onLocateSelf)
        LiquidGlassIconButton(Icons.Rounded.Refresh, "刷新", onClick = onRefresh)
    }
}

@Composable
private fun LiquidGlassIconButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = CircleShape
    Box(
        modifier = Modifier
            // 重点：右侧地图小组件按钮整体缩小，减少对地图内容的遮挡。
            .size(40.dp)
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = Color(0xFF8A96A3).copy(alpha = 0.22f),
                spotColor = Color(0xFF5F6873).copy(alpha = 0.16f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (enabled) 0.82f else 0.58f),
                        WanLvSurface.copy(alpha = if (enabled) 0.64f else 0.42f),
                        Color.White.copy(alpha = if (enabled) 0.72f else 0.48f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.74f), shape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) WanLvTextPrimary else WanLvTextSecondary.copy(alpha = 0.48f),
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun DigitalHumanWindow(
    state: MapDigitalHumanUiState,
    previewBitmap: Bitmap,
    onClose: () -> Unit
) {
    val displayBitmap = state.videoFrame ?: previewBitmap
    Box(
        modifier = Modifier.size(width = 118.dp, height = 184.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = displayBitmap.asImageBitmap(),
            contentDescription = "数字导游",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        if (state.connecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = WanLvGreen,
                strokeWidth = 3.dp
            )
        }

        if (state.connecting || state.sending || (!state.connected && state.status.isNotBlank())) {
            Text(
                text = state.status,
                color = WanLvTextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.68f))
                    .border(1.dp, Color.White.copy(alpha = 0.76f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }

        SmallGlassCloseButton(
            label = "关闭数字导游",
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun DigitalHumanInputBar(
    value: String,
    sending: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val canSend = value.trim().isNotEmpty() && !sending
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 430.dp),
        cornerRadius = 26.dp,
        padding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = !sending,
                singleLine = true,
                placeholder = { Text("直接向数字人提问", color = WanLvTextSecondary.copy(alpha = 0.72f), fontSize = 13.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.46f))
                    .border(1.dp, Color.White.copy(alpha = 0.76f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = "语音输入", tint = WanLvTextSecondary, modifier = Modifier.size(19.dp))
            }

            Box(
                modifier = Modifier
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    // 重点：发送按钮保留液态玻璃底色，输入可发送时再用绿色强调状态。
                    .background(if (canSend) WanLvGreen.copy(alpha = 0.78f) else WanLvGreenLight.copy(alpha = 0.42f))
                    .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(23.dp))
                    .clickable(
                        enabled = canSend,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSend
                    )
                    .padding(horizontal = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                if (sending) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = WanLvGreen, strokeWidth = 2.dp)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "发送",
                            tint = if (canSend) Color.White else WanLvTextSecondary.copy(alpha = 0.55f),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            "发送",
                            color = if (canSend) Color.White else WanLvTextSecondary.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapBottomOverlay(
    uiState: MapUiState,
    onCloseSpot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spot = uiState.selectedSpot ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = FloatingBottomBarAvoidance),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SpotInfoCard(spot = spot, onClose = onCloseSpot)
    }
}

@Composable
private fun SpotInfoCard(spot: MapSpotDto, onClose: () -> Unit) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, padding = PaddingValues(16.dp)) {
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
                Text(
                    spot.spotName,
                    color = WanLvTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    spot.description ?: spot.poiType ?: "暂无景点介绍",
                    color = WanLvTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            SmallGlassCloseButton(label = "关闭景点信息", onClick = onClose)
        }
    }
}

@Composable
private fun ScenicDetailPanel(
    area: MapScenicAreaDto?,
    selectedArea: ScenicAreaDto?,
    spotCount: Int,
    routeCount: Int,
    onClose: () -> Unit
) {
    val scenicName = area?.scenicName ?: selectedArea?.scenicName ?: "景区信息"
    val description = area?.description ?: selectedArea?.description ?: "暂无景区简介"
    val location = scenicAreaLocation(area, selectedArea)
    val openingHours = area?.openingHours?.takeIf { it.isNotBlank() } ?: "暂无开放时间"
    val levelText = scenicLevelText(selectedArea?.scenicLevel)

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 310.dp),
        cornerRadius = 24.dp,
        padding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = WanLvGreen, modifier = Modifier.size(16.dp))
                    Text("景区信息", color = WanLvGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                SmallGlassCloseButton(label = "关闭景区信息", onClick = onClose)
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    scenicName,
                    color = WanLvTextPrimary,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (levelText != null) {
                    GlassChip(text = levelText, accent = WanLvGreen)
                }
                Text(
                    description,
                    color = WanLvTextPrimary.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScenicMetric(
                    label = "景点",
                    value = spotCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                ScenicMetric(
                    label = "路线",
                    value = routeCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.42f))
                    .border(1.dp, Color.White.copy(alpha = 0.68f), RoundedCornerShape(16.dp))
            ) {
                ScenicDetailRow(Icons.Rounded.LocationOn, "位置", location)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(WanLvDivider.copy(alpha = 0.62f))
                )
                ScenicDetailRow(Icons.Rounded.AccessTime, "开放时间", openingHours)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(WanLvDivider.copy(alpha = 0.54f))
            )

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("地图图例", color = WanLvTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    Text("道路图层显示重点", color = WanLvTextSecondary, fontSize = 10.sp)
                }
                MapLegendContent()
            }
        }
    }
}

@Composable
private fun ScenicMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val unit = if (label == "路线") "条" else "个"
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.48f))
            .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = WanLvTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = WanLvGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(unit, color = WanLvTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ScenicDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = WanLvGreen, modifier = Modifier.size(15.dp))
        Text(label, color = WanLvTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
            value,
            color = WanLvTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MapLegendContent() {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        RouteLegendItems.forEach { item ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 5.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(item.color)
                )
                Text(item.label, color = WanLvTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RouteRecommendationPanel(
    routes: List<MapRouteDto>,
    visibleRouteIds: Set<String>,
    message: String?,
    onRouteClick: (MapRouteDto) -> Unit,
    onClose: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp),
        cornerRadius = 24.dp,
        padding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("推荐路线", color = WanLvTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(
                        "点击路线可控制地图显示，快速切换不同游览方式",
                        color = WanLvTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
                SmallGlassCloseButton(label = "关闭推荐路线", onClick = onClose)
            }

            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    color = WanLvTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.42f))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }

            if (routes.isEmpty()) {
                Text("暂无推荐路线", color = WanLvTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(routes, key = { it.id }) { route ->
                        RouteRecommendationCard(
                            route = route,
                            selected = visibleRouteIds.contains(route.id),
                            onClick = { onRouteClick(route) }
                        )
                    }
                }
            }

            Text(
                "小提示：路线显示后，可在地图中拖拽查看路径亮点、主要景点节点与推荐游览顺序。",
                color = WanLvTextSecondary,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.34f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun RouteRecommendationCard(route: MapRouteDto, selected: Boolean, onClick: () -> Unit) {
    val accent = if (route.isAgentRoute || route.routeType.equals("agent_custom", ignoreCase = true)) {
        Color(0xFF7C3AED)
    } else {
        Color(0xFFF97316)
    }
    val shape = RoundedCornerShape(17.dp)
    val chips = listOfNotNull(
        route.durationMinutes?.let { durationLabel(it) },
        routeTypeLabel(route),
        route.suitableCrowd?.takeIf { it.isNotBlank() }
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = if (selected) 0.62f else 0.44f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent.copy(alpha = 0.76f) else Color.White.copy(alpha = 0.72f),
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (route.hasGeoJson) accent else WanLvTextSecondary.copy(alpha = 0.45f))
                    )
                    Text(
                        route.routeName,
                        color = WanLvTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    chips.take(3).forEach { chip ->
                        GlassChip(text = chip, accent = accent)
                    }
                }
            }
            RouteVisibleBadge(selected = selected, enabled = route.hasGeoJson, accent = accent)
        }
        Text(
            text = route.recommendedReason ?: route.description ?: if (route.hasGeoJson) "点击切换路线轨迹" else "暂未配置轨迹",
            color = WanLvTextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RouteVisibleBadge(selected: Boolean, enabled: Boolean, accent: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected && enabled) accent else WanLvTextSecondary.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Visibility,
            contentDescription = null,
            tint = if (selected && enabled) Color.White else WanLvTextSecondary,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = when {
                !enabled -> "无轨迹"
                selected -> "已显示"
                else -> "显示"
            },
            color = if (selected && enabled) Color.White else WanLvTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GlassChip(text: String, accent: Color) {
    Text(
        text = text,
        color = accent,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.64f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun SmallGlassCloseButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.52f))
            .border(1.dp, Color.White.copy(alpha = 0.76f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Close, contentDescription = label, tint = WanLvTextPrimary, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .shadow(
                elevation = 22.dp,
                shape = shape,
                ambientColor = Color(0xFF8FA0AE).copy(alpha = 0.20f),
                spotColor = Color(0xFF64727F).copy(alpha = 0.14f)
            )
            .clip(shape)
            // 重点：统一地图页卡片的液态玻璃质感，按钮和面板都保持半透明高光与白色描边。
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.88f),
                        WanLvSurface.copy(alpha = 0.66f),
                        WanLvGreenLight.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.74f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.78f), shape)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 42.dp, bottom = 8.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.46f))
        )
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

private data class RouteLegendItem(
    val label: String,
    val color: Color
)

private val RouteLegendItems = listOf(
    RouteLegendItem("步行道路", Color(0xFFCBD5E1)),
    RouteLegendItem("车行道路", Color(0xFFFDBA74)),
    RouteLegendItem("游览步道", Color(0xFF86EFAC)),
    RouteLegendItem("服务通道", Color(0xFF93C5FD))
)

private fun durationLabel(minutes: Int): String =
    if (minutes >= 60) {
        val hours = minutes / 60
        val rest = minutes % 60
        if (rest == 0) "${hours}小时" else "${hours}小时${rest}分钟"
    } else {
        "${minutes}分钟"
    }

private fun routeTypeLabel(route: MapRouteDto): String =
    when {
        route.isAgentRoute || route.routeType.equals("agent_custom", ignoreCase = true) -> "AI路线"
        route.routeType.equals("official", ignoreCase = true) || route.routeType.isNullOrBlank() -> "官方路线"
        else -> route.routeType.orEmpty()
    }

private fun scenicLevelText(level: String?): String? =
    level
        ?.takeIf { it.isNotBlank() }
        ?.let { if (it.contains("景区")) it else "$it 级旅游景区" }

@Composable
private fun ScenicPickerSheet(
    scenicAreas: List<ScenicAreaDto>,
    selected: ScenicAreaDto?,
    onSelect: (ScenicAreaDto) -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp)
            .padding(bottom = 16.dp),
        cornerRadius = 28.dp,
        padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 42.dp, height = 5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WanLvTextSecondary.copy(alpha = 0.22f))
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("选择景区", color = WanLvTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("切换后会重新载入对应景区地图、点位与路线", color = WanLvTextSecondary, fontSize = 12.sp)
            }

            if (scenicAreas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.44f))
                        .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 18.dp)
                ) {
                    Text("暂无可切换景区", color = WanLvTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
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
}

@Composable
private fun ScenicAreaRow(area: ScenicAreaDto, selected: Boolean, onClick: () -> Unit) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        cornerRadius = 18.dp,
        padding = PaddingValues(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    // 重点：当前景区增加绿色光圈，让选中状态在玻璃卡片里更明显。
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(WanLvGreen.copy(alpha = 0.14f))
                            .border(1.5.dp, WanLvGreen.copy(alpha = 0.54f), CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(if (selected) 34.dp else 36.dp)
                        .clip(CircleShape)
                        .background(if (selected) WanLvGreen else WanLvGreenLight.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("游", color = if (selected) WanLvSurface else WanLvGreen, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    area.scenicName,
                    color = WanLvTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    scenicAreaLocation(area),
                    color = WanLvTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Text(
                    "当前",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(WanLvGreen)
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }
    }
}

private fun scenicAreaLocation(area: ScenicAreaDto): String =
    scenicAreaLocation(area = null, selectedArea = area)

private fun scenicAreaLocation(area: MapScenicAreaDto?, selectedArea: ScenicAreaDto?): String {
    val province = area?.province?.takeIf { it.isNotBlank() } ?: selectedArea?.province
    val city = area?.city?.takeIf { it.isNotBlank() } ?: selectedArea?.city
    val district = area?.district?.takeIf { it.isNotBlank() } ?: selectedArea?.district
    val address = area?.address?.takeIf { it.isNotBlank() } ?: selectedArea?.address
    return listOfNotNull(province, city, district)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { address ?: "景区地图" }
}

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

private data class MapSpotIconStyle(
    val type: String,
    val imageId: String,
    val color: String,
    val background: String,
    val selectedBackground: String
)

private object MapSpotIconRules {
    private val styles = mapOf(
        "TRAFFIC" to MapSpotIconStyle("TRAFFIC", "spot-icon-traffic", "#3B82F6", "#EFF6FF", "#DBEAFE"),
        "LOCATION" to MapSpotIconStyle("LOCATION", "spot-icon-location", "#64748B", "#F8FAFC", "#E2E8F0"),
        "ENTRANCE" to MapSpotIconStyle("ENTRANCE", "spot-icon-entrance", "#F59E0B", "#FFFBEB", "#FEF3C7"),
        "SERVICE" to MapSpotIconStyle("SERVICE", "spot-icon-service", "#0EA5E9", "#F0F9FF", "#E0F2FE"),
        "DINING" to MapSpotIconStyle("DINING", "spot-icon-dining", "#F97316", "#FFF7ED", "#FFEDD5"),
        "BUILDING" to MapSpotIconStyle("BUILDING", "spot-icon-building", "#64748B", "#F8FAFC", "#E2E8F0"),
        "PARKING" to MapSpotIconStyle("PARKING", "spot-icon-parking", "#64748B", "#F8FAFC", "#E2E8F0"),
        "SCENIC_SPOT" to MapSpotIconStyle("SCENIC_SPOT", "spot-icon-scenic", "#0F766E", "#ECFDF5", "#CCFBF1"),
        "SERVICE_CENTER" to MapSpotIconStyle("SERVICE_CENTER", "spot-icon-service-center", "#0284C7", "#F0F9FF", "#E0F2FE"),
        "RESTROOM" to MapSpotIconStyle("RESTROOM", "spot-icon-restroom", "#7C3AED", "#F5F3FF", "#EDE9FE"),
        "RESTAURANT" to MapSpotIconStyle("RESTAURANT", "spot-icon-restaurant", "#DC2626", "#FEF2F2", "#FEE2E2"),
        "SHOP" to MapSpotIconStyle("SHOP", "spot-icon-shop", "#DB2777", "#FDF2F8", "#FCE7F3"),
        "TRANSPORT" to MapSpotIconStyle("TRANSPORT", "spot-icon-transport", "#0EA5E9", "#F0F9FF", "#E0F2FE")
    )

    val allStyles: Collection<MapSpotIconStyle> = styles.values

    fun resolve(iconType: String?, poiType: String?): MapSpotIconStyle {
        val type = normalize(iconType).ifBlank { normalize(poiType) }
        return styles[type] ?: styles.getValue("SCENIC_SPOT")
    }

    private fun normalize(value: String?): String {
        val raw = value.orEmpty().trim().uppercase()
        if (raw.isBlank()) return ""
        // 重点：兼容后台 13 类图标类型，也兜底处理常见中文/英文别名，避免新旧数据混用时又退回绿色圆点。
        return when (raw) {
            "TRAFFIC", "GUIDE", "SIGN", "SIGNAGE", "交通指引" -> "TRAFFIC"
            "LOCATION", "POSITION", "定位", "景点定位" -> "LOCATION"
            "ENTRANCE", "GATE", "ENTRY", "EXIT", "入口", "入口标识" -> "ENTRANCE"
            "SERVICE", "SERVICE_FACILITY", "FACILITY", "服务", "服务设施" -> "SERVICE"
            "DINING", "FOOD", "DRINK", "餐饮" -> "DINING"
            "BUILDING", "ARCHITECTURE", "HOUSE", "建筑" -> "BUILDING"
            "PARKING", "PARK", "停车" -> "PARKING"
            "SCENIC_SPOT", "SPOT", "SCENIC", "ATTRACTION", "景点" -> "SCENIC_SPOT"
            "SERVICE_CENTER", "VISITOR_CENTER", "TOURIST_CENTER", "游客中心" -> "SERVICE_CENTER"
            "RESTROOM", "TOILET", "WC", "BATHROOM", "卫生间" -> "RESTROOM"
            "RESTAURANT", "餐厅" -> "RESTAURANT"
            "SHOP", "STORE", "MALL", "商店" -> "SHOP"
            "TRANSPORT", "BUS", "SHUTTLE", "交通" -> "TRANSPORT"
            else -> raw
        }
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
