package com.easyfamily.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyfamily.data.network.FamilyMemberItem
import com.easyfamily.data.network.MonitorSnapshot
import com.easyfamily.data.network.PhoneItem
import com.easyfamily.data.repository.FamilyRepository
import com.easyfamily.data.repository.MonitorRepository
import com.easyfamily.data.repository.PhoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyUiState(
    val myPhones: List<PhoneItem> = emptyList(),
    val familyMembers: List<FamilyMemberItem> = emptyList(),
    val monitorSnapshots: List<MonitorSnapshot> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val info: String = "加载中..."
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val phoneRepository: PhoneRepository,
    private val monitorRepository: MonitorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null, info = "加载中...")
            val phonesResult = phoneRepository.listMyPhones()
            val membersResult = familyRepository.listFamilyMembers()

            val phones = phonesResult.getOrElse { emptyList() }
            val members = membersResult.getOrElse { emptyList() }

            val hasError = phonesResult.isFailure || membersResult.isFailure
            val infoText = when {
                hasError -> "部分数据加载失败"
                phones.isEmpty() && members.isEmpty() -> "暂无家庭成员"
                else -> "已同步家庭成员"
            }
            val errorText = if (hasError) {
                listOfNotNull(
                    phonesResult.exceptionOrNull()?.message,
                    membersResult.exceptionOrNull()?.message
                ).firstOrNull()
            } else null

            _uiState.value = _uiState.value.copy(
                myPhones = phones,
                familyMembers = members,
                loading = false,
                error = errorText,
                info = infoText
            )
        }
    }

    fun addMember(name: String, phone: String, relation: String) {
        viewModelScope.launch {
            familyRepository.addFamilyMember(name, phone, relation)
                .onSuccess { loadData() }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message ?: "添加失败")
                }
        }
    }

    fun deleteMember(memberId: String) {
        viewModelScope.launch {
            familyRepository.deleteFamilyMember(memberId)
                .onSuccess { loadData() }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message ?: "删除失败")
                }
        }
    }

    fun loadMonitorSnapshots() {
        viewModelScope.launch {
            monitorRepository.getMonitorSnapshots()
                .onSuccess { snapshots ->
                    _uiState.value = _uiState.value.copy(monitorSnapshots = snapshots)
                }
                .onFailure { /* silently ignore monitor failures */ }
        }
    }
}
