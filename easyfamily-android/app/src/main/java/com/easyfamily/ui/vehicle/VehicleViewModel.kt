package com.easyfamily.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyfamily.data.ApiClient
import com.easyfamily.data.VehicleItemDto
import com.easyfamily.data.MaintenanceRecordDto
import com.easyfamily.data.VehicleStatsDto
import com.easyfamily.data.local.AuthDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

data class VehicleUiState(
    val vehicles: List<VehicleItemDto> = listOf(),
    val records: List<MaintenanceRecordDto> = listOf(),
    val stats: VehicleStatsDto? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val selectedVehicleId: Long? = null,
    val info: String = ""
)

@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val authDataStore: AuthDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleUiState())
    val uiState: StateFlow<VehicleUiState> = _uiState.asStateFlow()

    private suspend fun token(): String = authDataStore.accessToken.first()

    fun loadVehicles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val vehicles = ApiClient.listVehicles(token())
                _uiState.value = _uiState.value.copy(vehicles = vehicles, loading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun createVehicle(plate: String, brand: String, model: String, year: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            try {
                ApiClient.createVehicle(token(), plate, brand, model, year.toIntOrNull())
                loadVehicles()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun deleteVehicle(vehicleId: Long) {
        viewModelScope.launch {
            try {
                ApiClient.deleteVehicle(token(), vehicleId)
                loadVehicles()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun selectVehicle(vehicleId: Long) {
        _uiState.value = _uiState.value.copy(selectedVehicleId = vehicleId, records = listOf(), stats = null)
        loadRecords(vehicleId)
        loadStats(vehicleId)
    }

    private fun loadRecords(vehicleId: Long) {
        viewModelScope.launch {
            try {
                val records = ApiClient.listRecords(token(), vehicleId)
                _uiState.value = _uiState.value.copy(records = records)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun loadStats(vehicleId: Long) {
        viewModelScope.launch {
            try {
                val stats = ApiClient.getVehicleStats(token(), vehicleId)
                _uiState.value = _uiState.value.copy(stats = stats)
            } catch (_: Exception) { }
        }
    }

    fun createRecord(
        vehicleId: Long,
        serviceDate: String,
        mileageKm: String,
        shopName: String,
        notes: String,
        items: List<Triple<String, String, String>> // category, itemName, cost
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            try {
                val itemsJson = JSONArray()
                for ((cat, name, cost) in items) {
                    itemsJson.put(JSONObject()
                        .put("category", cat)
                        .put("itemName", name)
                        .put("cost", cost.toBigDecimalOrNull() ?: 0)
                        .put("isDiy", false)
                    )
                }
                ApiClient.createRecord(
                    token(), vehicleId, serviceDate,
                    mileageKm.toIntOrNull(), shopName.takeIf { it.isNotBlank() },
                    notes.takeIf { it.isNotBlank() }, itemsJson
                )
                selectVehicle(vehicleId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun deleteRecord(vehicleId: Long, recordId: Long) {
        viewModelScope.launch {
            try {
                ApiClient.deleteRecord(token(), vehicleId, recordId)
                selectVehicle(vehicleId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
