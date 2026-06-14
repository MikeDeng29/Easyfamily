package com.easyfamily.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyfamily.ui.theme.AppPalette

@Composable
fun VehicleFormScreen(
    onBack: () -> Unit,
    viewModel: VehicleViewModel
) {
    var plate by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    val canSave = plate.isNotBlank() && brand.isNotBlank() && model.isNotBlank()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("添加车辆", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppPalette.TextPrimary)

        OutlinedTextField(
            value = plate, onValueChange = { plate = it.take(16) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            label = { Text("车牌号") },
            placeholder = { Text("例: 京A12345") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPalette.Coral, cursorColor = AppPalette.Coral)
        )
        OutlinedTextField(
            value = brand, onValueChange = { brand = it.take(32) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            label = { Text("品牌") },
            placeholder = { Text("例: 丰田") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPalette.Coral, cursorColor = AppPalette.Coral)
        )
        OutlinedTextField(
            value = model, onValueChange = { model = it.take(64) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            label = { Text("型号") },
            placeholder = { Text("例: 卡罗拉") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPalette.Coral, cursorColor = AppPalette.Coral)
        )
        OutlinedTextField(
            value = year, onValueChange = { year = it.filter { c -> c.isDigit() }.take(4) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            label = { Text("年份（选填）") },
            placeholder = { Text("例: 2023") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPalette.Coral, cursorColor = AppPalette.Coral)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.createVehicle(plate, brand, model, year)
                onBack()
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Coral, contentColor = AppPalette.TextOnPrimary)
        ) { Text("保存") }
    }
}
