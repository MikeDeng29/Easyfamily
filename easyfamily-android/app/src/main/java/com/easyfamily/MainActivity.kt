package com.easyfamily

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.easyfamily.ui.family.FamilyScreen
import com.easyfamily.ui.login.LoginScreen
import com.easyfamily.ui.phone.PhoneManagementScreen
import com.easyfamily.ui.query.QueryScreen
import com.easyfamily.ui.theme.AppPalette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppPalette.CloudWhite
                ) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    var accessToken by remember { mutableStateOf("") }
    var firstLevelTab by remember { mutableStateOf("home") }

    if (accessToken.isBlank()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            LoginScreen(onLoginSuccess = { token ->
                accessToken = token
                firstLevelTab = "home"
            })
        }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = AppPalette.CloudSurface
            ) {
                NavigationBarItem(
                    selected = firstLevelTab == "home",
                    onClick = { firstLevelTab = "home" },
                    label = { Text("首页", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                    icon = {},
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppPalette.DeepPink,
                        selectedTextColor = AppPalette.DeepPink,
                        indicatorColor = AppPalette.SoftPink,
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
                        selectedIconColor = AppPalette.DeepPink,
                        selectedTextColor = AppPalette.DeepPink,
                        indicatorColor = AppPalette.SoftPink,
                        unselectedIconColor = AppPalette.TextSecondary,
                        unselectedTextColor = AppPalette.TextSecondary
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            when (firstLevelTab) {
                "home" -> QueryScreen(accessToken = accessToken)
                "mine" -> MineScreen(
                    accessToken = accessToken,
                    onLogout = {
                        accessToken = ""
                        firstLevelTab = "home"
                    }
                )
            }
        }
    }
}

@Composable
private fun MineScreen(
    accessToken: String,
    onLogout: () -> Unit
) {
    var secondLevelTab by remember { mutableStateOf("family") }

    Column(
        modifier = Modifier.fillMaxSize(),
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
                    containerColor = if (secondLevelTab == "family") AppPalette.PrimaryPink else AppPalette.SoftPink,
                    contentColor = if (secondLevelTab == "family") AppPalette.CloudSurface else AppPalette.TextPrimary
                )
            ) {
                Text("大家庭")
            }
            Button(
                onClick = { secondLevelTab = "phone-management" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (secondLevelTab == "phone-management") AppPalette.PrimaryPink else AppPalette.SoftPink,
                    contentColor = if (secondLevelTab == "phone-management") AppPalette.CloudSurface else AppPalette.TextPrimary
                )
            ) {
                Text("手机号管理")
            }
            Button(
                onClick = { secondLevelTab = "settings" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (secondLevelTab == "settings") AppPalette.PrimaryPink else AppPalette.SoftPink,
                    contentColor = if (secondLevelTab == "settings") AppPalette.CloudSurface else AppPalette.TextPrimary
                )
            ) {
                Text("设置")
            }
        }

        when (secondLevelTab) {
            "family" -> FamilyScreen(accessToken = accessToken)
            "phone-management" -> PhoneManagementScreen(accessToken = accessToken)
            "settings" -> SettingsScreen(onLogout = onLogout)
        }
    }
}

@Composable
private fun SettingsScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleMedium,
                color = AppPalette.TextPrimary
            )
            Text(
                text = "你可以在这里管理客户端配置。",
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextSecondary
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("退出登录", color = AppPalette.DeepPink)
            }
        }
    }
}
