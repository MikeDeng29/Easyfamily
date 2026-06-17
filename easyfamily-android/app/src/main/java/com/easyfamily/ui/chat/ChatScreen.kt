package com.easyfamily.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyfamily.ui.theme.AppPalette

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val words = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: return@rememberLauncherForActivityResult
            viewModel.onInputChange(words)
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPalette.Background)
    ) {
        // Top bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppPalette.Surface,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏠", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "青鸟管家",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AppPalette.TextPrimary
                    )
                    Text(
                        "AI 智能助手",
                        fontSize = 12.sp,
                        color = AppPalette.TextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppPalette.SoftViolet
                ) {
                    Text(
                        "配额 3/3",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = AppPalette.Violet,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Chat messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 16.dp
            )
        ) {
            if (uiState.messages.isEmpty()) {
                item {
                    WelcomeMessage()
                }
            }
            items(uiState.messages) { msg ->
                ChatBubble(
                    message = msg.content,
                    isUser = msg.role == "user",
                    isStreaming = msg.isStreaming
                )
            }
            uiState.pendingBillAction?.let { action ->
                item {
                    BillActionCard(
                        action = action,
                        onConfirm = viewModel::confirmBillAction,
                        onDismiss = viewModel::dismissBillAction
                    )
                }
            }
        }

        // Input bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppPalette.Surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = AppPalette.SoftViolet,
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出你想记录的内容")
                        }
                        speechLauncher.launch(intent)
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎤", fontSize = 18.sp)
                    }
                }

                OutlinedTextField(
                    value = uiState.input,
                    onValueChange = viewModel::onInputChange,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    placeholder = { Text("输入或说出你的问题...", fontSize = 14.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            viewModel.sendMessage()
                            keyboardController?.hide()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppPalette.Coral,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = AppPalette.SoftCoral.copy(alpha = 0.5f),
                        unfocusedContainerColor = AppPalette.SoftCoral.copy(alpha = 0.3f)
                    )
                )

                if (uiState.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = AppPalette.Coral
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = if (uiState.input.isNotBlank()) AppPalette.Coral else AppPalette.SoftCoral,
                        onClick = { viewModel.sendMessage() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("↑", color = Color.White, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🏠", fontSize = 48.sp)
        Text(
            "你好，我是青鸟管家",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.TextPrimary
        )
        Text(
            "可以帮你查询手机号、管理家庭成员、\n检查配额，直接告诉我你想做什么吧",
            fontSize = 14.sp,
            color = AppPalette.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SuggestionChip("查一下手机号")
            SuggestionChip("查询今日配额")
            SuggestionChip("添加家庭成员")
        }
    }
}

@Composable
private fun SuggestionChip(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppPalette.SoftCoral,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clickable { /* TODO: auto-fill input */ }
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            fontSize = 13.sp,
            color = AppPalette.Coral,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BillActionCard(
    action: BillActionData,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.SoftCoral)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "💰 记录支出？",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = AppPalette.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text("类别：", fontSize = 13.sp, color = AppPalette.TextSecondary)
                Text(action.category, fontSize = 13.sp, color = AppPalette.TextPrimary)
                Spacer(modifier = Modifier.width(16.dp))
                Text("金额：", fontSize = 13.sp, color = AppPalette.TextSecondary)
                Text("¥${"%.2f".format(action.amount)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.Coral)
            }
            if (!action.note.isNullOrBlank()) {
                Row {
                    Text("备注：", fontSize = 13.sp, color = AppPalette.TextSecondary)
                    Text(action.note, fontSize = 13.sp, color = AppPalette.TextPrimary)
                }
            }
            Row {
                Text("日期：", fontSize = 13.sp, color = AppPalette.TextSecondary)
                Text(action.date, fontSize = 13.sp, color = AppPalette.TextPrimary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消", color = AppPalette.TextSecondary)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Coral)
                ) {
                    Text("确认记录", color = AppPalette.TextOnPrimary)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: String, isUser: Boolean, isStreaming: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = AppPalette.SoftViolet
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🤖", fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) AppPalette.BubbleUser else AppPalette.BubbleAi
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    message,
                    fontSize = 14.sp,
                    color = AppPalette.TextPrimary,
                    lineHeight = 20.sp
                )
                if (isStreaming) {
                    Spacer(modifier = Modifier.height(4.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = AppPalette.Coral
                    )
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = AppPalette.SoftCoral
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("👤", fontSize = 14.sp)
                }
            }
        }
    }
}
