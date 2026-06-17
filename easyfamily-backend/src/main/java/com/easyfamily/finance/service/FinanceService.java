package com.easyfamily.finance.service;

import com.easyfamily.asset.dto.AssetDtos.AssetItem;
import com.easyfamily.asset.dto.AssetDtos.AssetListResponse;
import com.easyfamily.asset.service.AssetService;
import com.easyfamily.bill.dto.BillDtos.BillStatsDto;
import com.easyfamily.bill.service.BillService;
import com.easyfamily.finance.dto.FinanceDtos.FinancialHealthReport;
import com.easyfamily.liability.dto.LiabilityDtos.LiabilityListResponse;
import com.easyfamily.liability.service.LiabilityService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class FinanceService {

    private static final Set<String> LIQUID_ASSET_TYPES = Set.of("cash", "savings", "fund", "stock");

    private final BillService billService;
    private final AssetService assetService;
    private final LiabilityService liabilityService;

    public FinanceService(BillService billService, AssetService assetService,
                          LiabilityService liabilityService) {
        this.billService = billService;
        this.assetService = assetService;
        this.liabilityService = liabilityService;
    }

    public FinancialHealthReport getHealthReport(String userId, String month) {
        // Default to current month if not provided
        String effectiveMonth = (month != null && !month.isBlank())
                ? month
                : YearMonth.now().toString();

        // 1. Current month bill (personal)
        BillStatsDto billStats = billService.stats(userId, effectiveMonth);

        // 2. Assets
        AssetListResponse assets = assetService.list(userId);

        // 3. Liabilities
        LiabilityListResponse liabilities = liabilityService.list(userId);

        // 4. Core metrics
        BigDecimal netWorth = assets.totalValue().subtract(liabilities.totalBalance());

        // Liquid assets = cash + savings + fund + stock
        BigDecimal liquidAssets = assets.items().stream()
                .filter(a -> LIQUID_ASSET_TYPES.contains(a.assetType()))
                .map(AssetItem::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Emergency fund months = liquidAssets / monthlyExpense (when expense > 0)
        double emergencyFundMonths;
        if (billStats.totalExpense().compareTo(BigDecimal.ZERO) > 0) {
            emergencyFundMonths = liquidAssets
                    .divide(billStats.totalExpense(), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        } else {
            emergencyFundMonths = 99.0;
        }

        // Debt-to-income ratio = totalMonthlyPayment / monthlyIncome
        double debtToIncomeRatio;
        if (billStats.totalIncome().compareTo(BigDecimal.ZERO) > 0) {
            debtToIncomeRatio = liabilities.totalMonthlyPayment()
                    .divide(billStats.totalIncome(), 4, RoundingMode.HALF_UP)
                    .doubleValue();
        } else {
            debtToIncomeRatio = liabilities.totalMonthlyPayment().compareTo(BigDecimal.ZERO) > 0
                    ? 1.0
                    : 0.0;
        }

        // 5. Health score
        int score = calculateHealthScore(billStats.savingsRate(), emergencyFundMonths, debtToIncomeRatio);
        String level = score >= 80 ? "优秀" : score >= 60 ? "良好" : score >= 40 ? "警告" : "危险";

        // 6. Suggestions
        List<String> suggestions = buildSuggestions(billStats, emergencyFundMonths, debtToIncomeRatio, netWorth);

        return new FinancialHealthReport(
                billStats.totalIncome(),
                billStats.totalExpense(),
                billStats.netSavings(),
                billStats.savingsRate(),
                assets.totalValue(),
                liquidAssets,
                assets.items(),
                liabilities.totalBalance(),
                liabilities.totalMonthlyPayment(),
                debtToIncomeRatio,
                liabilities.items(),
                netWorth,
                emergencyFundMonths,
                score,
                level,
                suggestions
        );
    }

    private int calculateHealthScore(BigDecimal savingsRate, double emergencyMonths, double dtiRatio) {
        int score = 0;

        // Savings rate component (max 40)
        double sr = savingsRate != null ? savingsRate.doubleValue() : 0.0;
        if (sr >= 0.3) {
            score += 40;
        } else if (sr >= 0.2) {
            score += 28;
        } else if (sr >= 0.1) {
            score += 14;
        }

        // Emergency fund component (max 30)
        if (emergencyMonths >= 6) {
            score += 30;
        } else if (emergencyMonths >= 3) {
            score += 20;
        } else if (emergencyMonths >= 1) {
            score += 10;
        }

        // Debt-to-income ratio component (max 30)
        if (dtiRatio <= 0.2) {
            score += 30;
        } else if (dtiRatio <= 0.36) {
            score += 20;
        } else if (dtiRatio <= 0.5) {
            score += 10;
        }

        return score;
    }

    private List<String> buildSuggestions(BillStatsDto billStats, double emergencyMonths,
                                           double dtiRatio, BigDecimal netWorth) {
        List<String> suggestions = new ArrayList<>();

        // Savings rate suggestions
        BigDecimal savingsRate = billStats.savingsRate();
        if (savingsRate == null || savingsRate.compareTo(new BigDecimal("0.1")) < 0) {
            suggestions.add("当前储蓄率偏低，建议检查支出结构，争取将储蓄率提升至收入的10%以上");
        } else if (savingsRate.compareTo(new BigDecimal("0.2")) < 0) {
            suggestions.add("储蓄率尚可，建议继续努力，将储蓄率提升至20%以上以增强财务安全感");
        }

        // Emergency fund suggestions
        if (emergencyMonths < 1) {
            suggestions.add("应急资金严重不足，建议立即建立应急储备，目标为3-6个月生活费");
        } else if (emergencyMonths < 3) {
            suggestions.add("应急资金不足3个月生活费，建议优先积累至3个月以上");
        } else if (emergencyMonths < 6) {
            suggestions.add("应急资金达到基础水平，建议继续积累至6个月生活费以应对更长时间的风险");
        }

        // Debt-to-income suggestions
        if (dtiRatio > 0.5) {
            suggestions.add("月还款负担过重（超过收入50%），建议考虑债务重组或提前还款以减轻压力");
        } else if (dtiRatio > 0.36) {
            suggestions.add("月还款占收入比例较高，建议控制新增负债，逐步降低负债收入比");
        }

        // Net worth suggestions
        if (netWorth.compareTo(BigDecimal.ZERO) < 0) {
            suggestions.add("净资产为负，说明负债超过资产，建议制定还款计划，逐步改善资产负债结构");
        }

        // Positive feedback
        if (suggestions.isEmpty()) {
            suggestions.add("您的财务状况良好，继续保持当前的理财习惯，适时考虑增加投资以实现资产增值");
        }

        return suggestions;
    }
}
