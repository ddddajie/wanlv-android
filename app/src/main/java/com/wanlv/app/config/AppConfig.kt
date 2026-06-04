package com.wanlv.app.config

import android.content.Context
import com.wanlv.app.BuildConfig

object AppConfig {
    private const val CONFIG_FILE_NAME = "config.yml"
    private val values = mutableMapOf<String, String>()

    val apiBaseUrl: String
        get() = values["WANLV_API_BASE_URL"].orEmpty().ifBlank { BuildConfig.WANLV_API_BASE_URL }

    val debugUserId: Long?
        get() = values["WANLV_DEBUG_USER_ID"]?.toLongOrNull()

    val debugToken: String?
        get() = values["WANLV_DEBUG_TOKEN"]?.takeIf { it.isNotBlank() }

    fun init(context: Context) {
        if (values.isNotEmpty()) return
        loadBuildConfigValues()
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
                rawValues.forEach { (key, value) -> values[key] = expand(value, rawValues) }
            }
        }
    }

    private fun loadBuildConfigValues() {
        // 构建期读取 config 写入 BuildConfig，运行期读取 assets/config 作为覆盖配置。
        values["WANLV_API_BASE_URL"] = BuildConfig.WANLV_API_BASE_URL
        values["WANLV_DEBUG_USER_ID"] = BuildConfig.WANLV_DEBUG_USER_ID
        values["WANLV_DEBUG_TOKEN"] = BuildConfig.WANLV_DEBUG_TOKEN
    }

    private fun expand(value: String, source: Map<String, String>): String {
        var result = value
        val regex = Regex("""\$\{([A-Za-z0-9_]+)}""")
        repeat(4) {
            result = regex.replace(result) { match ->
                source[match.groupValues[1]] ?: values[match.groupValues[1]] ?: ""
            }
        }
        return result
    }
}
