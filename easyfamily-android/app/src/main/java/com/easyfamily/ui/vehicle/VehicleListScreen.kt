package com.easyfamily.ui.vehicle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyfamily.data.VehicleItemDto
import com.easyfamily.ui.theme.AppPalette

@Composable
fun VehicleListScreen(
    onAddVehicle: () -> Unit,
    onVehicleClick: (Long) -> Unit,
    viewModel: VehicleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadVehicles() }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "我的车辆",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppPalette.TextPrimary
            )
            TextButton(onClick = onAddVehicle) {
                Text("+ 添加", color = AppPalette.Coral, fontWeight = FontWeight.Medium)
            }
        }

        if (uiState.vehicles.isEmpty() && !uiState.loading) {
            Text("暂无车辆，点击右上角添加", color = AppPalette.TextSecondary)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.vehicles) { vehicle ->
                VehicleCard(vehicle, onClick = { onVehicleClick(vehicle.id) })
            }
        }
    }
}

@Composable
private fun VehicleCard(vehicle: VehicleItemDto, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.Surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(vehicle.plateNumber, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppPalette.TextPrimary)
                Text(
                    "${vehicle.brand} ${vehicle.model}${vehicle.year?.let { " ($it)" } ?: ""}",
                    color = AppPalette.TextSecondary, fontSize = 14.sp
                )
            }
            Text("→", fontSize = 20.sp, color = AppPalette.Coral)
        }
    }
}
