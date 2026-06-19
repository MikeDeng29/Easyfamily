package com.easyfamily.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyfamily.ui.theme.AppPalette

@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = uiState.profile

    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var butlerName by remember { mutableStateOf("") }
    var butlerAvatarId by remember { mutableIntStateOf(1) }
    var butlerPersona by remember { mutableStateOf("warm") }

    // Initialize fields from profile once loaded
    LaunchedEffect(profile) {
        if (profile != null) {
            nickname = profile.nickname ?: ""
            email = profile.email ?: ""
            butlerName = profile.butlerName ?: ""
            butlerAvatarId = profile.butlerAvatarId ?: 1
            butlerPersona = profile.butlerPersona ?: "warm"
        }
    }

    LaunchedEffect(Unit) { viewModel.loadProfile() }

    // Auto-close on save
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("个人信息", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppPalette.TextPrimary)
            if (uiState.saving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AppPalette.Coral, strokeWidth = 2.dp)
            } else {
                Button(
                    onClick = {
                        viewModel.saveAll(nickname, email, butlerName, butlerAvatarId, butlerPersona)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Coral)
                ) {
                    Text("保存", color = Color.White)
                }
            }
        }

        // Butler avatar preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val avatarInfo = butlerAvatarById(butlerAvatarId)
            Surface(
                shape = CircleShape,
                color = avatarInfo.color,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = avatarInfo.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        // Personal info section
        SectionLabel("个人信息")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AppPalette.Surface,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("昵称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "手机号：${profile?.phone ?: "—"}",
                    fontSize = 14.sp,
                    color = AppPalette.TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Butler section
        SectionLabel("我的管家")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AppPalette.Surface,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    "形象",
                    fontSize = 12.sp,
                    color = AppPalette.TextSecondary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                )
                // Avatar picker (horizontal scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BUTLER_AVATARS.forEach { avatar ->
                        val selected = avatar.id == butlerAvatarId
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = if (selected) AppPalette.TextPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .padding(if (selected) 3.dp else 0.dp)
                                .clickable { butlerAvatarId = avatar.id }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = avatar.color,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = avatar.icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = butlerName,
                    onValueChange = { butlerName = it },
                    label = { Text("管家名字") },
                    placeholder = { Text("青鸟管家") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "性格",
                    fontSize = 12.sp,
                    color = AppPalette.TextSecondary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                )

                BUTLER_PERSONAS.forEachIndexed { index, persona ->
                    if (index > 0) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 16.dp).background(AppPalette.Background))
                    }
                    val selected = butlerPersona == persona.key
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (selected) AppPalette.SoftCoral else Color.Transparent)
                            .clickable { butlerPersona = persona.key }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(persona.label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AppPalette.TextPrimary)
                            Text(persona.description, fontSize = 12.sp, color = AppPalette.TextSecondary)
                        }
                        if (selected) {
                            Text("✓", color = AppPalette.Coral, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        uiState.error?.let {
            Text(it, color = AppPalette.Error, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppPalette.TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}
