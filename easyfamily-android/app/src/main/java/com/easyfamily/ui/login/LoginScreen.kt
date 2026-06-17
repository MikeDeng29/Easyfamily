package com.easyfamily.ui.login

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyfamily.ui.theme.AppPalette

@Composable
fun LoginScreen(
    onLoginSuccess: (accessToken: String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val testPhone = "13800000000"
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var phone by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val isPhoneValid = phone.length == 11 && phone.all { it.isDigit() }
    val canSendCode = uiState.captchaToken.isNotBlank()
    val isSmsCodeValid = smsCode.length == 6 && smsCode.all { it.isDigit() }
    val canLogin = canSendCode && isPhoneValid && isSmsCodeValid

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.delay(700)
            onLoginSuccess("persisted")
            viewModel.onLoginConsumed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                })
            }
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(AppPalette.Coral, AppPalette.Violet, AppPalette.VioletDark),
                        center = Offset(600f, 200f),
                        radius = 1200f
                    )
                )
        )

        // Decorative blurred circles
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-20).dp)
                .blur(60.dp, BlurredEdgeTreatment.Unbounded)
                .background(AppPalette.Amber.copy(alpha = 0.3f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 40.dp)
                .blur(50.dp, BlurredEdgeTreatment.Unbounded)
                .background(AppPalette.Coral.copy(alpha = 0.25f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Brand section
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🏠", fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "青鸟管家",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "AI 智能家庭守护",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Login card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "手机号登录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.TextPrimary
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { input ->
                            phone = input.filter { it.isDigit() }.take(11)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        label = { Text("手机号") },
                        placeholder = { Text("请输入 11 位手机号") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppPalette.Coral,
                            focusedLabelColor = AppPalette.Coral,
                            cursorColor = AppPalette.Coral
                        )
                    )

                    // Captcha section
                    AnimatedVisibility(
                        visible = uiState.captchaToken.isBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Button(
                            onClick = { viewModel.tapVerifyCaptcha() },
                            enabled = !uiState.loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppPalette.Coral,
                                contentColor = Color.White
                            )
                        ) {
                            if (uiState.loading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text("点击完成人机验证 →", fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState.captchaToken.isNotBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            color = AppPalette.SoftCoral,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
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
                                    onClick = { viewModel.onCaptchaReset() },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 8.dp,
                                        vertical = 0.dp
                                    )
                                ) {
                                    Text("重置", color = AppPalette.Coral)
                                }
                            }
                        }
                    }

                    // SMS code row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = smsCode,
                            onValueChange = { input ->
                                smsCode = input.filter { it.isDigit() }.take(6)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            label = { Text("短信验证码") },
                            placeholder = { Text("6 位验证码") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppPalette.Coral,
                                focusedLabelColor = AppPalette.Coral,
                                cursorColor = AppPalette.Coral
                            )
                        )

                        Button(
                            onClick = {
                                if (!canSendCode || uiState.loading) return@Button
                                if (!isPhoneValid) return@Button
                                viewModel.sendSms(phone)
                            },
                            enabled = canSendCode && !uiState.loading && uiState.smsCooldownSeconds == 0,
                            modifier = Modifier
                                .width(130.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppPalette.Violet,
                                contentColor = Color.White,
                                disabledContainerColor = AppPalette.SoftViolet,
                                disabledContentColor = AppPalette.TextSecondary
                            )
                        ) {
                            if (uiState.loading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (uiState.smsCooldownSeconds > 0) {
                                Text("${uiState.smsCooldownSeconds}s")
                            } else {
                                Text("获取验证码", fontSize = 13.sp)
                            }
                        }
                    }

                    // Login button
                    Button(
                        onClick = {
                            if (!canLogin || uiState.loading) return@Button
                            viewModel.login(phone, smsCode)
                        },
                        enabled = canLogin && !uiState.loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppPalette.CoralDark,
                            contentColor = Color.White,
                            disabledContainerColor = AppPalette.SoftCoral,
                            disabledContentColor = AppPalette.TextSecondary
                        )
                    ) {
                        Text(
                            "登录",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Quick login
                    TextButton(
                        onClick = {
                            if (uiState.loading) return@TextButton
                            viewModel.oneClickLogin(testPhone)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "一键登录（测试）",
                            color = AppPalette.Violet,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = uiState.info,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "登录即代表同意《用户协议》和《隐私政策》",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
