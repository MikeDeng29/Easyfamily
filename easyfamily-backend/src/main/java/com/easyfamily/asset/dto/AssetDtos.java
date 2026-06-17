package com.easyfamily.asset.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class AssetDtos {

    private AssetDtos() {}

    public static final String ASSET_TYPE_PATTERN =
            "cash|savings|fund|stock|property|vehicle|other";

    public record AssetItem(
            Long id,
            String name,
            String assetType,
            BigDecimal value,
            String note,
            String createdAt
    ) {}

    public record AssetListResponse(
            List<AssetItem> items,
            BigDecimal totalValue
    ) {}

    public record AssetCreateRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank String assetType,
            @NotNull @DecimalMin("0") BigDecimal value,
            @Size(max = 500) String note
    ) {}
}
