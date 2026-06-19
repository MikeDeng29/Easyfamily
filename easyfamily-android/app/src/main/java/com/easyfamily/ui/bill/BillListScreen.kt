package com.easyfamily.ui.bill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyfamily.data.BillItemDto
import com.easyfamily.ui.theme.AppPalette

internal fun categoryEmoji(category: String) = when (category) {
    "餐饮" -> "🍜"
    "住房" -> "🏠"
    "交通" -> "🚗"
    "购物" -> "🛍"
    "医疗" -> "💊"
    "娱乐" -> "🎮"
    else   -> "📦"
}

@Composable
fun BillListScreen(
    onStats: () -> Unit,
    viewModel: BillViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("账单", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppPalette.TextPrimary)
            TextButton(onClick = onStats) {
                Text("查看统计", color = AppPalette.Amber, fontWeight = FontWeight.Medium)
            }
        }

        uiState.stats?.let { stats ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.Surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("本月总支出", fontSize = 12.sp, color = AppPalette.TextSecondary)
                        Text(
                            "¥%.2f".format(stats.totalAmount),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppPalette.Coral
                        )
                    }
                    Text("共 ${stats.count} 笔", fontSize = 13.sp, color = AppPalette.TextSecondary)
                }
            }
        }

        if (uiState.bills.isEmpty() && !uiState.loading) {
            Text(
                "暂无账单记录\n在「对话」页告诉 AI 你的消费，即可自动记录",
                fontSize = 14.sp,
                color = AppPalette.TextSecondary,
                lineHeight = 22.sp
            )
        }

        uiState.error?.let {
            Text(it, color = AppPalette.Error, fontSize = 13.sp)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.bills, key = { it.id }) { bill ->
                BillCard(bill = bill, onDelete = { viewModel.deleteBill(bill.id) })
            }
        }
    }
}

@Composable
private fun BillCard(bill: BillItemDto, onDelete: () -> Unit) {
    val isIncome = bill.direction == "income"
    val directionColor = if (isIncome) AppPalette.Success else AppPalette.Coral
    val amountText = if (isIncome) "+¥%.2f".format(bill.amount) else "-¥%.2f".format(bill.amount)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.Surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category emoji
            Text(categoryEmoji(bill.category), fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            // Direction icon badge
            Surface(
                shape = CircleShape,
                color = if (isIncome) Color(0xFFE8F5E9) else AppPalette.SoftCoral,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = if (isIncome) "收入" else "支出",
                    tint = directionColor,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    bill.note?.takeIf { it.isNotBlank() } ?: bill.category,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = AppPalette.TextPrimary
                )
                Text(
                    "${bill.category}  ·  ${bill.billedAt}",
                    fontSize = 12.sp,
                    color = AppPalette.TextSecondary
                )
            }
            Text(
                amountText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = directionColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onDelete,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AppPalette.SoftCoral,
                    contentColor = AppPalette.Error
                )
            ) {
                Text("删除", fontSize = 12.sp)
            }
        }
    }
}
