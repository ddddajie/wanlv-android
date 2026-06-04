package com.wanlv.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanlv.app.network.AuthSession
import com.wanlv.app.pojo.dto.NormalUserDto
import com.wanlv.app.repository.NormalUserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: NormalUserRepository = NormalUserRepository()
) : ViewModel() {
    val username = mutableStateOf("")
    val password = mutableStateOf("")
    val message = mutableStateOf(if (AuthSession.token.isNullOrBlank()) "请登录普通用户账号" else "已从配置读取登录态")
    val currentUser = mutableStateOf<NormalUserDto?>(null)
    val isLoading = mutableStateOf(false)

    fun updateUsername(value: String) {
        username.value = value
    }

    fun updatePassword(value: String) {
        password.value = value
    }

    fun login() {
        val account = username.value.trim()
        val pwd = password.value
        if (account.isBlank() || pwd.isBlank()) {
            message.value = "请输入账号和密码"
            return
        }
        isLoading.value = true
        viewModelScope.launch {
            runCatching { userRepository.login(account, pwd) }
                .onSuccess {
                    currentUser.value = it
                    message.value = "登录成功"
                    password.value = ""
                }
                .onFailure { message.value = it.message ?: "登录失败" }
            isLoading.value = false
        }
    }

    fun loadCurrentUser() {
        if (AuthSession.userId == null) return
        viewModelScope.launch {
            runCatching { userRepository.getCurrentUser() }
                .onSuccess {
                    currentUser.value = it
                    if (it != null) message.value = "资料已同步"
                }
                .onFailure { message.value = it.message ?: "资料同步失败" }
        }
    }

    fun logout() {
        AuthSession.clear()
        currentUser.value = null
        message.value = "已退出登录"
    }
}
