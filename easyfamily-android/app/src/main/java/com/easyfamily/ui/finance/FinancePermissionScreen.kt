package com.easyfamily.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyfamily.ui.theme.AppPalette
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancePermissionScreen(
    viewModel: FinancePermissionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showGrantSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("财务授权", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppPalette.TextPrimary)
            if (uiState.role == "head") {
                IconButton(onClick = { showGrantSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "添加授权", tint = AppPalette.Violet)
                }
            }
        }

        when {
            uiState.loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppPalette.Violet)
                }
            }
            uiState.role == "head" -> {
                HeadContent(
                    viewers = uiState.viewers,
                    error = uiState.error,
                    onRevoke = { viewModel.revokePermission(it) }
                )
            }
            else -> {
                NoPermissionView()
            }
        }
    }

    if (showGrantSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGrantSheet = false },
            sheetState = sheetState
        ) {
            GrantPermissionSheet(
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showGrantSheet = false }
                },
                onGrant = { phone ->
                    viewModel.grantPermission(
                        phone,
                        onSuccess = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showGrantSheet = false }
                        },
                        onError = { /* error displayed in sheet */ }
                    )
                }
            )
        }
    }
}

@Composable
private fun HeadContent(
    viewers: List<String>,
    error: String?,
    onRevoke: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Description
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppPalette.SoftViolet)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = AppPalette.Violet, modifier = Modifier.size(24.dp))
                Text(
                    "已授权以下成员查看你的家庭财务数据",
                    fontSize = 14.sp,
                    color = AppPalette.TextPrimary
                )
            }
        }

        error?.let { Text(it, color = AppPalette.Error, fontSize = 13.sp) }

        Text("已授权成员", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.TextSecondary)

        if (viewers.isEmpty()) {
            Text(
                "暂未授权任何人查看家庭财务",
                fontSize = 14.sp,
                color = AppPalette.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(viewers) { phone ->
                    ViewerRow(phone = phone, onRevoke = { onRevoke(phone) })
                }
            }
        }
    }
}

@Composable
private fun ViewerRow(phone: String, onRevoke: () -> Unit) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val threshold = -160f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPalette.Error, RoundedCornerShape(12.dp))
    ) {
        Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "撤销", tint = Color.White)
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < threshold) onRevoke()
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f }
                    ) { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(threshold, 0f)
                    }
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppPalette.Surface)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = AppPalette.SoftViolet,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("👤", fontSize = 14.sp)
                    }
                }
                Text(phone, fontSize = 15.sp, color = AppPalette.TextPrimary, modifier = Modifier.weight(1f))
                Text("向左滑动撤销", fontSize = 11.sp, color = AppPalette.TextSecondary)
            }
        }
    }
}

@Composable
private fun NoPermissionView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = AppPalette.TextSecondary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("无权限", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "只有户主才能管理财务授权",
            fontSize = 14.sp,
            color = AppPalette.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GrantPermissionSheet(
    onDismiss: () -> Unit,
    onGrant: (String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("添加财务授权", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppPalette.TextPrimary)
        Text("被授权成员可查看你的家庭财务数据，但无法修改", fontSize = 13.sp, color = AppPalette.TextSecondary)

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; error = null },
            label = { Text("手机号") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        error?.let { Text(it, color = AppPalette.Error, fontSize = 13.sp) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("取消", color = AppPalette.TextSecondary)
            }
            Button(
                onClick = {
                    if (phone.length == 11) onGrant(phone.trim())
                    else error = "请输入有效的11位手机号"
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Violet),
                enabled = phone.isNotBlank()
            ) {
                Text("授权", color = Color.White)
            }
        }
    }
}
