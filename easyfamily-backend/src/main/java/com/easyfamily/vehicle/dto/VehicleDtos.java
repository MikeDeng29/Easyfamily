package com.easyfamily.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class VehicleDtos {

    private VehicleDtos() {}

    public record VehicleItem(
            Long id,
            String plateNumber,
            String brand,
            String model,
            Integer year
    ) {}

    public record VehicleCreateRequest(
            @NotBlank String plateNumber,
            @NotBlank String brand,
            @NotBlank String model,
            Integer year
    ) {}

    public record VehicleUpdateRequest(
            @NotBlank String plateNumber,
            @NotBlank String brand,
            @NotBlank String model,
            Integer year
    ) {}

    public record MaintenanceItemDto(
            Long id,
            String category,
            String itemName,
            BigDecimal cost,
            boolean isDiy,
            String notes
    ) {}

    public record MaintenanceRecordItem(
            Long id,
            Long vehicleId,
            LocalDate serviceDate,
            Integer mileageKm,
            String shopName,
            BigDecimal totalCost,
            String notes,
            List<MaintenanceItemDto> items,
            String createdAt
    ) {}

    public record MaintenanceCreateRequest(
            @NotNull LocalDate serviceDate,
            @PositiveOrZero Integer mileageKm,
            String shopName,
            String notes,
            @NotNull List<MaintenanceItemInput> items
    ) {}

    public record MaintenanceItemInput(
            @NotBlank String category,
            @NotBlank String itemName,
            @NotNull @PositiveOrZero BigDecimal cost,
            boolean isDiy,
            String notes
    ) {}

    public record CategoryStat(
            String category,
            BigDecimal totalCost,
            long itemCount,
            long diyCount
    ) {}

    public record MaintenanceStats(
            BigDecimal totalCost,
            long totalRecords,
            long totalItems,
            List<CategoryStat> byCategory
    ) {}

    public record MaintenanceImportItemResult(
            String category,
            String itemName,
            Double cost
    ) {}

    public record MaintenanceImportResult(
            String plateNumber,
            String brand,
            String model,
            Integer year,
            String serviceDate,
            Integer mileageKm,
            String shopName,
            String notes,
            List<MaintenanceImportItemResult> items
    ) {}
}
