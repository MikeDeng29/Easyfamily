package com.easyfamily.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyfamily.data.ApiClient
import com.easyfamily.data.UserProfileData
import com.easyfamily.ui.asset.AssetScreen
import com.easyfamily.ui.bill.BillListScreen
import com.easyfamily.ui.bill.BillStatsScreen
import com.easyfamily.ui.bill.BillViewModel
import com.easyfamily.ui.chat.ChatScreen
import com.easyfamily.ui.family.FamilyScreen
import com.easyfamily.ui.finance.FinancePermissionScreen
import com.easyfamily.ui.finance.FinancialHealthScreen
import com.easyfamily.ui.liability.LiabilityScreen
import com.easyfamily.ui.phone.PhoneManagementScreen
import com.easyfamily.ui.profile.ProfileEditScreen
import com.easyfamily.ui.profile.ProfileViewModel
import com.easyfamily.ui.profile.butlerAvatarById
import com.easyfamily.ui.query.QueryScreen
import com.easyfamily.ui.theme.AppPalette
import com.easyfamily.ui.vehicle.RecordFormScreen
import com.easyfamily.ui.vehicle.RecordListScreen
import com.easyfamily.ui.vehicle.StatsScreen
import com.easyfamily.ui.vehicle.VehicleFormScreen
import com.easyfamily.ui.vehicle.VehicleListScreen
import com.easyfamily.ui.vehicle.VehicleViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(onLogout: () -> Unit) {
    var firstLevelTab by remember { mutableStateOf("chat") }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = AppPalette.Surface) {
                NavigationBarItem(
                    selected = firstLevelTab == "chat",
                    onClick = { firstLevelTab = "chat" },
                    label = { Text("对话", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                    icon = {},
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppPalette.Coral,
                        selectedTextColor = AppPalette.Coral,
                        indicatorColor = AppPalette.SoftCoral,
                        unselectedIconColor = AppPalette.TextSecondary,
                        unselectedTextColor = AppPalette.TextSecondary
                    )
                )
                NavigationBarItem(
                    selected = firstLevelTab == "mine",
                    onClick = { firstLevelTab = "mine" },
                    label = { Text("我的", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                    icon = {},
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppPalette.Coral,
                        selectedTextColor = AppPalette.Coral,
                        indicatorColor = AppPalette.SoftCoral,
                        unselectedIconColor = AppPalette.TextSecondary,
                        unselectedTextColor = AppPalette.TextSecondary
                    )
                )
            }
        }
    ) { innerPadding ->
        when (firstLevelTab) {
            "chat" -> Box(modifier = Modifier.padding(innerPadding)) { ChatScreen() }
            "mine" -> MineSection(onLogout = onLogout, innerPadding = innerPadding)
        }
    }
}

