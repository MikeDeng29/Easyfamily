package com.easyfamily.bill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public final class BillDtos {

    private BillDtos() {}

    public static final String CATEGORY_PATTERN = "餐饮|住房|交通|购物|医疗|娱乐|其他";

    public record BillItem(
            Long id,
            String category,
            BigDecimal amount,
            String note,
            String billedAt,
            Long createdAt
    ) {}

    public record CreateBillRequest(
            @NotBlank @Pattern(regexp = CATEGORY_PATTERN, message = "category must be one of: " + CATEGORY_PATTERN) String category,
            @NotNull @Positive BigDecimal amount,
            String note,
            @NotBlank String billedAt
    ) {}

    public record BillStatsDto(
            BigDecimal totalAmount,
            int count,
            List<CategoryStat> byCategory
    ) {}

    public record CategoryStat(
            String category,
            BigDecimal amount,
            int count
    ) {}
}
