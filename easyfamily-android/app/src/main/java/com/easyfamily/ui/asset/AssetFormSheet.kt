package com.easyfamily.ui.asset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyfamily.data.AssetData
import com.easyfamily.ui.theme.AppPalette

private val ASSET_TYPES = listOf(
    "real_estate" to "不动产",
    "cash" to "现金存款",
    "investment" to "投资理财",
    "vehicle" to "车辆",
    "other" to "其他"
)

fun assetTypeLabel(type: String) = ASSET_TYPES.firstOrNull { it.first == type }?.second ?: "其他"

@Composable
fun AssetFormSheet(
    editItem: AssetData? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, assetType: String, value: Double, note: String?) -> Unit
) {
    var name by remember { mutableStateOf(editItem?.name ?: "") }
    var assetType by remember { mutableStateOf(editItem?.assetType ?: ASSET_TYPES[0].first) }
    var valueText by remember { mutableStateOf(if (editItem != null) "%.2f".format(editItem.value) else "") }
    var note by remember { mutableStateOf(editItem?.note ?: "") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (editItem == null) "添加资产" else "编辑资产",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = AppPalette.TextPrimary
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Box {
            OutlinedTextField(
                value = assetTypeLabel(assetType),
                onValueChange = {},
                label = { Text("类型") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    TextButton(onClick = { typeMenuExpanded = true }) {
                        Text("选择", fontSize = 12.sp, color = AppPalette.Success)
                    }
                }
            )
            DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                ASSET_TYPES.forEach { (key, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { assetType = key; typeMenuExpanded = false }
                    )
                }
            }
        }

        OutlinedTextField(
            value = valueText,
            onValueChange = { valueText = it },
            label = { Text("金额（元）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("备注（可选）") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("取消", color = AppPalette.TextSecondary)
            }
            Button(
                onClick = {
                    val v = valueText.toDoubleOrNull() ?: return@Button
                    if (name.isNotBlank()) {
                        onSave(name.trim(), assetType, v, note.trim().takeIf { it.isNotEmpty() })
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Success),
                enabled = name.isNotBlank() && valueText.toDoubleOrNull() != null
            ) {
                Text("保存", color = AppPalette.TextOnPrimary)
            }
        }
    }
}
