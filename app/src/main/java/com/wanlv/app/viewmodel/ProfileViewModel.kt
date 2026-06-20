package com.wanlv.app.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanlv.app.BuildConfig
import com.wanlv.app.network.AuthSession
import com.wanlv.app.pojo.dto.NormalUserDto
import com.wanlv.app.pojo.dto.NormalUserUpdateRequest
import com.wanlv.app.pojo.dto.ReservationOrderDto
import com.wanlv.app.repository.NormalUserRepository
import com.wanlv.app.repository.UserReservationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

enum class LoginMode {
    PhoneCode,
    Password
}

class ProfileViewModel(
    private val userRepository: NormalUserRepository = NormalUserRepository(),
    private val reservationRepository: UserReservationRepository = UserReservationRepository()
) : ViewModel() {
    val loginMode = mutableStateOf(LoginMode.PhoneCode)
    val phone = mutableStateOf("")
    val verificationCode = mutableStateOf("")
    val username = mutableStateOf("")
    val password = mutableStateOf("")
    val message = mutableStateOf(
        if (AuthSession.token.isNullOrBlank()) "请输入手机号获取验证码" else "已恢复登录状态"
    )
    val messageIsError = mutableStateOf(false)
    val currentUser = mutableStateOf(
        AuthSession.cachedUserData()?.let { NormalUserDto.fromJson(it) }
    )
    val isLoading = mutableStateOf(false)
    val isSendingCode = mutableStateOf(false)
    val codeCountdownSeconds = mutableIntStateOf(0)
    val profileActionLoading = mutableStateOf(false)
    val profileActionMessage = mutableStateOf("")
    val profileActionMessageIsError = mutableStateOf(false)
    val reservationOrders = mutableStateListOf<ReservationOrderDto>()
    val reservationOrdersLoading = mutableStateOf(false)
    val reservationOrdersMessage = mutableStateOf("")
    val reservationOrdersMessageIsError = mutableStateOf(false)
    val enteringReservationNo = mutableStateOf<String?>(null)

    private var codeCountdownJob: Job? = null

    fun switchLoginMode(mode: LoginMode) {
        if (loginMode.value == mode) return
        loginMode.value = mode
        showMessage(
            if (mode == LoginMode.PhoneCode) "请输入手机号获取验证码" else "请输入账号和密码"
        )
    }

    fun updatePhone(value: String) {
        phone.value = value.filter(Char::isDigit).take(MainlandPhoneLength)
    }

    fun updateVerificationCode(value: String) {
        verificationCode.value = value.filter(Char::isDigit).take(VerificationCodeLength)
    }

    fun updateUsername(value: String) {
        username.value = value
    }

    fun updatePassword(value: String) {
        password.value = value
    }

    fun sendPhoneCode() {
        val normalizedPhone = phone.value.trim()
        if (!isValidMainlandPhone(normalizedPhone)) {
            showMessage("请输入正确的 11 位手机号", isError = true)
            return
        }
        if (isSendingCode.value || codeCountdownSeconds.intValue > 0) return

        isSendingCode.value = true
        viewModelScope.launch {
            runCatching { userRepository.sendPhoneCode(normalizedPhone) }
                .onSuccess { result ->
                    val expireSeconds = normalizeCodeExpireSeconds(result.expireSeconds)
                    // 重点：模拟短信仅在 Debug 包自动填充，正式包不依赖接口返回明文验证码。
                    val debugCode = result.code?.takeIf(::isValidVerificationCode)
                    if (BuildConfig.DEBUG && debugCode != null) {
                        verificationCode.value = debugCode
                        showMessage("验证码已发送，调试环境已自动填写")
                    } else {
                        showMessage("验证码已发送，请注意查收")
                    }
                    startCodeCountdown(expireSeconds)
                }
                .onFailure { showMessage(it.message ?: "验证码发送失败", isError = true) }
            isSendingCode.value = false
        }
    }

    fun loginWithPhoneCode() {
        val normalizedPhone = phone.value.trim()
        val normalizedCode = verificationCode.value.trim()
        when {
            !isValidMainlandPhone(normalizedPhone) -> {
                showMessage("请输入正确的 11 位手机号", isError = true)
                return
            }
            !isValidVerificationCode(normalizedCode) -> {
                showMessage("请输入 6 位数字验证码", isError = true)
                return
            }
        }
        performLogin { userRepository.loginWithPhoneCode(normalizedPhone, normalizedCode) }
    }

    fun login() {
        val account = username.value.trim()
        val pwd = password.value
        if (account.isBlank() || pwd.isBlank()) {
            showMessage("请输入账号和密码", isError = true)
            return
        }
        performLogin { userRepository.login(account, pwd) }
    }

    fun loadCurrentUser() {
        if (AuthSession.userId == null) return
        viewModelScope.launch {
            runCatching { userRepository.getCurrentUser() }
                .onSuccess {
                    currentUser.value = it
                    if (it != null) showMessage("资料已同步")
                }
                .onFailure { showMessage(it.message ?: "资料同步失败", isError = true) }
        }
    }

    fun clearUserWhenSessionMissing() {
        if (AuthSession.userId == null || AuthSession.token.isNullOrBlank()) {
            currentUser.value = null
        }
    }

    fun clearProfileActionMessage() {
        profileActionMessage.value = ""
        profileActionMessageIsError.value = false
    }

    fun updateProfile(
        request: NormalUserUpdateRequest,
        onSuccess: () -> Unit = {}
    ) {
        if (profileActionLoading.value) return
        val phone = request.phone?.trim().orEmpty()
        val email = request.email?.trim().orEmpty()
        val interestTags = request.interestTags?.trim().orEmpty()
        when {
            request.id <= 0L -> {
                showProfileActionMessage("用户信息无效，请重新登录", isError = true)
                return
            }
            phone.isNotEmpty() && !isValidMainlandPhone(phone) -> {
                showProfileActionMessage("请输入正确的 11 位手机号", isError = true)
                return
            }
            email.isNotEmpty() && !EmailRegex.matches(email) -> {
                showProfileActionMessage("请输入正确的邮箱地址", isError = true)
                return
            }
            request.age != null && request.age !in 1..120 -> {
                showProfileActionMessage("年龄需在 1-120 岁之间", isError = true)
                return
            }
            interestTags.isNotEmpty() && runCatching { JSONArray(interestTags) }.isFailure -> {
                showProfileActionMessage("兴趣标签需为正确的 JSON 数组", isError = true)
                return
            }
        }

        profileActionLoading.value = true
        clearProfileActionMessage()
        viewModelScope.launch {
            runCatching { userRepository.updateUser(request) }
                .onSuccess {
                    currentUser.value = it
                    showProfileActionMessage("个人信息已更新")
                    onSuccess()
                }
                .onFailure {
                    showProfileActionMessage(it.message ?: "个人信息更新失败", isError = true)
                }
            profileActionLoading.value = false
        }
    }

    fun verifyRealName(
        realName: String,
        idCardNo: String,
        onSuccess: () -> Unit = {}
    ) {
        if (profileActionLoading.value) return
        val normalizedName = realName.trim()
        val normalizedIdCard = idCardNo.trim().uppercase()
        when {
            normalizedName.isBlank() -> {
                showProfileActionMessage("请输入真实姓名", isError = true)
                return
            }
            !IdCardRegex.matches(normalizedIdCard) -> {
                showProfileActionMessage("请输入正确的 18 位身份证号", isError = true)
                return
            }
        }

        profileActionLoading.value = true
        clearProfileActionMessage()
        viewModelScope.launch {
            runCatching { userRepository.verifyRealName(normalizedName, normalizedIdCard) }
                .onSuccess {
                    currentUser.value = it
                    showProfileActionMessage("实名认证成功")
                    onSuccess()
                }
                .onFailure {
                    showProfileActionMessage(it.message ?: "实名认证失败", isError = true)
                }
            profileActionLoading.value = false
        }
    }

    fun loadReservationOrders() {
        if (reservationOrdersLoading.value) return
        reservationOrdersLoading.value = true
        reservationOrdersMessage.value = ""
        reservationOrdersMessageIsError.value = false
        viewModelScope.launch {
            runCatching { reservationRepository.listMyOrders() }
                .onSuccess {
                    reservationOrders.clear()
                    reservationOrders.addAll(it)
                }
                .onFailure {
                    reservationOrdersMessage.value = it.message ?: "预约记录加载失败"
                    reservationOrdersMessageIsError.value = true
                }
            reservationOrdersLoading.value = false
        }
    }

    fun enterReservationOrder(reservationNo: String) {
        val normalizedReservationNo = reservationNo.trim()
        if (normalizedReservationNo.isBlank() || enteringReservationNo.value != null) return
        enteringReservationNo.value = normalizedReservationNo
        reservationOrdersMessage.value = ""
        reservationOrdersMessageIsError.value = false
        viewModelScope.launch {
            runCatching { reservationRepository.enterOrder(normalizedReservationNo) }
                .onSuccess {
                    val index = reservationOrders.indexOfFirst {
                        it.reservationNo == normalizedReservationNo
                    }
                    if (index >= 0) {
                        // 重点：检票成功后立即更新本地状态，待使用列表无需等待二次请求即可移除该订单。
                        reservationOrders[index] = reservationOrders[index].copy(status = "ENTERED")
                    }
                    reservationOrdersMessage.value = "检票成功，订单已入园"
                }
                .onFailure {
                    reservationOrdersMessage.value = it.message ?: "检票入园失败"
                    reservationOrdersMessageIsError.value = true
                }
            enteringReservationNo.value = null
        }
    }

    fun logout() {
        if (isLoading.value) return
        isLoading.value = true
        viewModelScope.launch {
            runCatching { userRepository.logout() }
            currentUser.value = null
            codeCountdownJob?.cancel()
            codeCountdownSeconds.intValue = 0
            verificationCode.value = ""
            password.value = ""
            loginMode.value = LoginMode.PhoneCode
            isLoading.value = false
            showMessage("已退出登录")
        }
    }

    private fun performLogin(request: suspend () -> NormalUserDto) {
        if (isLoading.value) return
        isLoading.value = true
        viewModelScope.launch {
            runCatching { request() }
                .onSuccess {
                    currentUser.value = it
                    password.value = ""
                    verificationCode.value = ""
                    codeCountdownJob?.cancel()
                    codeCountdownSeconds.intValue = 0
                    showMessage("登录成功")
                }
                .onFailure { showMessage(it.message ?: "登录失败", isError = true) }
            isLoading.value = false
        }
    }

    private fun startCodeCountdown(seconds: Int) {
        codeCountdownJob?.cancel()
        codeCountdownJob = viewModelScope.launch {
            codeCountdownSeconds.intValue = seconds
            while (codeCountdownSeconds.intValue > 0) {
                delay(1_000)
                codeCountdownSeconds.intValue--
            }
        }
    }

    private fun showMessage(text: String, isError: Boolean = false) {
        message.value = text
        messageIsError.value = isError
    }

    private fun showProfileActionMessage(text: String, isError: Boolean = false) {
        profileActionMessage.value = text
        profileActionMessageIsError.value = isError
    }

    override fun onCleared() {
        codeCountdownJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val MainlandPhoneLength = 11
        const val VerificationCodeLength = 6
        val EmailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        val IdCardRegex = Regex("^\\d{17}[\\dX]$")
    }
}

internal fun isValidMainlandPhone(phone: String): Boolean =
    Regex("^1[3-9]\\d{9}$").matches(phone)

internal fun isValidVerificationCode(code: String): Boolean =
    Regex("^\\d{6}$").matches(code)

internal fun normalizeCodeExpireSeconds(expireSeconds: Int): Int =
    expireSeconds.takeIf { it > 0 } ?: 300
