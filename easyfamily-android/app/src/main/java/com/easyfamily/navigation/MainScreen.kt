package com.easyfamily.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyfamily.ui.bill.BillListScreen
import com.easyfamily.ui.bill.BillStatsScreen
import com.easyfamily.ui.bill.BillViewModel
import com.easyfamily.ui.chat.ChatScreen
import com.easyfamily.ui.family.FamilyScreen
import com.easyfamily.ui.phone.PhoneManagementScreen
import com.easyfamily.ui.query.QueryScreen
import com.easyfamily.ui.theme.AppPalette
import com.easyfamily.ui.vehicle.VehicleListScreen
import com.easyfamily.ui.vehicle.VehicleFormScreen
import com.easyfamily.ui.vehicle.RecordListScreen
import com.easyfamily.ui.vehicle.RecordFormScreen
import com.easyfamily.ui.vehicle.StatsScreen
import com.easyfamily.ui.vehicle.VehicleViewModel
import androidx.hilt.navigation.compose.hiltViewModel

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
            "mine" -> MineScreen(onLogout = onLogout, innerPadding = innerPadding)
        }
    }
}

@Composable
private fun MineScreen(onLogout: () -> Unit, innerPadding: androidx.compose.foundation.layout.PaddingValues) {
    var secondLevelTab by remember { mutableStateOf("family") }

    // Vehicle sub-navigation state
    var vehicleScreen by remember { mutableStateOf("list") }
    var selectedVehicleId by remember { mutableStateOf(0L) }
    val vehicleViewModel: VehicleViewModel = hiltViewModel()

    // Bill sub-navigation state
    var billScreen by remember { mutableStateOf("list") }
    val billViewModel: BillViewModel = hiltViewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { secondLevelTab = "family" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (secondLevelTab == "family") AppPalette.Coral else AppPalette.SoftCoral,
                    contentColor = if (secondLevelTab == "family") AppPalette.TextOnPrimary else AppPalette.TextPrimary
                )
            ) {
                Text("大家庭", fontSize = 12.sp)
            }
            Button(
                onClick = { secondLevelTab = "phone-management" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (secondLevelTab == "phone-management") AppPalette.Violet else AppPalette.SoftViolet,
                    contentColor = if (secondLevelTab == "phone-management") AppPalette.TextOnPrimary else AppPalette.TextPrimary
                )
            ) {
                Text("手机号", fontSize = 12.sp)
            }
            Button(
                onClick = { secondLevelTab = "query" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (secondLevelTab == "query") AppPalette.Coral else AppPalette.SoftCoral,
                    contentColor = if (secondLevelTab == "query") AppPalette.TextOnPrimary else AppPalette.TextPrimary
                )
            ) {
                Text("查询", fontSize = 12.sp)
            }
            Button(
                onClick = {
                    secondLevelTab = "vehicle"
                    vehicleScreen = "list"
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (secondLevelTab == "vehicle") AppPalette.Amber else AppPalette.SoftCoral,
                    contentColor = if (secondLevelTab == "vehicle") AppPalette.TextOnPrimary else AppPalette.TextPrimary
                )
            ) {
                Text("车辆", fontSize = 12.sp)
            }
            Button(
                onClick = {
                    secondLevelTab = "bill"
                    billScreen = "list"
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (secondLevelTab == "bill") AppPalette.Violet else AppPalette.SoftViolet,
                    contentColor = if (secondLevelTab == "bill") AppPalette.TextOnPrimary else AppPalette.TextPrimary
                )
            ) {
                Text("账单", fontSize = 12.sp)
            }
        }

        when (secondLevelTab) {
            "family" -> FamilyScreen()
            "phone-management" -> PhoneManagementScreen()
            "query" -> QueryScreen()
            "vehicle" -> {
                when (vehicleScreen) {
                    "list" -> VehicleListScreen(
                        onAddVehicle = { vehicleScreen = "form" },
                        onVehicleClick = { id ->
                            selectedVehicleId = id
                            vehicleViewModel.selectVehicle(id)
                            vehicleScreen = "records"
                        }
                    )
                    "form" -> VehicleFormScreen(
                        onBack = { vehicleScreen = "list" },
                        viewModel = vehicleViewModel
                    )
                    "records" -> RecordListScreen(
                        vehicleId = selectedVehicleId,
                        onAddRecord = { vehicleScreen = "record-form" },
                        onStats = { vehicleScreen = "stats" },
                        onBack = { vehicleScreen = "list" }
                    )
                    "record-form" -> RecordFormScreen(
                        vehicleId = selectedVehicleId,
                        onBack = { vehicleScreen = "records" },
                        viewModel = vehicleViewModel
                    )
                    "stats" -> StatsScreen(
                        vehicleId = selectedVehicleId,
                        onBack = { vehicleScreen = "records" }
                    )
                }
            }
            "bill" -> {
                when (billScreen) {
                    "list" -> BillListScreen(
                        onStats = { billScreen = "stats" },
                        viewModel = billViewModel
                    )
                    "stats" -> BillStatsScreen(
                        onBack = { billScreen = "list" },
                        viewModel = billViewModel
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("退出登录", color = AppPalette.Error)
        }
    }
}
