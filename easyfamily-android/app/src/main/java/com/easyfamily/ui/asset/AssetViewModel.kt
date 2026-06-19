package com.easyfamily.ui.asset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyfamily.data.ApiClient
import com.easyfamily.data.AssetData
import com.easyfamily.data.local.AuthDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssetUiState(
    val assets: List<AssetData> = emptyList(),
    val totalValue: Double = 0.0,
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AssetViewModel @Inject constructor(
    private val authDataStore: AuthDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssetUiState())
    val uiState: StateFlow<AssetUiState> = _uiState.asStateFlow()

    private suspend fun token(): String = authDataStore.accessToken.first()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val t = token()
                val (items, total) = ApiClient.listAssets(t)
                _uiState.value = _uiState.value.copy(assets = items, totalValue = total, loading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun createAsset(name: String, assetType: String, value: Double, note: String?) {
        viewModelScope.launch {
            try {
                ApiClient.createAsset(token(), name, assetType, value, note)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateAsset(id: Int, name: String, assetType: String, value: Double, note: String?) {
        viewModelScope.launch {
            try {
                ApiClient.updateAsset(token(), id, name, assetType, value, note)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteAsset(id: Int) {
        viewModelScope.launch {
            try {
                ApiClient.deleteAsset(token(), id)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
