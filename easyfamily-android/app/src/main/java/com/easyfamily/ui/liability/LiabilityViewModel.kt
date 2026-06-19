package com.easyfamily.ui.liability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyfamily.data.ApiClient
import com.easyfamily.data.LiabilityData
import com.easyfamily.data.local.AuthDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiabilityUiState(
    val liabilities: List<LiabilityData> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalMonthlyPayment: Double = 0.0,
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LiabilityViewModel @Inject constructor(
    private val authDataStore: AuthDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiabilityUiState())
    val uiState: StateFlow<LiabilityUiState> = _uiState.asStateFlow()

    private suspend fun token(): String = authDataStore.accessToken.first()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val t = token()
                val (items, total, monthly) = ApiClient.listLiabilities(t)
                _uiState.value = _uiState.value.copy(
                    liabilities = items,
                    totalBalance = total,
                    totalMonthlyPayment = monthly,
                    loading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun createLiability(
        name: String,
        liabilityType: String,
        balance: Double,
        monthlyPayment: Double?,
        interestRate: Double?,
        note: String?
    ) {
        viewModelScope.launch {
            try {
                ApiClient.createLiability(token(), name, liabilityType, balance, monthlyPayment, interestRate, note)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateLiability(
        id: Int,
        name: String,
        liabilityType: String,
        balance: Double,
        monthlyPayment: Double?,
        interestRate: Double?,
        note: String?
    ) {
        viewModelScope.launch {
            try {
                ApiClient.updateLiability(token(), id, name, liabilityType, balance, monthlyPayment, interestRate, note)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteLiability(id: Int) {
        viewModelScope.launch {
            try {
                ApiClient.deleteLiability(token(), id)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