@Composable
private fun MineSection(onLogout: () -> Unit, innerPadding: PaddingValues) {
    var screen by remember { mutableStateOf("home") }
    var vehicleScreen by remember { mutableStateOf("list") }
    var selectedVehicleId by remember { mutableStateOf(0L) }
    val vehicleViewModel: VehicleViewModel = hiltViewModel()
    var billScreen by remember { mutableStateOf("list") }
    val billViewModel: BillViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    when (screen) {
        "home" -> MineHome(
            innerPadding = innerPadding,
            profileViewModel = profileViewModel,
            onNavigate = { dest ->
                screen = dest
                if (dest == "vehicle") vehicleScreen = "list"
                if (dest == "bill") billScreen = "list"
            },
            onLogout = onLogout
        )
        "profile-edit" -> Box(Modifier.padding(innerPadding).fillMaxSize()) {
            ProfileEditScreen(onBack = { screen = "home" }, viewModel = profileViewModel)
        }
        "family" -> Box(Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp)) {
            FamilyScreen()
        }
        "phone" -> Box(Modifier.padding(innerPadding).fillMaxSize()) { PhoneManagementScreen() }
        "query" -> Box(Modifier.padding(innerPadding).fillMaxSize()) { QueryScreen() }
        "vehicle" -> Box(Modifier.padding(innerPadding).fillMaxSize()) {
            when (vehicleScreen) {
                "list" -> VehicleListScreen(
                    onAddVehicle = { vehicleScreen = "form" },
                    onVehicleClick = { id -> selectedVehicleId = id; vehicleViewModel.selectVehicle(id); vehicleScreen = "records" }
                )
                "form" -> VehicleFormScreen(onBack = { vehicleScreen = "list" }, viewModel = vehicleViewModel)
                "records" -> RecordListScreen(
                    vehicleId = selectedVehicleId,
                    onAddRecord = { vehicleScreen = "record-form" },
                    onStats = { vehicleScreen = "stats" },
                    onBack = { vehicleScreen = "list" }
                )
                "record-form" -> RecordFormScreen(vehicleId = selectedVehicleId, onBack = { vehicleScreen = "records" }, viewModel = vehicleViewModel)
                "stats" -> StatsScreen(vehicleId = selectedVehicleId, onBack = { vehicleScreen = "records" })
            }
        }
        "bill" -> Box(Modifier.padding(innerPadding).fillMaxSize()) {
            when (billScreen) {
                "list" -> BillListScreen(onStats = { billScreen = "stats" }, viewModel = billViewModel)
                "stats" -> BillStatsScreen(onBack = { billScreen = "list" }, viewModel = billViewModel)
            }
        }
        "asset" -> Box(Modifier.padding(innerPadding).fillMaxSize()) { AssetScreen() }
        "liability" -> Box(Modifier.padding(innerPadding).fillMaxSize()) { LiabilityScreen() }
        "finance" -> Box(Modifier.padding(innerPadding).fillMaxSize()) { FinancialHealthScreen() }
        "finance_permissions" -> Box(Modifier.padding(innerPadding).fillMaxSize()) { FinancePermissionScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MineHome(
    innerPadding: PaddingValues,
    profileViewModel: ProfileViewModel,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val profile = profileUiState.profile
    var financeRole by remember { mutableStateOf("none") }
    var showFeedback by remember { mutableStateOf(false) }
    val feedbackSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { profileViewModel.loadProfile() }

    // Load finance role once profile is available
    LaunchedEffect(profile) {
        if (profile != null && financeRole == "none") {
            try {
                val t = profileViewModel.token()
                val role = ApiClient.getFinanceRole(t)
                financeRole = role.role
            } catch (_: Exception) {}
        }
    }

    val baseEntries = listOf(
        MenuEntry("family", "大家庭", "🏠", AppPalette.Coral, AppPalette.SoftCoral),
        MenuEntry("phone", "手机号", "📱", AppPalette.Violet, AppPalette.SoftViolet),
        MenuEntry("vehicle", "车辆", "🚗", AppPalette.Amber, Color(0xFFFFF8E1)),
        MenuEntry("bill", "账单", "💰", AppPalette.Violet, AppPalette.SoftViolet),
    )
    val financeEntries = if (financeRole == "head" || financeRole == "viewer") listOf(
        MenuEntry("finance", "财务健康", "📊", AppPalette.Violet, AppPalette.SoftViolet),
        MenuEntry("asset", "家庭资产", "🏛", Color(0xFF2E7D32), Color(0xFFE8F5E9)),
        MenuEntry("liability", "家庭负债", "💳", AppPalette.Error, Color(0xFFFFEBEE)),
    ) else emptyList()
    val permEntry = if (financeRole == "head") listOf(
        MenuEntry("finance_permissions", "财务授权", "🔑", AppPalette.Violet, AppPalette.SoftViolet)
    ) else emptyList()
    val allEntries = baseEntries + financeEntries + permEntry

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ProfileCard(profile = profile, onClick = { onNavigate("profile-edit") })
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (financeRole == "viewer") {
            item {
                Text(
                    "你正在查看家庭财务数据",
                    fontSize = 12.sp,
                    color = AppPalette.TextSecondary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.Surface)
            ) {
                allEntries.forEachIndexed { idx, entry ->
                    MenuRow(entry = entry, onClick = { onNavigate(entry.id) })
                    if (idx < allEntries.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(start = 60.dp))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(start = 60.dp))
                MenuRow(
                    entry = MenuEntry("feedback", "问题反馈", "💬", AppPalette.Coral, AppPalette.SoftCoral),
                    onClick = { showFeedback = true }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text("退出登录", color = AppPalette.Error)
            }
        }
    }

    if (showFeedback) {
        ModalBottomSheet(
            onDismissRequest = { showFeedback = false },
            sheetState = feedbackSheetState
        ) {
            InlineFeedbackSheet(
                profileViewModel = profileViewModel,
                prefillEmail = profile?.email ?: "",
                onDismiss = {
                    scope.launch { feedbackSheetState.hide() }.invokeOnCompletion { showFeedback = false }
                }
            )
        }
    }
}

@Composable
private fun ProfileCard(profile: UserProfileData?, onClick: () -> Unit) {
    val avatarId = profile?.butlerAvatarId ?: 1
    val avatarInfo = butlerAvatarById(avatarId)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.Surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = CircleShape, color = avatarInfo.color, modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = avatarInfo.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(profile?.nickname ?: "青鸟管家用户", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = AppPalette.TextPrimary)
                profile?.phone?.let { Text(it, fontSize = 13.sp, color = AppPalette.TextSecondary) }
                profile?.email?.takeIf { it.isNotEmpty() }?.let { Text(it, fontSize = 12.sp, color = AppPalette.TextSecondary) }
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = AppPalette.TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

private data class MenuEntry(val id: String, val label: String, val emoji: String, val accentColor: Color, val bgColor: Color)

@Composable
private fun MenuRow(entry: MenuEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(entry.bgColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(entry.emoji, fontSize = 16.sp)
        }
        Text(entry.label, modifier = Modifier.weight(1f), fontSize = 15.sp, color = AppPalette.TextPrimary)
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = AppPalette.TextSecondary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun InlineFeedbackSheet(
    profileViewModel: ProfileViewModel,
    prefillEmail: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(prefillEmail) }
    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(submitted) {
        if (submitted) {
            kotlinx.coroutines.delay(1200)
            onDismiss()
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("问题反馈", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppPalette.TextPrimary)

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题（可选）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("联系邮箱（可选）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("问题描述") },
                placeholder = { Text("请描述遇到的问题或建议...", color = AppPalette.TextSecondary) },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                maxLines = 8
            )

            error?.let { Text(it, color = AppPalette.Error, fontSize = 13.sp) }

            Button(
                onClick = {
                    if (description.isBlank()) { error = "请填写问题描述"; return@Button }
                    scope.launch {
                        isSubmitting = true; error = null
                        try {
                            val t = profileViewModel.token()
                            ApiClient.submitFeedback(t, title.trim().takeIf { it.isNotEmpty() }, description.trim(), email.trim().takeIf { it.isNotEmpty() })
                            submitted = true
                        } catch (e: Exception) { error = "提交失败：${e.message}" }
                        finally { isSubmitting = false }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Coral),
                enabled = !isSubmitting && description.isNotBlank()
            ) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("提交反馈", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        AnimatedVisibility(visible = submitted, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier.fillMaxSize().background(AppPalette.Background.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("✅", fontSize = 48.sp)
                    Text("已收到你的反馈，感谢！", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
