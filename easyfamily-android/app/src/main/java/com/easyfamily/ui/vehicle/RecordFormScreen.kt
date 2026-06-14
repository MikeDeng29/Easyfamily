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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyfamily.ui.theme.AppPalette

data class ItemInput(val category: String, val itemName: String, val cost: String)

private val CATEGORIES = listOf("机油", "刹车", "轮胎", "空调", "滤芯", "火花塞", "蓄电池", "其他")

@Composable
fun RecordFormScreen(
    vehicleId: Long,
    onBack: () -> Unit,
    viewModel: VehicleViewModel
) {
    var serviceDate by remember { mutableStateOf("") }
    var mileageKm by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<ItemInput>() }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var newItemCat by remember { mutableStateOf("机油") }
    var newItemName by remember { mutableStateOf("") }
    var newItemCost by remember { mutableStateOf("") }

    val isDateValid = serviceDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))

    fun addItem() {
        if (newItemName.isNotBlank() && newItemCost.isNotBlank()) {
            items.add(ItemInput(newItemCat, newItemName, newItemCost))
            newItemName = ""
            newItemCost = ""
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack) { Text("← 返回", color = AppPalette.Coral) }
            }
        }

        item {
            OutlinedTextField(
                value = serviceDate, onValueChange = { serviceDate = it.take(10) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                label = { Text("保养日期") },
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPalette.Coral, cursorColor = AppPalette.Coral)
            )
        }
        item {
            OutlinedTextField(
                value = mileageKm, onValueChange = { mileageKm = it.filter { c -> c.isDigit() }.take(7) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                label = { Text("里程 (km)") },
                placeholder = { Text("选填") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPalette.Coral, cursorColor = AppPalette.Coral)
            )
        }
        item {
            OutlinedTextField(
                value = shopName, onValueChange = { shopName = it.take(128) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                label = { Text("保养店铺") },
                placeholder = { Text("选填") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPalette.Coral, cursorColor = AppPalette.Coral)
            )
        }
        item {
            // Add item section
            Text("保养项目", fontWeight = FontWeight.SemiBold, color = AppPalette.TextPrimary)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { showCategoryMenu = true }) {
                        Text(newItemCat, color = AppPalette.Coral)
                    }
                    DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                        CATEGORIES.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = {
                                newItemCat = cat
                                showCategoryMenu = false
                            })
                        }
                    }
                    OutlinedTextField(
                        value = newItemName, onValueChange = { newItemName = it.take(32) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("项目名称") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPalette.Coral, cursorColor = AppPalette.Coral)
                    )
                    OutlinedTextField(
                        value = newItemCost, onValueChange = { newItemCost = it.filter { c -> c.isDigit() || c == '.' }.take(10) },
                        modifier = Modifier.weight(0.6f),
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("费用") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPalette.Coral, cursorColor = AppPalette.Coral)
                    )
                }
                Button(
                    onClick = { addItem() },
                    enabled = newItemName.isNotBlank() && newItemCost.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Violet, contentColor = AppPalette.TextOnPrimary)
                ) { Text("+ 添加项目") }
            }
        }
        itemsIndexed(items) { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("[${item.category}] ${item.itemName} - ¥${item.cost}", color = AppPalette.TextPrimary, fontSize = 14.sp)
                TextButton(onClick = { items.removeAt(index) }) { Text("删除", color = AppPalette.Error) }
            }
        }
        item {
            OutlinedTextField(
                value = notes, onValueChange = { notes = it.take(200) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                label = { Text("备注（选填）") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPalette.Coral, cursorColor = AppPalette.Coral)
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val itemTriples = items.map { Triple(it.category, it.itemName, it.cost) }
                    viewModel.createRecord(vehicleId, serviceDate, mileageKm, shopName, notes, itemTriples)
                    onBack()
                },
                enabled = isDateValid && items.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Coral, contentColor = AppPalette.TextOnPrimary)
            ) { Text("保存保养记录") }
        }
    }
}
