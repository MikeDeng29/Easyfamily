package com.easyfamily.ui.bill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyfamily.data.ApiClient
import com.easyfamily.data.BillItemDto
import com.easyfamily.data.BillStatsDto
import com.easyfamily.data.local.AuthDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BillUiState(
    val bills: List<BillItemDto> = emptyList(),
    val stats: BillStatsDto? = null,
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BillViewModel @Inject constructor(
    private val authDataStore: AuthDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillUiState())
    val uiState: StateFlow<BillUiState> = _uiState.asStateFlow()

    private suspend fun token(): String = authDataStore.accessToken.first()

    fun load(month: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val t = token()
                val bills = ApiClient.listBills(t, month)
                val stats = ApiClient.getBillStats(t, month)
                _uiState.value = _uiState.value.copy(bills = bills, stats = stats, loading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun deleteBill(id: Long) {
        viewModelScope.launch {
            try {
                ApiClient.deleteBill(token(), id)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun createBill(category: String, amount: Double, note: String?, date: String) {
        viewModelScope.launch {
            try {
                ApiClient.createBill(token(), category, amount, note, date)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
