package com.easyfamily.ui.bill

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyfamily.data.BillCategoryStatDto
import com.easyfamily.ui.theme.AppPalette

@Composable
fun BillStatsScreen(
    onBack: () -> Unit,
    viewModel: BillViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats = uiState.stats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← 返回", color = AppPalette.Coral)
            }
            Text(
                "支出统计",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppPalette.TextPrimary
            )
        }

        if (stats == null) {
            Text("暂无统计数据", color = AppPalette.TextSecondary)
            return@Column
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.Surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("总支出", fontSize = 12.sp, color = AppPalette.TextSecondary)
                    Text(
                        "¥%.2f".format(stats.totalAmount),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.Coral
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("账单数", fontSize = 12.sp, color = AppPalette.TextSecondary)
                    Text(
                        "${stats.count} 笔",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.TextPrimary
                    )
                }
            }
        }

        Text("分类明细", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.TextPrimary)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(stats.byCategory) { cat ->
                CategoryStatRow(cat = cat, total = stats.totalAmount)
            }
        }
    }
}

@Composable
private fun CategoryStatRow(cat: BillCategoryStatDto, total: Double) {
    val ratio = if (total > 0) (cat.amount / total).toFloat().coerceIn(0f, 1f) else 0f

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.Surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(categoryEmoji(cat.category), fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(cat.category, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppPalette.TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${cat.count} 笔", fontSize = 12.sp, color = AppPalette.TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("¥%.2f".format(cat.amount), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppPalette.TextPrimary)
                    Text("%.0f%%".format(ratio * 100), fontSize = 11.sp, color = AppPalette.TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppPalette.SoftCoral)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AppPalette.Coral)
                )
            }
        }
    }
}
