package com.easyfamily.ui.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.easyfamily.data.ApiClient
import com.easyfamily.data.PhoneItem
import com.easyfamily.ui.theme.AppPalette
import kotlinx.coroutines.launch

@Composable
fun PhoneManagementScreen(accessToken: String) {
    var phone by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var phones by remember { mutableStateOf(emptyList<PhoneItem>()) }
    var info by remember { mutableStateOf("加载中...") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val phoneFocusRequester = remember { FocusRequester() }

    fun refreshPhones() {
        scope.launch {
            loading = true
            try {
                phones = ApiClient.listMyPhones(accessToken)
                info = "号码列表已刷新"
            } catch (e: Exception) {
                info = "刷新失败：${e.message ?: "unknown"}"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(accessToken) {
        refreshPhones()
    }

    LaunchedEffect(Unit) {
        phoneFocusRequester.requestFocus()
        keyboardController?.show()
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.CloudSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "我的手机号管理",
                style = MaterialTheme.typography.titleMedium,
                color = AppPalette.TextPrimary
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() }.take(11) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(phoneFocusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            keyboardController?.show()
                        }
                    },
                shape = RoundedCornerShape(14.dp),
                label = { Text("手机号") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                value = smsCode,
                onValueChange = { smsCode = it.filter { c -> c.isDigit() }.take(6) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                label = { Text("验证码（绑定时需要）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                ApiClient.bindPhone(accessToken, phone, smsCode)
                                info = "绑定成功"
                                refreshPhones()
                            } catch (e: Exception) {
                                info = "绑定失败：${e.message ?: "unknown"}"
                            }
                        }
                    },
                    enabled = phone.length == 11 && smsCode.isNotBlank() && !loading,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppPalette.PrimaryPink,
                        contentColor = AppPalette.CloudSurface
                    )
                ) { Text("绑定") }

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                ApiClient.unbindPhone(accessToken, phone)
                                info = "解绑成功"
                                refreshPhones()
                            } catch (e: Exception) {
                                info = "解绑失败：${e.message ?: "unknown"}"
                            }
                        }
                    },
                    enabled = phone.length == 11 && !loading,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppPalette.DeepPink,
                        contentColor = AppPalette.CloudSurface
                    )
                ) { Text("解绑") }
            }

            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            ApiClient.setPrimaryPhone(accessToken, phone)
                            info = "已切换主号"
                            refreshPhones()
                        } catch (e: Exception) {
                            info = "切换主号失败：${e.message ?: "unknown"}"
                        }
                    }
                },
                enabled = phone.length == 11 && !loading
            ) {
                Text("设为主号")
            }

            Text("当前号码列表：", style = MaterialTheme.typography.bodyMedium)
            phones.forEach { item ->
                Text(
                    text = "${item.phone}  ${if (item.isPrimary) "[主号]" else ""}  ${item.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.TextPrimary
                )
            }
            Text(info, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextSecondary)
        }
    }
}
