package com.easyfamily.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.easyfamily.data.ApiClient
import com.easyfamily.ui.captcha.SlideCaptchaWidget
import com.easyfamily.ui.theme.AppPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (accessToken: String) -> Unit
) {
    val testPhone = "13800000000"
    var phone by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var captchaToken by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var smsCooldownSeconds by remember { mutableStateOf(0) }
    var info by remember { mutableStateOf("请先完成安全校验，再获取短信验证码。") }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val isPhoneValid = phone.length == 11 && phone.all { it.isDigit() }
    val canSendCode = captchaToken.isNotBlank()
    val isSmsCodeValid = smsCode.length == 6 && smsCode.all { it.isDigit() }
    val canLogin = canSendCode && isPhoneValid && isSmsCodeValid

    LaunchedEffect(smsCooldownSeconds) {
        if (smsCooldownSeconds > 0) {
            delay(1000)
            smsCooldownSeconds -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = AppPalette.CloudWhite)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                })
            }
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "欢迎使用 easyfamily",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.TextPrimary
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.CloudSurface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "手机号登录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppPalette.TextPrimary
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { input ->
                            phone = input.filter { it.isDigit() }.take(11)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        label = { Text("手机号") },
                        placeholder = { Text("请输入 11 位手机号") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )

                    if (captchaToken.isBlank()) {
                        SlideCaptchaWidget(
                            onVerified = { token ->
                                captchaToken = token
                                info = "安全校验通过，可以获取验证码。"
                            }
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            color = AppPalette.SoftPink,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "✓ 安全校验已通过",
                                    color = AppPalette.TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                TextButton(
                                    onClick = {
                                        captchaToken = ""
                                        info = "请重新完成人机校验。"
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("重置", color = AppPalette.PrimaryPink)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = smsCode,
                            onValueChange = { input ->
                                smsCode = input.filter { it.isDigit() }.take(6)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("短信验证码") },
                            placeholder = { Text("请输入验证码") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                        )

                        Button(
                            onClick = {
                                if (!canSendCode || loading) return@Button
                                if (!isPhoneValid) {
                                    info = "请输入正确的 11 位手机号。"
                                    return@Button
                                }
                                loading = true
                                scope.launch {
                                    try {
                                        ApiClient.sendSms(phone, captchaToken)
                                        info = "验证码已发送（测试环境默认 123456）。"
                                        smsCooldownSeconds = 30
                                    } catch (e: Exception) {
                                        info = "发送失败：${e.message ?: "unknown"}"
                                    } finally {
                                        loading = false
                                    }
                                }
                            },
                            enabled = canSendCode && !loading && smsCooldownSeconds == 0,
                            modifier = Modifier
                                .width(132.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppPalette.PrimaryPink,
                                contentColor = AppPalette.CloudSurface
                            )
                        ) {
                            if (loading) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
                            } else if (smsCooldownSeconds > 0) {
                                Text("${smsCooldownSeconds}s")
                            } else {
                                Text("获取验证码")
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (!canLogin || loading) return@Button
                            if (!isPhoneValid) {
                                info = "请输入正确的 11 位手机号。"
                                return@Button
                            }
                            if (!isSmsCodeValid) {
                                info = "请输入 6 位短信验证码。"
                                return@Button
                            }
                            loading = true
                            scope.launch {
                                try {
                                    val login = ApiClient.login(phone, smsCode)
                                    info = "登录成功，userId=${login.userId}"
                                    Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                                    delay(700)
                                    onLoginSuccess(login.accessToken)
                                } catch (e: Exception) {
                                    info = "登录失败：${e.message ?: "unknown"}"
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        enabled = canLogin && !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppPalette.DeepPink,
                            contentColor = AppPalette.CloudSurface
                        )
                    ) {
                        Text("登录")
                    }

                    TextButton(
                        onClick = {
                            if (loading) return@TextButton
                            loading = true
                            scope.launch {
                                try {
                                    val oneClickCaptchaToken = ApiClient.verifyCaptcha("android-one-click")
                                    captchaToken = oneClickCaptchaToken
                                    ApiClient.sendSms(testPhone, oneClickCaptchaToken)
                                    val login = ApiClient.login(testPhone, "123456")
                                    phone = testPhone
                                    smsCode = "123456"
                                    info = "一键登录成功，userId=${login.userId}"
                                    onLoginSuccess(login.accessToken)
                                } catch (e: Exception) {
                                    info = "一键登录失败：${e.message ?: "unknown"}"
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    ) {
                        Text("一键登录（测试）")
                    }

                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.TextSecondary
                    )
                }
            }

            Text(
                text = "登录即代表同意《用户协议》和《隐私政策》",
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextSecondary
            )
        }
    }
}
