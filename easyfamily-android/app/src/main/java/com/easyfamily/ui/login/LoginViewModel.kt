package com.easyfamily.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyfamily.data.repository.AuthRepository
import com.easyfamily.data.repository.CaptchaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val loading: Boolean = false,
    val captchaToken: String = "",
    val smsSent: Boolean = false,
    val smsCooldownSeconds: Int = 0,
    val info: String = "请先完成安全校验，再获取短信验证码。",
    val loginSuccess: Boolean = false,
    val loginError: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    val captchaRepository: CaptchaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onCaptchaVerified(token: String) {
        _uiState.value = _uiState.value.copy(
            captchaToken = token,
            info = "安全校验通过，可以获取验证码。"
        )
    }

    fun onCaptchaReset() {
        _uiState.value = _uiState.value.copy(
            captchaToken = "",
            info = "请重新完成人机校验。"
        )
    }

    fun tapVerifyCaptcha() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            authRepository.verifyCaptcha("mock")
                .onSuccess { token ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        captchaToken = token,
                        info = "安全校验通过，可以获取验证码。"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        info = "校验失败：${e.message ?: "unknown"}"
                    )
                }
        }
    }

    fun sendSms(phone: String) {
        val captchaToken = _uiState.value.captchaToken
        if (captchaToken.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            authRepository.sendSms(phone, captchaToken)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        smsSent = true,
                        smsCooldownSeconds = 30,
                        info = "验证码已发送（测试环境默认 123456）。"
                    )
                    startCooldown()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        info = "发送失败：${e.message ?: "unknown"}"
                    )
                }
        }
    }

    fun login(phone: String, smsCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, loginError = null)
            authRepository.login(phone, smsCode)
                .onSuccess { loginData ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        loginSuccess = true,
                        info = "登录成功，userId=${loginData.userId}"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        loginError = "登录失败：${e.message ?: "unknown"}",
                        info = "登录失败：${e.message ?: "unknown"}"
                    )
                }
        }
    }

    fun oneClickLogin(testPhone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, loginError = null)
            authRepository.verifyCaptcha("android-one-click")
                .onSuccess { token ->
                    val captchaToken = token
                    authRepository.sendSms(testPhone, captchaToken)
                        .onSuccess {
                            authRepository.login(testPhone, "123456")
                                .onSuccess { loginData ->
                                    _uiState.value = _uiState.value.copy(
                                        loading = false,
                                        captchaToken = captchaToken,
                                        loginSuccess = true,
                                        info = "一键登录成功，userId=${loginData.userId}"
                                    )
                                }
                                .onFailure { e ->
                                    _uiState.value = _uiState.value.copy(
                                        loading = false,
                                        info = "一键登录失败：${e.message ?: "unknown"}"
                                    )
                                }
                        }
                        .onFailure { e ->
                            _uiState.value = _uiState.value.copy(
                                loading = false,
                                info = "一键登录失败：${e.message ?: "unknown"}"
                            )
                        }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        info = "一键登录失败：${e.message ?: "unknown"}"
                    )
                }
        }
    }

    fun onLoginConsumed() {
        _uiState.value = _uiState.value.copy(loginSuccess = false)
    }

    private fun startCooldown() {
        viewModelScope.launch {
            var seconds = _uiState.value.smsCooldownSeconds
            while (seconds > 0) {
                kotlinx.coroutines.delay(1000)
                seconds -= 1
                _uiState.value = _uiState.value.copy(smsCooldownSeconds = seconds)
            }
        }
    }
}
