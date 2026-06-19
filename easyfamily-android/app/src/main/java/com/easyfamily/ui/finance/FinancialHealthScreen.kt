package com.easyfamily.ui.finance

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyfamily.data.FamilyBillStatsData
import com.easyfamily.data.HealthReportData
import com.easyfamily.ui.theme.AppPalette

private val IncomeGreen = Color(0xFF4CAF50)
private val DangerRed = Color(0xFFE53935)
private val WarnOrange = Color(0xFFFFB74D)

@Composable
fun FinancialHealthScreen(
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    var monthMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadAll() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with month picker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("财务健康", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppPalette.TextPrimary)

            Box {
                TextButton(onClick = { monthMenuExpanded = true }) {
                    Text(uiState.selectedMonth, color = AppPalette.Violet, fontSize = 14.sp)
                    Text(" ▾", color = AppPalette.Violet, fontSize = 12.sp)
                }
                DropdownMenu(expanded = monthMenuExpanded, onDismissRequest = { monthMenuExpanded = false }) {
                    viewModel.recentMonths().forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month) },
                            onClick = {
                                viewModel.selectMonth(month)
                                monthMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Tab
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = AppPalette.Surface,
            contentColor = AppPalette.Coral
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("财务健康") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("家庭账单") })
        }

        when (selectedTab) {
            0 -> HealthTab(uiState = uiState)
            1 -> FamilyBillTab(stats = uiState.familyBillStats)
        }
    }
}

@Composable
private fun HealthTab(uiState: FinanceUiState) {
    when {
        uiState.loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppPalette.Coral)
            }
        }
        uiState.healthReport != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HealthScoreCard(report = uiState.healthReport)
                NetWorthCard(report = uiState.healthReport)
                CashFlowCard(report = uiState.healthReport)
                DebtPressureCard(report = uiState.healthReport)
                if (uiState.healthReport.suggestions.isNotEmpty()) {
                    SuggestionsCard(suggestions = uiState.healthReport.suggestions)
                }
            }
        }
        uiState.error != null -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("😢", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(uiState.error, color = AppPalette.TextSecondary, textAlign = TextAlign.Center)
            }
        }
        else -> {
            ActivationGuide()
        }
    }
}

@Composable
private fun HealthScoreCard(report: HealthReportData) {
    val scoreColor = levelColor(report.healthLevel)
    val animatedProgress by animateFloatAsState(
        targetValue = report.healthScore / 100f,
        animationSpec = tween(800),
        label = "score"
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(120.dp),
                    color = scoreColor.copy(alpha = 0.15f),
                    strokeWidth = 12.dp
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(120.dp),
                    color = scoreColor,
                    strokeWidth = 12.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${report.healthScore}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text("分", fontSize = 12.sp, color = AppPalette.TextSecondary)
                }
            }
            Surface(
                shape = CircleShape,
                color = scoreColor.copy(alpha = 0.1f)
            ) {
                Text(
                    report.healthLevel,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = scoreColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun NetWorthCard(report: HealthReportData) {
    val netWorthColor = if (report.netWorth >= 0) IncomeGreen else AppPalette.Coral
    val totalForRatio = report.totalAssets + report.totalLiabilities
    val assetsRatio = if (totalForRatio > 0) (report.totalAssets / totalForRatio).toFloat() else 1f

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("净资产", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                "¥${formatAmount(report.netWorth)}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = netWorthColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(IncomeGreen, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("总资产", fontSize = 12.sp, color = AppPalette.TextSecondary, modifier = Modifier.weight(1f))
                    Text("¥${formatAmount(report.totalAssets)}", fontWeight = FontWeight.SemiBold, color = IncomeGreen)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(AppPalette.Coral, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("总负债", fontSize = 12.sp, color = AppPalette.TextSecondary, modifier = Modifier.weight(1f))
                    Text("¥${formatAmount(report.totalLiabilities)}", fontWeight = FontWeight.SemiBold, color = AppPalette.Coral)
                }
                // Asset vs liability bar
                Row(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(assetsRatio)
                            .height(10.dp)
                            .background(IncomeGreen, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f - assetsRatio)
                            .height(10.dp)
                            .background(AppPalette.Coral, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun CashFlowCard(report: HealthReportData) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("现金流", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(modifier = Modifier.fillMaxWidth()) {
                CashFlowColumn("月收入", report.monthlyIncome, IncomeGreen, Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).height(44.dp).background(AppPalette.Background))
                CashFlowColumn("月支出", report.monthlyExpense, AppPalette.Coral, Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).height(44.dp).background(AppPalette.Background))
                CashFlowColumn(
                    "净结余",
                    report.netSavings,
                    if (report.netSavings >= 0) IncomeGreen else AppPalette.Coral,
                    Modifier.weight(1f)
                )
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppPalette.Background))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("储蓄率", fontSize = 12.sp, color = AppPalette.TextSecondary)
                    Text(
                        report.savingsRate?.let { "${(it * 100).toInt()}%" } ?: "--",
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeGreen
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("应急基金", fontSize = 12.sp, color = AppPalette.TextSecondary)
                    Text(
                        "%.1f 个月".format(report.emergencyFundMonths),
                        fontWeight = FontWeight.SemiBold,
                        color = emergencyFundColor(report.emergencyFundMonths)
                    )
                }
            }
        }
    }
}

