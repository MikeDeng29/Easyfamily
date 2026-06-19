package com.easyfamily.ui.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyfamily.data.ApiClient
import com.easyfamily.data.local.AuthDataStore
import com.easyfamily.ui.theme.AppPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun FeedbackSheet(
    authDataStore: AuthDataStore,
    prefillEmail: String = "",
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(prefillEmail) }
    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Auto-close after success
    LaunchedEffect(submitted) {
        if (submitted) {
            delay(1200)
            onDismiss()
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "问题反馈",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = AppPalette.TextPrimary
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("联系邮箱（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("问题描述") },
                placeholder = { Text("请描述遇到的问题或建议...", color = AppPalette.TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                maxLines = 8
            )

            error?.let {
                Text(it, color = AppPalette.Error, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    if (description.isBlank()) {
                        error = "请填写问题描述"
                        return@Button
                    }
                    scope.launch {
                        isSubmitting = true
                        error = null
                        try {
                            val token = authDataStore.accessToken.first()
                            ApiClient.submitFeedback(
                                accessToken = token,
                                title = title.trim().takeIf { it.isNotEmpty() },
                                description = description.trim(),
                                email = email.trim().takeIf { it.isNotEmpty() }
                            )
                            submitted = true
                        } catch (e: Exception) {
                            error = "提交失败：${e.message}"
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Coral),
                enabled = !isSubmitting && description.isNotBlank()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("提交反馈", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Success overlay
        AnimatedVisibility(
            visible = submitted,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppPalette.Background.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("✅", fontSize = 48.sp)
                    Text(
                        "已收到你的反馈，感谢！",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.TextPrimary
                    )
                }
            }
        }
    }
}
