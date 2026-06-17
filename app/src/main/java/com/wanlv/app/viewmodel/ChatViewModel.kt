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
import com.wanlv.app.data.MockData
import com.wanlv.app.digitalhuman.DigitalHumanSessionManager
import com.wanlv.app.digitalhuman.GreenScreenKeyer
import com.wanlv.app.digitalhuman.sanitizeDigitalHumanSpeechText
import com.wanlv.app.model.ChatMessage
import com.wanlv.app.repository.UserAgentRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val previewBitmaps = ChatDigitalHumanPersona.all.associateWith { persona ->
        GreenScreenKeyer.keyBitmap(BitmapFactory.decodeResource(application.resources, persona.previewResId))
    }

    val messages = mutableStateListOf<ChatMessage>().apply {
        addAll(MockData.initialMessages)
    }
    var input = mutableStateOf("")
        private set
    var digitalHumanState by mutableStateOf(ChatDigitalHumanUiState())
        private set

    val currentDigitalHumanPreviewBitmap: Bitmap
        get() = previewBitmaps[digitalHumanState.persona]
            ?: previewBitmaps.getValue(ChatDigitalHumanPersona.Guide)

    fun updateInput(value: String) {
        input.value = value
    }

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
        sessionManager.disconnect()
        digitalHumanState = ChatDigitalHumanUiState(
            persona = persona,
            status = if (shouldReconnect) "正在切换为${persona.shortLabel}..." else "已切换为${persona.label}"
        )
        if (shouldReconnect) connectDigitalHuman()
    }

    fun connectDigitalHuman() {
        if (digitalHumanState.connected || digitalHumanState.connecting) return
        viewModelScope.launch {
            runCatching { ensureDigitalHumanConnected() }
                .onFailure { error ->
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

    fun disconnectDigitalHuman() {
        sessionManager.disconnect()
        digitalHumanState = ChatDigitalHumanUiState(
            persona = digitalHumanState.persona,
            status = "数字人已断开，不影响继续问答"
        )
    }

    fun sendMessage() {
        val content = input.value.trim()
        if (content.isEmpty()) return
        messages.add(ChatMessage(System.currentTimeMillis(), content, true))
        input.value = ""
        val loadingId = System.currentTimeMillis() + 1
        messages.add(ChatMessage(loadingId, "正在思考...", false))
        viewModelScope.launch {
            if (digitalHumanState.connected) {
                // 重点：新问题开始前先打断上一段数字人播报，避免语音重叠。
                runCatching { sessionManager.interrupt(digitalHumanState.persona.apiUrl) }
                digitalHumanState = digitalHumanState.copy(speaking = false, status = "正在等待 AI 回复...")
            }

            val result = runCatching { agentRepository.chat(content) }
            messages.removeAll { it.id == loadingId }
            if (result.isSuccess) {
                val response = result.getOrThrow()
                messages.add(ChatMessage(System.currentTimeMillis(), response.answer, false))
                speakAnswerIfConnected(response.answer)
            } else {
                val error = result.exceptionOrNull()
                // 智能问答需要登录态；失败时不静默吞掉，方便联调定位接口或 token 问题。
                messages.add(ChatMessage(System.currentTimeMillis(), error?.message ?: "智能问答接口调用失败", false))
            }
        }
    }

    private suspend fun ensureDigitalHumanConnected(): Long = connectMutex.withLock {
        sessionManager.sessionId ?: run {
            val persona = digitalHumanState.persona
            digitalHumanState = digitalHumanState.copy(
                connecting = true,
                connected = false,
                speaking = false,
                status = "正在连接${persona.shortLabel}..."
            )
            val sessionId = sessionManager.connect(
                baseUrl = persona.apiUrl,
                onVideoFrame = ::onVideoFrame
            )
            digitalHumanState = digitalHumanState.copy(
                connecting = false,
                connected = true,
                sessionId = sessionId,
                status = "${persona.shortLabel}已就绪"
            )
            sessionId
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

    private fun onVideoFrame(bitmap: Bitmap) {
        mainHandler.post {
            digitalHumanState = digitalHumanState.copy(videoFrame = bitmap)
        }
    }

    override fun onCleared() {
        sessionManager.release()
        super.onCleared()
    }
}
