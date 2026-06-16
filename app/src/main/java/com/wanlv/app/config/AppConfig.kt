package com.wanlv.app.config

import android.content.Context
import com.wanlv.app.BuildConfig

object AppConfig {
    private const val CONFIG_FILE_NAME = "config.yml"
    private const val DEVELOPER_CONFIG_PREFS = "developer_config"
    private val values = mutableMapOf<String, String>()
    private val fallbackValues = mutableMapOf<String, String>()
    private var appContext: Context? = null

    private val editableConfigDefinitions = listOf(
        DeveloperConfigDefinition(
            key = "WANLV_API_BASE_URL",
            title = "后端服务地址",
            description = "登录、地图、预约、AI 问答等业务接口基础地址",
            required = true
        ),
        DeveloperConfigDefinition(
            key = "WANLV_DIGITAL_HUMAN_GUIDE_API_URL",
            title = "导游女数字人",
            description = "导游数字人的 WebRTC /offer 与播报接口地址",
            required = true
        ),
        DeveloperConfigDefinition(
            key = "WANLV_DIGITAL_HUMAN_SERVICE_API_URL",
            title = "客服男数字人",
            description = "客服数字人的 WebRTC /offer 与播报接口地址",
            required = true
        ),
        DeveloperConfigDefinition(
            key = "WANLV_MAP_STYLE_URL",
            title = "地图样式地址",
            description = "MapLibre style.json 地址，可留空使用应用内样式",
            required = false
        ),
        DeveloperConfigDefinition(
            key = "WANLV_MAP_VECTOR_SOURCE_URL",
            title = "地图矢量瓦片地址",
            description = "地图矢量瓦片源地址",
            required = true
        ),
        DeveloperConfigDefinition(
            key = "WANLV_MAP_RASTER_TILE_URL",
            title = "地图栅格瓦片地址",
            description = "栅格瓦片地址，可留空关闭栅格底图",
            required = false
        )
    )

    val apiBaseUrl: String
        get() = values["WANLV_API_BASE_URL"].orEmpty().ifBlank { BuildConfig.WANLV_API_BASE_URL }

    val digitalHumanGuideApiUrl: String
        get() = values["WANLV_DIGITAL_HUMAN_GUIDE_API_URL"].orEmpty()
            .ifBlank { BuildConfig.WANLV_DIGITAL_HUMAN_GUIDE_API_URL }
            .androidReachableUrl()

    val digitalHumanServiceApiUrl: String
        get() = values["WANLV_DIGITAL_HUMAN_SERVICE_API_URL"].orEmpty()
            .ifBlank { BuildConfig.WANLV_DIGITAL_HUMAN_SERVICE_API_URL }
            .androidReachableUrl()

    val debugUserId: Long?
        get() = values["WANLV_DEBUG_USER_ID"]?.toLongOrNull()

    val debugToken: String?
        get() = values["WANLV_DEBUG_TOKEN"]?.takeIf { it.isNotBlank() }

    val mapStyleUrl: String?
        get() = values["WANLV_MAP_STYLE_URL"]?.takeIf { it.isNotBlank() }?.androidReachableUrl()

    val mapVectorSourceUrl: String
        get() = values["WANLV_MAP_VECTOR_SOURCE_URL"].orEmpty()
            .ifBlank { BuildConfig.WANLV_MAP_VECTOR_SOURCE_URL }
            .androidReachableUrl()

    val mapRasterTileUrl: String?
        get() = values["WANLV_MAP_RASTER_TILE_URL"]?.takeIf { it.isNotBlank() }?.androidReachableUrl()

    val mapTileAttribution: String
        get() = values["WANLV_MAP_TILE_ATTRIBUTION"].orEmpty()
            .ifBlank { BuildConfig.WANLV_MAP_TILE_ATTRIBUTION }

    fun init(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        if (values.isNotEmpty()) return
        reload(applicationContext)
    }

    fun developerConfigItems(): List<DeveloperConfigItem> =
        editableConfigDefinitions.map { definition ->
            DeveloperConfigItem(
                key = definition.key,
                title = definition.title,
                description = definition.description,
                required = definition.required,
                value = values[definition.key].orEmpty(),
                defaultValue = fallbackValues[definition.key].orEmpty()
            )
        }

