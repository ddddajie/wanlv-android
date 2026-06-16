plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun loadWanLvEnv(): Map<String, String> {
    val envFile = project.file("src/main/assets/config.yml")
    if (!envFile.exists()) return emptyMap()
    val rawValues = envFile.readLines(Charsets.UTF_8)
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && (it.contains(":") || it.contains("=")) }
        .associate {
            val separator = if (it.contains(":")) ":" else "="
            val key = it.substringBefore(separator).trim()
            val value = it.substringAfter(separator).trim()
            key to value
        }
    val regex = Regex("""\$\{([A-Za-z0-9_]+)}""")
    fun expand(value: String): String {
        var result = value
        repeat(4) {
            result = regex.replace(result) { match -> rawValues[match.groupValues[1]] ?: "" }
        }
        return result
    }
    return rawValues.mapValues { (_, value) -> expand(value) }
}

val wanLvEnv = loadWanLvEnv()

fun wanLvEnvValue(key: String, fallback: String = ""): String =
    wanLvEnv[key]?.takeIf { it.isNotBlank() } ?: fallback

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.wanlv.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wanlv.app"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "WANLV_API_BASE_URL", buildConfigString(wanLvEnvValue("WANLV_API_BASE_URL", "http://127.0.0.1:8080")))
        buildConfigField("String", "WANLV_DIGITAL_HUMAN_GUIDE_API_URL", buildConfigString(wanLvEnvValue("WANLV_DIGITAL_HUMAN_GUIDE_API_URL", "http://127.0.0.1:8011")))
        buildConfigField("String", "WANLV_DIGITAL_HUMAN_SERVICE_API_URL", buildConfigString(wanLvEnvValue("WANLV_DIGITAL_HUMAN_SERVICE_API_URL", "http://127.0.0.1:8010")))
        buildConfigField("String", "WANLV_MAP_STYLE_URL", buildConfigString(wanLvEnvValue("WANLV_MAP_STYLE_URL")))
        buildConfigField("String", "WANLV_MAP_VECTOR_SOURCE_URL", buildConfigString(wanLvEnvValue("WANLV_MAP_VECTOR_SOURCE_URL", "http://10.0.2.2:3000/china")))
        buildConfigField("String", "WANLV_MAP_RASTER_TILE_URL", buildConfigString(wanLvEnvValue("WANLV_MAP_RASTER_TILE_URL")))
        buildConfigField("String", "WANLV_MAP_TILE_ATTRIBUTION", buildConfigString(wanLvEnvValue("WANLV_MAP_TILE_ATTRIBUTION", "Local Martin tiles")))
        buildConfigField("String", "WANLV_DEBUG_USER_ID", buildConfigString(wanLvEnvValue("WANLV_DEBUG_USER_ID")))
        buildConfigField("String", "WANLV_DEBUG_TOKEN", buildConfigString(wanLvEnvValue("WANLV_DEBUG_TOKEN")))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.maplibre.android)
    implementation(libs.haze)
    implementation(libs.webrtc.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
