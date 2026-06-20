package com.wanlv.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wanlv.app.R
import com.wanlv.app.config.AppConfig
import com.wanlv.app.data.RecentScenicContextStore
import com.wanlv.app.digitalhuman.DigitalHumanSessionManager
import com.wanlv.app.digitalhuman.GreenScreenKeyer
import com.wanlv.app.digitalhuman.sanitizeDigitalHumanSpeechText
import com.wanlv.app.model.ChatMessage
import com.wanlv.app.network.AuthSession
import com.wanlv.app.repository.UserAgentRepository
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val LoginRequiredMessage = "请先登录后再使用智能问答"

enum class ChatDigitalHumanPersona(
    val label: String,
    val shortLabel: String,
    val description: String,
    val previewResId: Int
) {
    Guide("导游女数字人", "导游", "景区讲解和路线建议", R.drawable.famale),
    Service("客服男数字人", "客服", "咨询答疑和服务说明", R.drawable.male);

    val apiUrl: String
        get() = when (this) {
            Guide -> AppConfig.digitalHumanGuideApiUrl
            Service -> AppConfig.digitalHumanServiceApiUrl
        }

    companion object {
        val all: List<ChatDigitalHumanPersona> = values().toList()
    }
}

data class ChatDigitalHumanUiState(
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val speaking: Boolean = false,
    val persona: ChatDigitalHumanPersona = ChatDigitalHumanPersona.Guide,
    val status: String = "未连接数字人",
    val sessionId: Long? = null,
    val videoFrame: Bitmap? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val agentRepository = UserAgentRepository()
    private val sessionManager = DigitalHumanSessionManager(application.applicationContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val connectMutex = Mutex()
    private val messageIdGenerator = AtomicLong(System.currentTimeMillis())
    private var connectionJob: Job? = null
    private var sessionGeneration = 0L
    private val previewBitmaps = ChatDigitalHumanPersona.all.associateWith { persona ->
        GreenScreenKeyer.keyBitmap(BitmapFactory.decodeResource(application.resources, persona.previewResId))
    }

    val messages = mutableStateListOf<ChatMessage>()
    var input = mutableStateOf("")
        private set
    var scenicContext by mutableStateOf(RecentScenicContextStore.load(application))
        private set
    var digitalHumanState by mutableStateOf(ChatDigitalHumanUiState())
        private set

    val currentDigitalHumanPreviewBitmap: Bitmap
        get() = previewBitmaps[digitalHumanState.persona]
            ?: previewBitmaps.getValue(ChatDigitalHumanPersona.Guide)

    fun updateInput(value: String) {
        input.value = value
    }

    fun refreshScenicContext() {
        scenicContext = RecentScenicContextStore.load(getApplication())
    }

    fun previewBitmap(persona: ChatDigitalHumanPersona): Bitmap =
        previewBitmaps[persona] ?: previewBitmaps.getValue(ChatDigitalHumanPersona.Guide)

    fun toggleDigitalHumanConnection() {
        if (digitalHumanState.connected || digitalHumanState.connecting) {
            disconnectDigitalHuman()
        } else {
            connectDigitalHuman()
        }
    }

    fun switchDigitalHumanPersona(persona: ChatDigitalHumanPersona) {
        if (persona == digitalHumanState.persona) return
        val shouldReconnect = digitalHumanState.connected || digitalHumanState.connecting
        sessionGeneration += 1L
        connectionJob?.cancel()
        sessionManager.disconnect("persona_switched")
        digitalHumanState = ChatDigitalHumanUiState(
            persona = persona,
            status = if (shouldReconnect) "正在切换为${persona.shortLabel}..." else "已切换为${persona.label}"
        )
        if (shouldReconnect) connectDigitalHuman()
    }

    fun connectDigitalHuman() {
        if (digitalHumanState.connected || digitalHumanState.connecting) return
        val generation = sessionGeneration
        val persona = digitalHumanState.persona
        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            try {
                ensureDigitalHumanConnected(generation, persona)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (generation == sessionGeneration && persona == digitalHumanState.persona) {
                    digitalHumanState = digitalHumanState.copy(
                        connecting = false,
                        connected = false,
                        speaking = false,
                        sessionId = null,
                        status = error.message ?: "数字人连接失败"
                    )
                }
            }
        }
    }

    fun disconnectDigitalHuman() {
        disconnectDigitalHuman("manual_disconnect")
    }

    fun onHostStopped() {
        disconnectDigitalHuman("android_on_stop")
    }

    fun onRouteDisposed() {
        disconnectDigitalHuman("route_disposed")
    }

    private fun disconnectDigitalHuman(reason: String) {
        sessionGeneration += 1L
        connectionJob?.cancel()
        connectionJob = null
        mainHandler.removeCallbacksAndMessages(null)
        sessionManager.disconnect(reason)
        digitalHumanState = ChatDigitalHumanUiState(
            persona = digitalHumanState.persona,
            status = "数字人已断开，不影响继续问答"
        )
    }

    fun sendMessage() {
        val content = input.value.trim()
        if (content.isEmpty()) return

        if (AuthSession.userId == null || AuthSession.token.isNullOrBlank()) {
            input.value = ""
            // 重点：未登录时不发起 Agent 请求，并且只保留一条提示，避免连续发送堆积消息。
            addSystemMessageIfChanged(LoginRequiredMessage)
            return
        }

        messages.add(ChatMessage(nextMessageId(), content, true))
        input.value = ""
        val loadingId = nextMessageId()
        messages.add(ChatMessage(loadingId, "正在思考...", false))
        viewModelScope.launch {
            if (digitalHumanState.connected) {
                // 重点：新问题开始前先打断上一段数字人播报，避免语音重叠。
                runCatching { sessionManager.interrupt(digitalHumanState.persona.apiUrl) }
                digitalHumanState = digitalHumanState.copy(speaking = false, status = "正在等待 AI 回复...")
            }

            val context = scenicContext
            val result = runCatching {
                agentRepository.chat(
                    content = content,
                    // 重点：景区名称仅供页面展示，请求按后端协议只显式发送景区 ID。
                    scenicAreaId = context?.scenicAreaId
                )
            }
            messages.removeAll { it.id == loadingId }
            if (result.isSuccess) {
                val response = result.getOrThrow()
                messages.add(ChatMessage(nextMessageId(), response.answer, false))
                speakAnswerIfConnected(response.answer)
            } else {
                val error = result.exceptionOrNull()
                // 智能问答需要登录态；失败时不静默吞掉，方便联调定位接口或 token 问题。
                addSystemMessageIfChanged(error?.message ?: "智能问答接口调用失败")
            }
        }
    }

    private fun nextMessageId(): Long = messageIdGenerator.incrementAndGet()

    private fun addSystemMessageIfChanged(content: String) {
        if (messages.lastOrNull()?.let { !it.fromUser && it.content == content } == true) return
        // 重点：严格递增 ID 保证 LazyColumn 的 key 永不重复，连续快速发送也不会触发 Compose 崩溃。
        messages.add(ChatMessage(nextMessageId(), content, false))
    }

    private suspend fun ensureDigitalHumanConnected(
        generation: Long,
        persona: ChatDigitalHumanPersona
    ): Long = connectMutex.withLock {
        ensureSessionCurrent(generation, persona)
        sessionManager.sessionId ?: run {
            digitalHumanState = digitalHumanState.copy(
                connecting = true,
                connected = false,
                speaking = false,
                status = "正在连接${persona.shortLabel}..."
            )
            val sessionId = sessionManager.connect(
                baseUrl = persona.apiUrl,
                onVideoFrame = { bitmap -> onVideoFrame(generation, persona, bitmap) },
                onConnectionLost = { reason -> onConnectionLost(generation, persona, reason) }
            )
            ensureSessionCurrent(generation, persona)
            digitalHumanState = digitalHumanState.copy(
                connecting = false,
                connected = true,
                sessionId = sessionId,
                status = "${persona.shortLabel}已就绪"
            )
            sessionId
        }
    }

    private suspend fun ensureSessionCurrent(generation: Long, persona: ChatDigitalHumanPersona) {
        currentCoroutineContext().ensureActive()
        if (generation != sessionGeneration || persona != digitalHumanState.persona) {
            throw CancellationException("数字人会话已关闭")
        }
    }

    private suspend fun speakAnswerIfConnected(answer: String) {
        if (!digitalHumanState.connected || digitalHumanState.connecting) return
        val persona = digitalHumanState.persona
        val speechText = sanitizeDigitalHumanSpeechText(answer).ifBlank { return }
        // 重点：数字人播报是附加能力，失败只更新状态，不影响 AI 问答文本展示。
        runCatching {
            digitalHumanState = digitalHumanState.copy(speaking = true, status = "${persona.shortLabel}正在播报...")
            sessionManager.speak(baseUrl = persona.apiUrl, text = speechText)
        }.onSuccess {
            digitalHumanState = digitalHumanState.copy(speaking = false, status = "${persona.shortLabel}播报已发送")
        }.onFailure { error ->
            digitalHumanState = digitalHumanState.copy(
                speaking = false,
                status = error.message ?: "数字人播报失败，问答仍可继续"
            )
        }
    }

    private fun onVideoFrame(generation: Long, persona: ChatDigitalHumanPersona, bitmap: Bitmap) {
        mainHandler.post {
            if (generation == sessionGeneration && persona == digitalHumanState.persona) {
                digitalHumanState = digitalHumanState.copy(videoFrame = bitmap)
            }
        }
    }

    private fun onConnectionLost(generation: Long, persona: ChatDigitalHumanPersona, reason: String) {
        mainHandler.post {
            if (generation == sessionGeneration && persona == digitalHumanState.persona) {
                digitalHumanState = digitalHumanState.copy(
                    connecting = false,
                    connected = false,
                    speaking = false,
                    sessionId = null,
                    status = if (reason == "webrtc_disconnected_timeout") {
                        "${persona.shortLabel}连接已断开，请重新连接"
                    } else {
                        "${persona.shortLabel}连接异常，请重新连接"
                    }
                )
            }
        }
    }

    override fun onCleared() {
        sessionGeneration += 1L
        connectionJob?.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        sessionManager.release()
        super.onCleared()
    }
}
