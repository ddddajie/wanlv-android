package com.wanlv.app.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanlv.app.data.MockData
import com.wanlv.app.model.ChatMessage
import com.wanlv.app.repository.UserAgentRepository
import kotlinx.coroutines.launch

class ChatViewModel(
    private val agentRepository: UserAgentRepository = UserAgentRepository()
) : ViewModel() {
    val messages = mutableStateListOf<ChatMessage>().apply {
        addAll(MockData.initialMessages)
    }
    var input = mutableStateOf("")
        private set

    fun updateInput(value: String) {
        input.value = value
    }

    fun sendMessage() {
        val content = input.value.trim()
        if (content.isEmpty()) return
        messages.add(ChatMessage(System.currentTimeMillis(), content, true))
        input.value = ""
        val loadingId = System.currentTimeMillis() + 1
        messages.add(ChatMessage(loadingId, "正在思考...", false))
        viewModelScope.launch {
            runCatching { agentRepository.chat(content) }
                .onSuccess { response ->
                    messages.removeAll { it.id == loadingId }
                    messages.add(ChatMessage(System.currentTimeMillis(), response.answer, false))
                }
                .onFailure { error ->
                    messages.removeAll { it.id == loadingId }
                    // 智能问答需要登录态；失败时不静默吞掉，方便联调定位接口或 token 问题。
                    messages.add(ChatMessage(System.currentTimeMillis(), error.message ?: "智能问答接口调用失败", false))
                }
        }
    }
}
