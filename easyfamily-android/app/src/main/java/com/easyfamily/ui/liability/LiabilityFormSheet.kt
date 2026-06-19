package com.easyfamily.ui.liability

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
import com.easyfamily.data.LiabilityData
import com.easyfamily.ui.theme.AppPalette

private val LIABILITY_TYPES = listOf(
    "mortgage" to "房贷",
    "car_loan" to "车贷",
    "credit_card" to "信用卡",
    "personal_loan" to "个人贷款",
    "other" to "其他"
)

fun liabilityTypeLabel(type: String) = LIABILITY_TYPES.firstOrNull { it.first == type }?.second ?: "其他"

@Composable
fun LiabilityFormSheet(
    editItem: LiabilityData? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, liabilityType: String, balance: Double, monthlyPayment: Double?, interestRate: Double?, note: String?) -> Unit
) {
    var name by remember { mutableStateOf(editItem?.name ?: "") }
    var liabilityType by remember { mutableStateOf(editItem?.liabilityType ?: LIABILITY_TYPES[0].first) }
    var balanceText by remember { mutableStateOf(if (editItem != null) "%.2f".format(editItem.balance) else "") }
    var monthlyText by remember { mutableStateOf(editItem?.monthlyPayment?.let { "%.2f".format(it) } ?: "") }
    var rateText by remember { mutableStateOf(editItem?.interestRate?.let { "%.2f".format(it) } ?: "") }
    var note by remember { mutableStateOf(editItem?.note ?: "") }
    var typeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            if (editItem == null) "添加负债" else "编辑负债",
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
                value = liabilityTypeLabel(liabilityType),
                onValueChange = {},
                label = { Text("类型") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    TextButton(onClick = { typeExpanded = true }) {
                        Text("选择", fontSize = 12.sp, color = AppPalette.Error)
                    }
                }
            )
            DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                LIABILITY_TYPES.forEach { (key, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { liabilityType = key; typeExpanded = false }
                    )
                }
            }
        }

        OutlinedTextField(
            value = balanceText,
            onValueChange = { balanceText = it },
            label = { Text("余额（元）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        OutlinedTextField(
            value = monthlyText,
            onValueChange = { monthlyText = it },
            label = { Text("月还款额（可选）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        OutlinedTextField(
            value = rateText,
            onValueChange = { rateText = it },
            label = { Text("年利率%（可选）") },
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
                    val b = balanceText.toDoubleOrNull() ?: return@Button
                    if (name.isNotBlank()) {
                        onSave(
                            name.trim(),
                            liabilityType,
                            b,
                            monthlyText.toDoubleOrNull(),
                            rateText.toDoubleOrNull(),
                            note.trim().takeIf { it.isNotEmpty() }
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Error),
                enabled = name.isNotBlank() && balanceText.toDoubleOrNull() != null
            ) {
                Text("保存", color = AppPalette.TextOnPrimary)
            }
        }
    }
}
