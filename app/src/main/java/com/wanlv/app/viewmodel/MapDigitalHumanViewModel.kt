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
import com.wanlv.app.digitalhuman.sanitizeDigitalHumanSpeechText
import com.wanlv.app.repository.UserAgentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    private var operationJob: Job? = null
    private var sessionGeneration = 0L

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
            val generation = sessionGeneration
            operationJob?.cancel()
            operationJob = viewModelScope.launch {
                try {
                    ensureConnected(generation)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    if (generation == sessionGeneration && uiState.visible) {
                        uiState = uiState.copy(
                            connecting = false,
                            connected = false,
                            status = error.message ?: "数字人连接失败"
                        )
                    }
                }
            }
        }
    }

    fun close() {
        close("map_closed")
    }

    fun close(reason: String) {
        // 重点：关闭时先让当前代连接失效并取消协程，防止旧 /offer 返回后重新写回 session。
        sessionGeneration += 1L
        operationJob?.cancel()
        operationJob = null
        mainHandler.removeCallbacksAndMessages(null)
        sessionManager.disconnect(reason)
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
        val generation = sessionGeneration
        operationJob = viewModelScope.launch {
            try {
                ensureConnected(generation)
                // 重点：新问题开始前先打断上一段播报，避免数字人多段语音重叠。
                sessionManager.interrupt(AppConfig.digitalHumanGuideApiUrl)
                ensureSessionCurrent(generation)
                uiState = uiState.copy(status = "正在向 AI 提问...")
                val answer = agentRepository.chat(question, scenicAreaId).answer
                ensureSessionCurrent(generation)
                val speechText = sanitizeDigitalHumanSpeechText(answer).ifBlank { "暂时没有获取到回复" }
                uiState = uiState.copy(answer = answer, status = "数字导游正在播报...")
                sessionManager.speak(
                    baseUrl = AppConfig.digitalHumanGuideApiUrl,
                    text = speechText
                )
                ensureSessionCurrent(generation)
                uiState = uiState.copy(
                    sending = false,
                    answer = answer,
                    status = "数字导游正在播报..."
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (generation == sessionGeneration && uiState.visible) {
                    uiState = uiState.copy(
                        sending = false,
                        status = error.message ?: "数字人问答失败"
                    )
                }
            }
        }
    }

    private suspend fun ensureConnected(generation: Long): Long = connectMutex.withLock {
        ensureSessionCurrent(generation)
        sessionManager.sessionId ?: run {
            uiState = uiState.copy(connecting = true, connected = false, status = "正在连接数字导游...")
            try {
                val sessionId = sessionManager.connect(
                    baseUrl = AppConfig.digitalHumanGuideApiUrl,
                    onVideoFrame = { bitmap -> onVideoFrame(generation, bitmap) },
                    onConnectionLost = { reason -> onConnectionLost(generation, reason) }
                )
                ensureSessionCurrent(generation)
                uiState = uiState.copy(
                    connecting = false,
                    connected = true,
                    sessionId = sessionId,
                    status = "数字导游已就绪"
                )
                sessionId
            } catch (error: Throwable) {
                if (generation == sessionGeneration) {
                    uiState = uiState.copy(connecting = false, connected = false, sessionId = null)
                }
                throw error
            }
        }
    }

    private suspend fun ensureSessionCurrent(generation: Long) {
        currentCoroutineContext().ensureActive()
        if (generation != sessionGeneration || !uiState.visible) {
            throw CancellationException("数字人会话已关闭")
        }
    }

    private fun onVideoFrame(generation: Long, bitmap: Bitmap) {
        mainHandler.post {
            if (generation == sessionGeneration && uiState.visible) {
                uiState = uiState.copy(videoFrame = bitmap)
            }
        }
    }

    private fun onConnectionLost(generation: Long, reason: String) {
        mainHandler.post {
            if (generation == sessionGeneration && uiState.visible) {
                uiState = uiState.copy(
                    connecting = false,
                    connected = false,
                    sessionId = null,
                    status = if (reason == "webrtc_disconnected_timeout") {
                        "数字人连接已断开，请重新连接"
                    } else {
                        "数字人连接异常，请重新连接"
                    }
                )
            }
        }
    }

    override fun onCleared() {
        sessionGeneration += 1L
        operationJob?.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        sessionManager.release()
        super.onCleared()
    }
}
