package com.easyfamily.bill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public final class BillDtos {

    private BillDtos() {}

    public static final String DIRECTION_PATTERN = "income|expense";

    public static final String CATEGORY_PATTERN =
            "餐饮|住房|交通|购物|医疗|娱乐|教育|其他|工资|奖金|理财|租金|副业|其他收入";

    public record BillItem(
            Long id,
            String direction,
            String category,
            BigDecimal amount,
            String note,
            String billedAt,
            Long createdAt
    ) {}

    public record CreateBillRequest(
            @NotBlank @Pattern(regexp = DIRECTION_PATTERN, message = "direction must be income or expense")
            String direction,
            @NotBlank @Pattern(regexp = CATEGORY_PATTERN, message = "category must be one of the supported values")
            String category,
            @NotNull @Positive BigDecimal amount,
            String note,
            @NotBlank String billedAt
    ) {}

    public record BillStatsDto(
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal netSavings,
            BigDecimal savingsRate,
            int count,
            List<CategoryStat> byCategory
    ) {}

    public record CategoryStat(
            String direction,
            String category,
            BigDecimal amount,
            int count
    ) {}

    public record MonthlyTrendItem(
            String month,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal netSavings
    ) {}

    public record SecurityReportDto(
            boolean hasEnoughData,
            String period,
            BigDecimal avgMonthlyIncome,
            BigDecimal avgMonthlyExpense,
            BigDecimal avgMonthlySavings,
            BigDecimal savingsRate,
            int savingsRateStatus,
            BigDecimal emergencyFundMonths,
            int emergencyFundStatus,
            int healthScore,
            String healthLevel
    ) {}
}