@Composable
private fun CashFlowColumn(title: String, amount: Double, color: Color, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 12.sp, color = AppPalette.TextSecondary)
        Text("¥${formatAmount(amount)}", fontWeight = FontWeight.SemiBold, color = color, fontSize = 13.sp)
    }
}

@Composable
private fun DebtPressureCard(report: HealthReportData) {
    val ratio = report.debtToIncomeRatio
    val ratioColor = when {
        ratio > 0.5 -> DangerRed
        ratio > 0.3 -> WarnOrange
        else -> IncomeGreen
    }
    val animatedRatio by animateFloatAsState(
        targetValue = minOf(ratio.toFloat(), 1f),
        animationSpec = tween(600),
        label = "dti"
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("负债压力", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("月还款额", fontSize = 12.sp, color = AppPalette.TextSecondary)
                    Text("¥${formatAmount(report.totalMonthlyPayment)}", fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("负债收入比", fontSize = 12.sp, color = AppPalette.TextSecondary)
                    Text("${(ratio * 100).toInt()}%", fontWeight = FontWeight.SemiBold, color = ratioColor)
                }
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { animatedRatio },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = ratioColor,
                    trackColor = ratioColor.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }
            if (ratio > 0.5) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⚠️", fontSize = 13.sp)
                    Text(
                        "负债收入比超过50%，建议控制新增负债",
                        fontSize = 13.sp,
                        color = DangerRed
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionsCard(suggestions: List<String>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("AI 建议", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            suggestions.forEach { suggestion ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("💡", fontSize = 15.sp)
                    Text(suggestion, fontSize = 14.sp, color = AppPalette.TextPrimary, lineHeight = 22.sp)
                }
                if (suggestion != suggestions.last()) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppPalette.Background))
                }
            }
        }
    }
}

@Composable
private fun ActivationGuide() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📊", fontSize = 52.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "财务健康评分待激活",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.TextPrimary
        )
        Spacer(modifier = Modifier.height(20.dp))
        ActivationStep("1", "在「账单」中记录本月收支")
        Spacer(modifier = Modifier.height(12.dp))
        ActivationStep("2", "在「家庭资产」中录入房产、存款等")
        Spacer(modifier = Modifier.height(12.dp))
        ActivationStep("3", "在「家庭负债」中填写房贷、车贷等")
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "三项数据录入后，评分自动激活",
            fontSize = 13.sp,
            color = AppPalette.TextSecondary
        )
    }
}

@Composable
private fun ActivationStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = CircleShape,
            color = AppPalette.Violet,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Text(text, fontSize = 15.sp, color = AppPalette.TextPrimary)
    }
}

@Composable
private fun FamilyBillTab(stats: FamilyBillStatsData?) {
    if (stats == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无家庭账单数据", color = AppPalette.TextSecondary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.Surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("家庭总览", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("总收入", fontSize = 12.sp, color = AppPalette.TextSecondary)
                        Text("¥${formatAmount(stats.totalIncome)}", fontWeight = FontWeight.SemiBold, color = IncomeGreen)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("总支出", fontSize = 12.sp, color = AppPalette.TextSecondary)
                        Text("¥${formatAmount(stats.totalExpense)}", fontWeight = FontWeight.SemiBold, color = AppPalette.Coral)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("净结余", fontSize = 12.sp, color = AppPalette.TextSecondary)
                        Text(
                            "¥${formatAmount(stats.netSavings)}",
                            fontWeight = FontWeight.SemiBold,
                            color = if (stats.netSavings >= 0) IncomeGreen else AppPalette.Coral
                        )
                    }
                }
            }
        }

        stats.members.forEach { member ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AppPalette.Surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(member.memberName, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(member.relation, fontSize = 12.sp, color = AppPalette.TextSecondary)
                    }
                    val total = member.income + member.expense
                    if (total > 0) {
                        val incomeRatio = (member.income / total).toFloat()
                        Row(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(incomeRatio)
                                    .height(8.dp)
                                    .background(IncomeGreen, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f - incomeRatio)
                                    .height(8.dp)
                                    .background(AppPalette.Coral, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("收入 ¥${formatAmount(member.income)}", fontSize = 12.sp, color = IncomeGreen)
                        Text("支出 ¥${formatAmount(member.expense)}", fontSize = 12.sp, color = AppPalette.Coral)
                    }
                }
            }
        }
    }
}

private fun levelColor(level: String) = when (level) {
    "危险" -> DangerRed
    "警告" -> WarnOrange
    "良好" -> IncomeGreen
    "优秀" -> AppPalette.Violet
    else -> IncomeGreen
}

private fun emergencyFundColor(months: Double) = when {
    months >= 6 -> IncomeGreen
    months >= 3 -> WarnOrange
    else -> DangerRed
}

private fun formatAmount(value: Double): String {
    return if (value >= 10000) "%.1f万".format(value / 10000) else "%.0f".format(value)
}
