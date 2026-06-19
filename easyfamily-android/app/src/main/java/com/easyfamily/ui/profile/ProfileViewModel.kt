package com.easyfamily.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyfamily.data.ApiClient
import com.easyfamily.data.UserProfileData
import com.easyfamily.data.local.AuthDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfileData? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authDataStore: AuthDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    suspend fun token(): String = authDataStore.accessToken.first()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val profile = ApiClient.getUserProfile(token())
                _uiState.value = _uiState.value.copy(profile = profile, loading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun saveAll(
        nickname: String,
        email: String,
        butlerName: String,
        butlerAvatarId: Int,
        butlerPersona: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, error = null)
            try {
                val t = token()
                val profile = _uiState.value.profile
                var updated: UserProfileData? = null

                val trimNickname = nickname.trim()
                if (trimNickname.isNotEmpty() && trimNickname != (profile?.nickname ?: "")) {
                    updated = ApiClient.updateNickname(t, trimNickname)
                }
                val trimEmail = email.trim()
                if (trimEmail != (profile?.email ?: "")) {
                    updated = ApiClient.updateEmail(t, trimEmail)
                }
                val effectiveButlerName = butlerName.trim().ifEmpty { "青鸟管家" }.take(10)
                val butlerChanged = effectiveButlerName != (profile?.butlerName ?: "青鸟管家")
                    || butlerAvatarId != (profile?.butlerAvatarId ?: 1)
                    || butlerPersona != (profile?.butlerPersona ?: "warm")
                if (butlerChanged) {
                    updated = ApiClient.updateButler(t, effectiveButlerName, butlerAvatarId, butlerPersona)
                }
                _uiState.value = _uiState.value.copy(
                    profile = updated ?: _uiState.value.profile,
                    saving = false,
                    saved = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
            }
        }
    }
}
