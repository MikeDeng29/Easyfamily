package com.easyfamily.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.easyfamily.data.ApiClient
import com.easyfamily.data.FamilyMemberItem
import com.easyfamily.data.PhoneItem
import com.easyfamily.ui.theme.AppPalette

private data class FamilyMember(
    val name: String,
    val phone: String,
    val relation: String
)

@Composable
fun FamilyScreen(accessToken: String) {
    var members by remember { mutableStateOf(emptyList<FamilyMember>()) }
    var info by remember { mutableStateOf("加载中...") }

    LaunchedEffect(accessToken) {
        try {
            val myPhones = ApiClient.listMyPhones(accessToken)
            val familyMembers = ApiClient.listFamilyMembers(accessToken)
            members = buildFamilyMembers(myPhones, familyMembers)
            info = if (members.isEmpty()) "暂无家庭成员" else "已同步家庭成员"
        } catch (e: Exception) {
            members = emptyList()
            info = "加载失败：${e.message ?: "unknown"}"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "大家庭",
            style = MaterialTheme.typography.titleMedium,
            color = AppPalette.TextPrimary
        )
        Text(
            text = "本人与家人信息统一展示，便于后续查询和关怀。",
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (members.isEmpty()) {
            Text(
                text = "暂无可展示成员",
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextSecondary
            )
        } else {
            members.forEach { member ->
                FamilyMemberCard(member = member)
            }
        }
        Text(info, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextSecondary)
    }
}

@Composable
private fun FamilyMemberCard(member: FamilyMember) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppPalette.CloudSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
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
                color = if (member.relation == "户主") AppPalette.DeepPink else AppPalette.TextPrimary,
                modifier = Modifier
                    .background(
                        color = if (member.relation == "户主") AppPalette.SoftPink else AppPalette.CloudWhite,
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
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
        FamilyMember(name = "我", phone = maskPhone(it), relation = "户主")
    }

    val cares = familyMembers.map {
        FamilyMember(
            name = it.name.ifBlank { "未命名成员" },
            phone = maskPhone(it.phone),
            relation = "关心对象"
        )
    }

    return listOfNotNull(owner) + cares
}

private fun maskPhone(phone: String): String {
    if (phone.length != 11 || !phone.all { it.isDigit() }) {
        return phone
    }
    return phone.take(3) + "****" + phone.takeLast(4)
}
