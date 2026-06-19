package com.easyfamily.ui.liability

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyfamily.data.LiabilityData
import com.easyfamily.ui.theme.AppPalette
import kotlinx.coroutines.launch

private val LiabilityRed = Color(0xFFB71C1C)
private val LiabilityRedLight = Color(0xFFEF5350)
private val LiabilityRedSoft = Color(0xFFFFEBEE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiabilityScreen(
    viewModel: LiabilityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<LiabilityData?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("家庭负债", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppPalette.TextPrimary)
            IconButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加", tint = LiabilityRed)
            }
        }

        // Summary card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(listOf(LiabilityRed, LiabilityRedLight)),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(vertical = 16.dp, horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("总负债", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                Text(
                    formatAmount(uiState.totalBalance),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text("元", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "月还款总额：${formatAmount(uiState.totalMonthlyPayment)} 元/月",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        uiState.error?.let { Text(it, color = AppPalette.Error, fontSize = 13.sp) }

        if (uiState.loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp),
                color = LiabilityRed
            )
        } else if (uiState.liabilities.isEmpty()) {
            LiabilityEmptyState(onAdd = { showAddSheet = true })
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(uiState.liabilities, key = { it.id }) { liability ->
                    LiabilityRow(
                        liability = liability,
                        onClick = { editItem = liability },
                        onDelete = { viewModel.deleteLiability(liability.id) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState
        ) {
            LiabilityFormSheet(
                editItem = null,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
                },
                onSave = { name, type, balance, monthly, rate, note ->
                    viewModel.createLiability(name, type, balance, monthly, rate, note)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
                }
            )
        }
    }

    editItem?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { editItem = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            LiabilityFormSheet(
                editItem = item,
                onDismiss = { editItem = null },
                onSave = { name, type, balance, monthly, rate, note ->
                    viewModel.updateLiability(item.id, name, type, balance, monthly, rate, note)
                    editItem = null
                }
            )
        }
    }
}

@Composable
private fun LiabilityEmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("💳", fontSize = 48.sp)
        Text(
            "记录家庭负债",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.TextPrimary
        )
        Text(
            "录入房贷、车贷等，结合资产计算净资产和健康评分",
            fontSize = 14.sp,
            color = AppPalette.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = LiabilityRed),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  添加第一条负债", color = Color.White)
        }
    }
}

@Composable
private fun LiabilityRow(
    liability: LiabilityData,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(LiabilityRedSoft, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(liabilityTypeEmoji(liability.liabilityType), fontSize = 18.sp)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(liability.name, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = AppPalette.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(liabilityTypeLabel(liability.liabilityType), fontSize = 12.sp, color = AppPalette.TextSecondary)
                    liability.monthlyPayment?.let { mp ->
                        if (mp > 0) Text("月还 ${formatAmount(mp)}", fontSize = 12.sp, color = Color(0xFFFF6D00))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatAmount(liability.balance),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = LiabilityRedLight
                )
                liability.interestRate?.let { ir ->
                    if (ir > 0) Text("${ir}%/年", fontSize = 11.sp, color = AppPalette.TextSecondary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = AppPalette.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun liabilityTypeEmoji(type: String) = when (type) {
    "mortgage" -> "🏠"
    "car_loan" -> "🚗"
    "credit_card" -> "💳"
    "personal_loan" -> "👤"
    else -> "📋"
}

private fun formatAmount(value: Double): String {
    return if (value >= 10000) "%.1f万".format(value / 10000) else "¥%.0f".format(value)
}
