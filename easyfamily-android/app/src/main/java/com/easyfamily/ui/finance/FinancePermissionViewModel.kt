package com.easyfamily.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyfamily.data.ApiClient
import com.easyfamily.data.local.AuthDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionUiState(
    val role: String = "none",
    val viewers: List<String> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FinancePermissionViewModel @Inject constructor(
    private val authDataStore: AuthDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    private suspend fun token(): String = authDataStore.accessToken.first()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val t = token()
                val role = ApiClient.getFinanceRole(t)
                val viewers = if (role.role == "head") {
                    runCatching { ApiClient.listFinancePermissions(t) }.getOrElse { emptyList() }
                } else emptyList()
                _uiState.value = _uiState.value.copy(
                    role = role.role,
                    viewers = viewers,
                    loading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun grantPermission(phone: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                ApiClient.grantFinancePermission(token(), phone)
                load()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "授权失败")
            }
        }
    }

    fun revokePermission(phone: String) {
        viewModelScope.launch {
            try {
                ApiClient.revokeFinancePermission(token(), phone)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
