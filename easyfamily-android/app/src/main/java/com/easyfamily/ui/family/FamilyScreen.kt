package com.easyfamily.ui.family

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyfamily.data.network.FamilyMemberItem
import com.easyfamily.data.network.PhoneItem
import com.easyfamily.ui.theme.AppPalette
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class FamilyMember(
    val memberId: String?,
    val name: String,
    val phone: String,
    val relation: String,
    val isOwner: Boolean = false
)

private val RELATION_OPTIONS = listOf("配偶", "子女", "父母", "兄弟姐妹", "其他")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    viewModel: FamilyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val members = buildFamilyMembers(uiState.myPhones, uiState.familyMembers)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "大家庭",
                style = MaterialTheme.typography.titleMedium,
                color = AppPalette.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showAddSheet = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加成员",
                    tint = AppPalette.Coral
                )
            }
        }

        Text(
            text = "本人与家人信息统一展示，便于后续查询和关怀。",
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        uiState.error?.let { err ->
            Text(
                err,
                color = AppPalette.Error,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (members.isEmpty()) {
            Text(
                text = "暂无可展示成员",
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(members, key = { it.memberId ?: ("owner_" + it.phone) }) { member ->
                    if (member.isOwner) {
                        OwnerCard(member = member)
                    } else {
                        SwipeToDeleteCard(
                            member = member,
                            onDelete = {
                                member.memberId?.let { id -> viewModel.deleteMember(id) }
                            }
                        )
                    }
                }
            }
        }

        Text(
            uiState.info,
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState
        ) {
            AddMemberSheet(
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
                },
                onSave = { name, phone, relation ->
                    viewModel.addMember(name, phone, relation)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
                }
            )
        }
    }
}

@Composable
private fun SwipeToDeleteCard(member: FamilyMember, onDelete: () -> Unit) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val threshold = -180f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPalette.Error, RoundedCornerShape(14.dp))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.White)
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < threshold) onDelete()
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f }
                    ) { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(threshold, 0f)
                    }
                },
            colors = CardDefaults.cardColors(containerColor = AppPalette.Surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            MemberCardContent(member = member)
        }
    }
}

@Composable
private fun OwnerCard(member: FamilyMember) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppPalette.Surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        MemberCardContent(member = member)
    }
}

@Composable
private fun MemberCardContent(member: FamilyMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.titleSmall,
                color = AppPalette.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = member.phone,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextSecondary
            )
        }
        Text(
            text = member.relation,
            style = MaterialTheme.typography.labelLarge,
            color = if (member.isOwner) AppPalette.Coral else AppPalette.TextPrimary,
            modifier = Modifier
                .background(
                    color = if (member.isOwner) AppPalette.SoftCoral else AppPalette.Background,
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun AddMemberSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, relation: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf(RELATION_OPTIONS[0]) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "添加家庭成员",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = AppPalette.TextPrimary
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("姓名") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("手机号") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Box {
            OutlinedTextField(
                value = relation,
                onValueChange = {},
                label = { Text("关系") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    TextButton(onClick = { expanded = true }) {
                        Text("选择", fontSize = 12.sp, color = AppPalette.Coral)
                    }
                }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                RELATION_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { relation = option; expanded = false }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("取消", color = AppPalette.TextSecondary)
            }
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) onSave(name.trim(), phone.trim(), relation)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Coral),
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text("保存", color = AppPalette.TextOnPrimary)
            }
        }
    }
}

private fun buildFamilyMembers(
    myPhones: List<PhoneItem>,
    familyMembers: List<FamilyMemberItem>
): List<FamilyMember> {
    val ownerPhone = myPhones.firstOrNull { it.isPrimary }?.phone
        ?: myPhones.firstOrNull()?.phone

    val owner = ownerPhone?.let {
        FamilyMember(memberId = null, name = "我", phone = maskPhone(it), relation = "户主", isOwner = true)
    }

    val cares = familyMembers.map {
        FamilyMember(
            memberId = it.memberId,
            name = it.name.ifBlank { "未命名成员" },
            phone = maskPhone(it.phone),
            relation = it.relation.ifBlank { "关心对象" }
        )
    }

    return listOfNotNull(owner) + cares
}

private fun maskPhone(phone: String): String {
    if (phone.length != 11 || !phone.all { it.isDigit() }) return phone
    return phone.take(3) + "****" + phone.takeLast(4)
}
