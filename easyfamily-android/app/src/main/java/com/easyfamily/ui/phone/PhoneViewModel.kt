package com.easyfamily.ui.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyfamily.data.network.PhoneItem
import com.easyfamily.data.network.RealNameVerifyResult
import com.easyfamily.data.repository.PhoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhoneUiState(
    val phones: List<PhoneItem> = emptyList(),
    val loading: Boolean = false,
    val info: String = "加载中...",
    val queryResult: RealNameVerifyResult? = null
)

@HiltViewModel
class PhoneViewModel @Inject constructor(
    private val phoneRepository: PhoneRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneUiState())
    val uiState: StateFlow<PhoneUiState> = _uiState.asStateFlow()

    init {
        refreshPhones()
    }

    fun refreshPhones() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            phoneRepository.listMyPhones()
                .onSuccess { phones ->
                    _uiState.value = _uiState.value.copy(
                        phones = phones,
                        loading = false,
                        info = "号码列表已刷新"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        info = "刷新失败：${e.message ?: "unknown"}"
                    )
                }
        }
    }

    fun bindPhone(phone: String, smsCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            phoneRepository.bindPhone(phone, smsCode)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(info = "绑定成功")
                    refreshPhones()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        info = "绑定失败：${e.message ?: "unknown"}"
                    )
                }
        }
    }

    fun unbindPhone(phone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            phoneRepository.unbindPhone(phone)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(info = "解绑成功")
                    refreshPhones()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        info = "解绑失败：${e.message ?: "unknown"}"
                    )
                }
        }
    }

    fun setPrimaryPhone(phone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            phoneRepository.setPrimaryPhone(phone)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(info = "已切换主号")
                    refreshPhones()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        info = "切换主号失败：${e.message ?: "unknown"}"
                    )
                }
        }
    }

    fun verifyRealName(phone: String, name: String, idCardNo: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            phoneRepository.verifyRealName(phone, name, idCardNo)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        queryResult = result,
                        info = "实名校验完成"
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
}
