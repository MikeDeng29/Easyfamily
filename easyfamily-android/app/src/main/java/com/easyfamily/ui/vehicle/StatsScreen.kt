package com.easyfamily.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyfamily.data.CategoryStatDto
import com.easyfamily.ui.theme.AppPalette
import java.math.BigDecimal

@Composable
fun StatsScreen(
    vehicleId: Long,
    onBack: () -> Unit,
    viewModel: VehicleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats = uiState.stats

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← 返回", color = AppPalette.Coral) }

        stats?.let { s ->
            Text("花费统计", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppPalette.TextPrimary)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("总花费", "¥${s.totalCost}", AppPalette.Coral)
                StatCard("保养次数", "${s.totalRecords}", AppPalette.Violet)
                StatCard("项目数", "${s.totalItems}", AppPalette.Amber)
            }

            Text("按分类统计", fontWeight = FontWeight.SemiBold, color = AppPalette.TextPrimary)

            val maxCost = s.byCategory.maxOfOrNull { it.totalCost } ?: BigDecimal.ONE

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(s.byCategory) { cat ->
                    CategoryBar(cat, maxCost)
                }
            }
        } ?: Text("暂无统计数据", color = AppPalette.TextSecondary)
    }
}

@Composable
private fun RowScope.StatCard(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(label, fontSize = 12.sp, color = AppPalette.TextSecondary)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun CategoryBar(cat: CategoryStatDto, maxCost: BigDecimal) {
    val fraction = if (maxCost.compareTo(BigDecimal.ZERO) > 0)
        cat.totalCost.toDouble() / maxCost.toDouble() else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Text(cat.category, fontWeight = FontWeight.Medium, color = AppPalette.TextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                if (cat.diyCount > 0) {
                    Text("${cat.diyCount}项DIY", fontSize = 12.sp, color = AppPalette.Success)
                }
            }
            Text("¥${cat.totalCost}", fontWeight = FontWeight.SemiBold, color = AppPalette.Coral)
        }
        LinearProgressIndicator(
            progress = { fraction.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = AppPalette.Coral,
            trackColor = AppPalette.SoftCoral
        )
        Text(
            "${cat.itemCount} 项 · 占 ${(fraction * 100).toInt()}%",
            fontSize = 12.sp,
            color = AppPalette.TextSecondary
        )
    }
}
