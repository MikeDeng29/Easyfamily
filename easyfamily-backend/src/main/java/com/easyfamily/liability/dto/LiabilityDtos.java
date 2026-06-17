package com.easyfamily.liability.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class LiabilityDtos {

    private LiabilityDtos() {}

    public record LiabilityItem(
            Long id,
            String name,
            String liabilityType,
            BigDecimal balance,
            BigDecimal monthlyPayment,
            BigDecimal interestRate,
            String note,
            String createdAt
    ) {}

    public record LiabilityListResponse(
            List<LiabilityItem> items,
            BigDecimal totalBalance,
            BigDecimal totalMonthlyPayment
    ) {}

    public record LiabilityCreateRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank String liabilityType,
            @NotNull @DecimalMin("0") BigDecimal balance,
            @DecimalMin("0") BigDecimal monthlyPayment,
            @DecimalMin("0") @DecimalMax("100") BigDecimal interestRate,
            @Size(max = 500) String note
    ) {}
}
