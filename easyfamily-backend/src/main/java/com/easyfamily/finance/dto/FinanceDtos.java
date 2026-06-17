package com.easyfamily.finance.dto;

import com.easyfamily.asset.dto.AssetDtos.AssetItem;
import com.easyfamily.liability.dto.LiabilityDtos.LiabilityItem;

import java.math.BigDecimal;
import java.util.List;

public final class FinanceDtos {

    private FinanceDtos() {}

    public record FinancialHealthReport(
            // Cash flow
            BigDecimal monthlyIncome,
            BigDecimal monthlyExpense,
            BigDecimal netSavings,
            BigDecimal savingsRate,
            // Assets
            BigDecimal totalAssets,
            BigDecimal liquidAssets,
            List<AssetItem> assetBreakdown,
            // Liabilities
            BigDecimal totalLiabilities,
            BigDecimal totalMonthlyPayment,
            double debtToIncomeRatio,
            List<LiabilityItem> liabilityBreakdown,
            // Net worth
            BigDecimal netWorth,
            double emergencyFundMonths,
            // Health score
            int healthScore,
            String healthLevel,
            List<String> suggestions
    ) {}
}
