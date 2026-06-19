package com.easyfamily.ui.asset

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import com.easyfamily.data.AssetData
import com.easyfamily.ui.theme.AppPalette
import kotlinx.coroutines.launch

private val AssetGreen = Color(0xFF2E7D32)
private val AssetGreenLight = Color(0xFF4CAF50)
private val AssetGreenSoft = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetScreen(
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<AssetData?>(null) }
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
            Text("家庭资产", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppPalette.TextPrimary)
            IconButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加", tint = AssetGreen)
            }
        }

        // Summary card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(listOf(AssetGreen, AssetGreenLight)),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("总资产", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                Text(
                    formatAmount(uiState.totalValue),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text("元", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        uiState.error?.let { Text(it, color = AppPalette.Error, fontSize = 13.sp) }

        if (uiState.loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp),
                color = AssetGreen
            )
        } else if (uiState.assets.isEmpty()) {
            AssetEmptyState(onAdd = { showAddSheet = true })
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(uiState.assets, key = { it.id }) { asset ->
                    AssetRow(
                        asset = asset,
                        onClick = { editItem = asset },
                        onDelete = { viewModel.deleteAsset(asset.id) }
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
            AssetFormSheet(
                editItem = null,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
                },
                onSave = { name, type, value, note ->
                    viewModel.createAsset(name, type, value, note)
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
            AssetFormSheet(
                editItem = item,
                onDismiss = { editItem = null },
                onSave = { name, type, value, note ->
                    viewModel.updateAsset(item.id, name, type, value, note)
                    editItem = null
                }
            )
        }
    }
}

@Composable
private fun AssetEmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🏛", fontSize = 48.sp)
        Text(
            "开始记录家庭资产",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.TextPrimary
        )
        Text(
            "录入房产、存款等，计算净资产和财务健康评分",
            fontSize = 14.sp,
            color = AppPalette.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = AssetGreen),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  添加第一条资产", color = Color.White)
        }
    }
}

@Composable
private fun AssetRow(
    asset: AssetData,
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
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AssetGreenSoft,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(assetTypeEmoji(asset.assetType), fontSize = 18.sp)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(asset.name, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = AppPalette.TextPrimary)
                Text(assetTypeLabel(asset.assetType), fontSize = 12.sp, color = AppPalette.TextSecondary)
            }
            Text(
                formatAmount(asset.value),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = AssetGreenLight
            )
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

private fun assetTypeEmoji(type: String) = when (type) {
    "real_estate" -> "🏠"
    "cash" -> "💰"
    "investment" -> "📈"
    "vehicle" -> "🚗"
    else -> "💼"
}

private fun formatAmount(value: Double): String {
    return if (value >= 10000) "%.1f万".format(value / 10000) else "¥%.0f".format(value)
}
