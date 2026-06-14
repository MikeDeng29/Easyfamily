package com.easyfamily.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyfamily.data.MaintenanceRecordDto
import com.easyfamily.ui.theme.AppPalette
import java.math.BigDecimal

@Composable
fun RecordListScreen(
    vehicleId: Long,
    onAddRecord: (Long) -> Unit,
    onStats: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: VehicleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← 返回", color = AppPalette.Coral) }
            Row {
                TextButton(onClick = { onStats(vehicleId) }) { Text("统计", color = AppPalette.Violet) }
                TextButton(onClick = { onAddRecord(vehicleId) }) { Text("+ 记录", color = AppPalette.Coral) }
            }
        }

        uiState.stats?.let { stats ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.SoftCoral)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("累计花费", fontSize = 12.sp, color = AppPalette.TextSecondary)
                    Text("¥${stats.totalCost}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppPalette.Coral)
                    Text("${stats.totalRecords} 次保养 · ${stats.totalItems} 个项目", fontSize = 13.sp, color = AppPalette.TextSecondary)
                }
            }
        }

        if (uiState.records.isEmpty()) {
            Text("暂无保养记录", color = AppPalette.TextSecondary)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.records) { record ->
                RecordCard(record)
            }
        }
    }
}

@Composable
private fun RecordCard(record: MaintenanceRecordDto) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(record.serviceDate, fontWeight = FontWeight.SemiBold, color = AppPalette.TextPrimary)
                Text("¥${record.totalCost}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppPalette.Coral)
            }
            record.mileageKm?.let { Text("里程: ${it}km", fontSize = 13.sp, color = AppPalette.TextSecondary) }
            record.shopName?.let { Text(it, fontSize = 13.sp, color = AppPalette.TextSecondary) }
            if (record.items.isNotEmpty()) {
                record.items.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("[${item.category}] ${item.itemName}", fontSize = 13.sp, color = AppPalette.TextPrimary)
                        Text("¥${item.cost}", fontSize = 13.sp, color = AppPalette.TextSecondary)
                    }
                }
            }
        }
    }
}
