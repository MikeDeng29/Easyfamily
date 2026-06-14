package com.easyfamily.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyfamily.data.ApiClient
import com.easyfamily.data.local.AuthDataStore
import com.easyfamily.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class ChatMessage(
    val role: String, // "user" or "ai"
    val content: String,
    val isStreaming: Boolean = false
)

data class BillActionData(
    val category: String,
    val amount: Double,
    val note: String?,
    val date: String
)

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(),
    val input: String = "",
    val loading: Boolean = false,
    val accessToken: String = "",
    val pendingBillAction: BillActionData? = null
)

private val BILL_ACTION_REGEX = Regex("""<!--BILL_ACTION:(\{.*?\})-->""", RegexOption.DOT_MATCHES_ALL)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authDataStore: AuthDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val token = authDataStore.accessToken.first()
            _uiState.value = _uiState.value.copy(accessToken = token)
        }
    }

    fun onInputChange(value: String) {
        _uiState.value = _uiState.value.copy(input = value)
    }

    fun sendMessage() {
        val message = _uiState.value.input.trim()
        val token = _uiState.value.accessToken
        if (message.isBlank() || token.isBlank() || _uiState.value.loading) return

        val userMsg = ChatMessage(role = "user", content = message)
        val streamMsg = ChatMessage(role = "ai", content = "", isStreaming = true)

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg + streamMsg,
            input = "",
            loading = true
        )

        viewModelScope.launch {
            try {
                chatRepository.streamChat(message, token)
                    .catch { e ->
                        val msgs = _uiState.value.messages.toMutableList()
                        msgs[msgs.lastIndex] = ChatMessage(role = "ai", content = "回复失败：${e.message}")
                        _uiState.value = _uiState.value.copy(messages = msgs, loading = false)
                    }
                    .collect { chunk ->
                        val msgs = _uiState.value.messages.toMutableList()
                        val last = msgs.removeAt(msgs.lastIndex)
                        val updated = last.copy(content = last.content + chunk, isStreaming = true)
                        msgs.add(updated)
                        _uiState.value = _uiState.value.copy(messages = msgs)
                    }
                // Stream complete — mark as not streaming, parse BILL_ACTION if present
                val msgs = _uiState.value.messages.toMutableList()
                var pendingBill: BillActionData? = null
                if (msgs.isNotEmpty()) {
                    val last = msgs.last()
                    val match = BILL_ACTION_REGEX.find(last.content)
                    if (match != null) {
                        val cleanContent = last.content.replace(match.value, "").trimEnd()
                        msgs[msgs.lastIndex] = last.copy(content = cleanContent, isStreaming = false)
                        pendingBill = parseBillAction(match.groupValues[1])
                    } else {
                        msgs[msgs.lastIndex] = last.copy(isStreaming = false)
                    }
                }
                _uiState.value = _uiState.value.copy(messages = msgs, loading = false, pendingBillAction = pendingBill)
            } catch (e: Exception) {
                val msgs = _uiState.value.messages.toMutableList()
                if (msgs.isNotEmpty()) {
                    msgs[msgs.lastIndex] = ChatMessage(role = "ai", content = "AI 回复失败，请稍后重试")
                }
                _uiState.value = _uiState.value.copy(messages = msgs, loading = false)
            }
        }
    }

    fun confirmBillAction() {
        val action = _uiState.value.pendingBillAction ?: return
        val token = _uiState.value.accessToken
        viewModelScope.launch {
            try {
                ApiClient.createBill(token, action.category, action.amount, action.note, action.date)
                val confirmMsg = ChatMessage(
                    role = "ai",
                    content = "✅ 已记录 ${action.category} ¥${"%.2f".format(action.amount)}"
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + confirmMsg,
                    pendingBillAction = null
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(pendingBillAction = null)
            }
        }
    }

    fun dismissBillAction() {
        _uiState.value = _uiState.value.copy(pendingBillAction = null)
    }

    private fun parseBillAction(json: String): BillActionData? {
        return try {
            val obj = JSONObject(json)
            BillActionData(
                category = obj.getString("category"),
                amount = obj.getDouble("amount"),
                note = obj.optString("note").takeIf { it.isNotEmpty() },
                date = obj.getString("date")
            )
        } catch (_: Exception) { null }
    }
}