    fun saveDeveloperConfig(overrides: Map<String, String>) {
        val context = requireNotNull(appContext) { "AppConfig.init must be called before saving developer config." }
        val prefs = context.getSharedPreferences(DEVELOPER_CONFIG_PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editableConfigDefinitions.forEach { definition ->
            val value = overrides[definition.key].orEmpty().trim()
            editor.putString(definition.key, value)
            values[definition.key] = value
        }
        editor.apply()
    }

    fun resetDeveloperConfig() {
        val context = requireNotNull(appContext) { "AppConfig.init must be called before resetting developer config." }
        val prefs = context.getSharedPreferences(DEVELOPER_CONFIG_PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editableConfigDefinitions.forEach { definition -> editor.remove(definition.key) }
        editor.apply()
        reload(context)
    }

    private fun reload(context: Context) {
        values.clear()
        loadBuildConfigValues(values)
        runCatching {
            context.assets.open(CONFIG_FILE_NAME).bufferedReader(Charsets.UTF_8).useLines { lines ->
                val rawValues = lines
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") && (it.contains(":") || it.contains("=")) }
                    .associate {
                        val separator = if (it.contains(":")) ":" else "="
                        val key = it.substringBefore(separator).trim()
                        val value = it.substringAfter(separator).trim()
                        key to value
                    }
                rawValues.forEach { (key, value) -> values[key] = expand(value, rawValues, values) }
            }
        }
        fallbackValues.clear()
        fallbackValues.putAll(values)
        loadDeveloperValues(context)
    }

    private fun loadBuildConfigValues(target: MutableMap<String, String>) {
        target["WANLV_API_BASE_URL"] = BuildConfig.WANLV_API_BASE_URL
        target["WANLV_DIGITAL_HUMAN_GUIDE_API_URL"] = BuildConfig.WANLV_DIGITAL_HUMAN_GUIDE_API_URL
        target["WANLV_DIGITAL_HUMAN_SERVICE_API_URL"] = BuildConfig.WANLV_DIGITAL_HUMAN_SERVICE_API_URL
        target["WANLV_MAP_STYLE_URL"] = BuildConfig.WANLV_MAP_STYLE_URL
        target["WANLV_MAP_VECTOR_SOURCE_URL"] = BuildConfig.WANLV_MAP_VECTOR_SOURCE_URL
        target["WANLV_MAP_RASTER_TILE_URL"] = BuildConfig.WANLV_MAP_RASTER_TILE_URL
        target["WANLV_MAP_TILE_ATTRIBUTION"] = BuildConfig.WANLV_MAP_TILE_ATTRIBUTION
        // 构建期读取 config 写入 BuildConfig，运行期读取 assets/config 作为覆盖配置。
        target["WANLV_DEBUG_USER_ID"] = BuildConfig.WANLV_DEBUG_USER_ID
        target["WANLV_DEBUG_TOKEN"] = BuildConfig.WANLV_DEBUG_TOKEN
    }

    private fun loadDeveloperValues(context: Context) {
        val prefs = context.getSharedPreferences(DEVELOPER_CONFIG_PREFS, Context.MODE_PRIVATE)
        // 重点：开发者页面的配置优先级最高，保存后不用重新打包就能覆盖接口地址。
        editableConfigDefinitions.forEach { definition ->
            if (prefs.contains(definition.key)) {
                values[definition.key] = prefs.getString(definition.key, "").orEmpty()
            }
        }
    }

    private fun expand(value: String, source: Map<String, String>, fallback: Map<String, String>): String {
        var result = value
        val regex = Regex("""\$\{([A-Za-z0-9_]+)}""")
        repeat(4) {
            result = regex.replace(result) { match ->
                source[match.groupValues[1]] ?: fallback[match.groupValues[1]] ?: ""
            }
        }
        return result
    }

    private fun String.androidReachableUrl(): String =
        replace("://localhost", "://10.0.2.2")
            .replace("://127.0.0.1", "://10.0.2.2")
}

data class DeveloperConfigItem(
    val key: String,
    val title: String,
    val description: String,
    val required: Boolean,
    val value: String,
    val defaultValue: String
)

private data class DeveloperConfigDefinition(
    val key: String,
    val title: String,
    val description: String,
    val required: Boolean
)
