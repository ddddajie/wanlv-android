package com.wanlv.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wanlv.app.R
import com.wanlv.app.config.AppConfig
import com.wanlv.app.digitalhuman.DigitalHumanSessionManager
import com.wanlv.app.digitalhuman.GreenScreenKeyer
import com.wanlv.app.repository.UserAgentRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class MapDigitalHumanUiState(
    val visible: Boolean = false,
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val sending: Boolean = false,
    val input: String = "",
    val status: String = "点击后连接数字导游",
    val answer: String? = null,
    val sessionId: Long? = null,
    val videoFrame: Bitmap? = null
)

class MapDigitalHumanViewModel(application: Application) : AndroidViewModel(application) {
    private val agentRepository = UserAgentRepository()
    private val sessionManager = DigitalHumanSessionManager(application.applicationContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val connectMutex = Mutex()

    val previewBitmap: Bitmap = GreenScreenKeyer.keyBitmap(
        BitmapFactory.decodeResource(application.resources, R.drawable.famale)
    )

    var uiState by mutableStateOf(MapDigitalHumanUiState())
        private set

    fun toggleVisible() {
        if (uiState.visible) {
            close()
        } else {
            open()
        }
    }

    fun open() {
        uiState = uiState.copy(visible = true)
        if (!uiState.connected && !uiState.connecting) {
            viewModelScope.launch {
                runCatching { ensureConnected() }
                    .onFailure { error ->
                        uiState = uiState.copy(
                            connecting = false,
                            connected = false,
                            status = error.message ?: "数字人连接失败"
                        )
                    }
            }
        }
    }

    fun close() {
        sessionManager.disconnect()
        uiState = MapDigitalHumanUiState()
    }

    fun updateInput(value: String) {
        uiState = uiState.copy(input = value)
    }

    fun sendQuestion(scenicAreaId: Long?) {
        val question = uiState.input.trim()
        if (question.isEmpty() || uiState.sending) return
        uiState = uiState.copy(
            visible = true,
            input = "",
            sending = true,
            answer = null,
            status = "正在唤醒数字导游..."
        )
        viewModelScope.launch {
            runCatching {
                ensureConnected()
                // 重点：新问题开始前先打断上一段播报，避免数字人多段语音重叠。
                sessionManager.interrupt(AppConfig.digitalHumanGuideApiUrl)
                uiState = uiState.copy(status = "正在向 AI 提问...")
                val answer = agentRepository.chat(question, scenicAreaId).answer
                val speechText = sanitizeReplyText(answer).ifBlank { "暂时没有获取到回复" }
                uiState = uiState.copy(answer = answer, status = "数字导游正在播报...")
                sessionManager.speak(
                    baseUrl = AppConfig.digitalHumanGuideApiUrl,
                    text = speechText
                )
                answer
            }.onSuccess { answer ->
                uiState = uiState.copy(
                    sending = false,
                    answer = answer,
                    status = "数字导游正在播报..."
                )
            }.onFailure { error ->
                uiState = uiState.copy(
                    sending = false,
                    status = error.message ?: "数字人问答失败"
                )
            }
        }
    }

    private suspend fun ensureConnected(): Long = connectMutex.withLock {
        sessionManager.sessionId ?: run {
            uiState = uiState.copy(connecting = true, connected = false, status = "正在连接数字导游...")
            val sessionId = sessionManager.connect(
                baseUrl = AppConfig.digitalHumanGuideApiUrl,
                onVideoFrame = ::onVideoFrame
            )
            uiState = uiState.copy(
                connecting = false,
                connected = true,
                sessionId = sessionId,
                status = "数字导游已就绪"
            )
            sessionId
        }
    }

    private fun onVideoFrame(bitmap: Bitmap) {
        mainHandler.post {
            uiState = uiState.copy(videoFrame = bitmap)
        }
    }

    override fun onCleared() {
        sessionManager.release()
        super.onCleared()
    }
}

private fun sanitizeReplyText(text: String): String =
    removeEmojiForSpeech(text)
        .replace(Regex("\\*\\*|\\*|#|\\[|\\]|\\(|\\)"), "")
        .replace(Regex("[ \\t]{2,}"), " ")
        .replace(Regex("\\s+\\n"), "\n")
        .trim()

private fun removeEmojiForSpeech(text: String): String {
    val builder = StringBuilder(text.length)
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        // 重点：数字人播报前过滤 emoji、肤色修饰符、国旗区域符号和零宽连接符，避免 TTS 读出异常字符。
        if (!isSpeechEmojiCodePoint(codePoint)) {
            builder.appendCodePoint(codePoint)
        }
        index += Character.charCount(codePoint)
    }
    return builder.toString()
}

private fun isSpeechEmojiCodePoint(codePoint: Int): Boolean =
    when {
        codePoint == 0xFE0F || codePoint == 0x200D -> true
        codePoint in 0x1F1E6..0x1F1FF -> true
        codePoint in 0x1F3FB..0x1F3FF -> true
        codePoint in 0x1F000..0x1FAFF -> true
        codePoint in 0x2600..0x27BF -> true
        codePoint in 0x2300..0x23FF -> true
        codePoint in 0x2B00..0x2BFF -> true
        codePoint in 0x2194..0x21AA -> true
        codePoint == 0x00A9 || codePoint == 0x00AE -> true
        codePoint == 0x203C || codePoint == 0x2049 || codePoint == 0x2122 || codePoint == 0x2139 -> true
        codePoint == 0x3030 || codePoint == 0x303D || codePoint == 0x3297 || codePoint == 0x3299 -> true
        else -> false
    }
