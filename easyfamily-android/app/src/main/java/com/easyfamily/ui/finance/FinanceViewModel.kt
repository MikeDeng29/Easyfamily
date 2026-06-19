package com.easyfamily.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyfamily.data.ApiClient
import com.easyfamily.data.FamilyBillStatsData
import com.easyfamily.data.FinanceRoleData
import com.easyfamily.data.HealthReportData
import com.easyfamily.data.local.AuthDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class FinanceUiState(
    val selectedMonth: String = currentMonth(),
    val healthReport: HealthReportData? = null,
    val familyBillStats: FamilyBillStatsData? = null,
    val financeRole: FinanceRoleData? = null,
    val loading: Boolean = false,
    val error: String? = null
)

private fun currentMonth(): String {
    val now = LocalDate.now()
    return now.format(DateTimeFormatter.ofPattern("yyyy-MM"))
}

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val authDataStore: AuthDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    private suspend fun token(): String = authDataStore.accessToken.first()

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val t = token()
                val month = _uiState.value.selectedMonth
                val report = runCatching { ApiClient.getHealthReport(t, month) }.getOrNull()
                val billStats = runCatching { ApiClient.getFamilyBillStats(t, month) }.getOrNull()
                val role = runCatching { ApiClient.getFinanceRole(t) }.getOrNull()
                _uiState.value = _uiState.value.copy(
                    healthReport = report,
                    familyBillStats = billStats,
                    financeRole = role,
                    loading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun selectMonth(month: String) {
        _uiState.value = _uiState.value.copy(selectedMonth = month)
        loadAll()
    }

    fun recentMonths(): List<String> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        val now = LocalDate.now()
        return (0 until 12).map { i ->
            now.minusMonths(i.toLong()).format(formatter)
        }
    }
}
