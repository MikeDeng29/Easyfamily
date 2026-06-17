package com.easyfamily.finance.dto;

import com.easyfamily.asset.dto.AssetDtos.AssetItem;
import com.easyfamily.liability.dto.LiabilityDtos.LiabilityItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

public final class FinanceDtos {

    private FinanceDtos() {}

    public record MyRoleResponse(String role, String headUserId, String headName) {}

    public record PermissionListResponse(List<String> viewers) {}

    public record GrantRequest(
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "phone must be an 11-digit number starting with 1")
            String phone
    ) {}

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
