package com.easyfamily.ui.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Eco
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class ButlerAvatarInfo(
    val id: Int,
    val icon: ImageVector,
    val color: Color
)

val BUTLER_AVATARS = listOf(
    ButlerAvatarInfo(1, Icons.Default.AutoAwesome, Color(0xFF8B5CF6)),  // sparkle, 紫色
    ButlerAvatarInfo(2, Icons.Default.Favorite, Color(0xFFFF7B5C)),     // favorite, 红色
    ButlerAvatarInfo(3, Icons.Default.Star, Color(0xFFFFB74D)),         // star, 橙色
    ButlerAvatarInfo(4, Icons.Default.Eco, Color(0xFF4CAF50)),          // eco, 绿色
    ButlerAvatarInfo(5, Icons.Default.DarkMode, Color(0xFF5C6BC0)),     // dark_mode, 深蓝
    ButlerAvatarInfo(6, Icons.Default.WbSunny, Color(0xFFFF9800)),      // wb_sunny, 橙黄
    ButlerAvatarInfo(7, Icons.Default.Pets, Color(0xFF8D6E63)),         // pets, 棕色
    ButlerAvatarInfo(8, Icons.Default.WorkspacePremium, Color(0xFFEC407A)) // workspace_premium, 粉色
)

fun butlerAvatarById(id: Int) = BUTLER_AVATARS.firstOrNull { it.id == id } ?: BUTLER_AVATARS[0]

data class ButlerPersonaInfo(
    val key: String,
    val label: String,
    val description: String
)

val BUTLER_PERSONAS = listOf(
    ButlerPersonaInfo("warm", "温暖贴心", "语气友好温暖，适合家庭场景"),
    ButlerPersonaInfo("strict", "严谨高效", "回复简洁有条理，专注效率"),
    ButlerPersonaInfo("humorous", "幽默风趣", "聊天轻松活泼，偶尔卖个萌")
)
