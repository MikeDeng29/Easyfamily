package com.easyfamily.ui.query

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.easyfamily.data.ApiClient
import com.easyfamily.data.RealNameVerifyResult
import com.easyfamily.data.PhoneItem
import com.easyfamily.ui.theme.AppPalette
import kotlinx.coroutines.launch

@Composable
fun QueryScreen(accessToken: String) {
    val customOptionLabel = "新建手机号（手动输入）"

    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var idCardNo by remember { mutableStateOf("") }
    var managedPhones by remember { mutableStateOf(emptyList<PhoneItem>()) }
    var sourceExpanded by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<RealNameVerifyResult?>(null) }
    var info by remember { mutableStateOf("请输入手机号、姓名和身份证号进行实名校验。") }
    var loading by remember { mutableStateOf(false) }
    var loadingPhones by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val nameFocusRequester = remember { FocusRequester() }
    val idCardFocusRequester = remember { FocusRequester() }
    val isPhoneValid = phone.length == 11 && phone.all { it.isDigit() }
    val isNameValid = name.trim().length >= 2
    val isIdCardValid = idCardNo.isBlank() || idCardNo.length == 18 || idCardNo.length == 15

    LaunchedEffect(accessToken) {
        loadingPhones = true
        try {
            managedPhones = ApiClient.listMyPhones(accessToken)
            val preferred = managedPhones.firstOrNull { it.isPrimary }?.phone
                ?: managedPhones.firstOrNull()?.phone
            if (preferred != null) {
                phone = preferred
            }
        } catch (e: Exception) {
            info = "加载我的手机号失败：${e.message ?: "unknown"}"
        } finally {
            loadingPhones = false
        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "手机号实名校验",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = AppPalette.TextPrimary
            )

            // One combined control: choose my phone or type a new phone.
            Box {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { value ->
                        phone = value.filter { it.isDigit() }.take(11)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("查询手机号") },
                    placeholder = { Text("可选择我的手机号，或手动输入新手机号") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    trailingIcon = {
                        TextButton(onClick = { sourceExpanded = true }) {
                            Text("选择")
                        }
                    }
                )
                DropdownMenu(
                    expanded = sourceExpanded,
                    onDismissRequest = { sourceExpanded = false }
                ) {
                    managedPhones
                        .map { it.phone }
                        .distinct()
                        .forEach { itemPhone ->
                            DropdownMenuItem(
                                text = { Text(itemPhone) },
                                onClick = {
                                    phone = itemPhone
                                    sourceExpanded = false
                                }
                            )
                        }
                    DropdownMenuItem(
                        text = { Text(customOptionLabel) },
                        onClick = {
                            phone = ""
                            sourceExpanded = false
                        }
                    )
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            keyboardController?.show()
                        }
                    },
                shape = RoundedCornerShape(14.dp),
                label = { Text("姓名") },
                placeholder = { Text("请输入真实姓名") },
                singleLine = true
            )
            OutlinedTextField(
                value = idCardNo,
                onValueChange = { value ->
                    idCardNo = value.filter { it.isDigit() || it == 'X' || it == 'x' }.take(18)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(idCardFocusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            keyboardController?.show()
                        }
                    },
                shape = RoundedCornerShape(14.dp),
                label = { Text("身份证号（选填）") },
                placeholder = { Text("可不填；填写时请输入15或18位") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Button(
                onClick = {
                    if (loading || loadingPhones || !isPhoneValid || !isNameValid || !isIdCardValid) return@Button
                    loading = true
                    scope.launch {
                        try {
                            result = ApiClient.verifyRealName(
                                accessToken = accessToken,
                                phone = phone,
                                name = name.trim(),
                                idCardNo = idCardNo.uppercase()
                            )
                            info = "实名校验完成"
                        } catch (e: Exception) {
                            info = "校验失败：${e.message ?: "unknown"}"
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading && !loadingPhones && isPhoneValid && isNameValid && isIdCardValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppPalette.PrimaryPink,
                    contentColor = AppPalette.CloudSurface
                )
            ) {
                Text(
                    when {
                        loading -> "校验中..."
                        loadingPhones -> "加载手机号中..."
                        else -> "开始校验"
                    }
                )
            }

            val display = result
            if (display != null) {
                Text("手机号：${display.phone}", style = MaterialTheme.typography.bodyMedium)
                Text("姓名：${display.name}", style = MaterialTheme.typography.bodyMedium)
                Text("身份证号：${display.idCardMasked}", style = MaterialTheme.typography.bodyMedium)
                Text("实名结果：${if (display.verified) "通过" else "未通过"}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "结果来源：${display.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.TextSecondary
                )
            }
            Text(info, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextSecondary)
        }
    }
}
