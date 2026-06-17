package com.easyfamily.asset.controller;

import com.easyfamily.asset.dto.AssetDtos.AssetCreateRequest;
import com.easyfamily.asset.dto.AssetDtos.AssetItem;
import com.easyfamily.asset.dto.AssetDtos.AssetListResponse;
import com.easyfamily.asset.service.AssetService;
import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.finance.service.FinancePermissionService;
import com.easyfamily.finance.service.FinanceRole;
import com.easyfamily.security.AuthContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/asset")
public class AssetController {

    private final AssetService assetService;
    private final FinancePermissionService financePermissionService;

    public AssetController(AssetService assetService,
                           FinancePermissionService financePermissionService) {
        this.assetService = assetService;
        this.financePermissionService = financePermissionService;
    }

    @GetMapping
    public ApiResponse<AssetListResponse> list() {
        var user = AuthContext.currentUser();
        FinanceRole role = financePermissionService.resolveRole(user.userId(), user.phone());
        if (!role.hasAccess()) {
            throw new BusinessException("FINANCE_ACCESS_DENIED", "no access to family finance");
        }
        String dataUserId = role.dataUserId(user.userId());
        return ApiResponse.ok(assetService.list(dataUserId));
    }

    @PostMapping
    public ApiResponse<AssetItem> create(@Valid @RequestBody AssetCreateRequest request) {
        var user = AuthContext.currentUser();
        FinanceRole role = financePermissionService.resolveRole(user.userId(), user.phone());
        if (!role.hasAccess()) {
            throw new BusinessException("FINANCE_ACCESS_DENIED", "no access to family finance");
        }
        if (!role.isHead()) {
            throw new BusinessException("FINANCE_ACCESS_DENIED", "viewers cannot modify finance data");
        }
        return ApiResponse.ok(assetService.create(user.userId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AssetItem> update(
            @PathVariable Long id,
            @Valid @RequestBody AssetCreateRequest request
    ) {
        var user = AuthContext.currentUser();
        FinanceRole role = financePermissionService.resolveRole(user.userId(), user.phone());
        if (!role.hasAccess()) {
            throw new BusinessException("FINANCE_ACCESS_DENIED", "no access to family finance");
        }
        if (!role.isHead()) {
            throw new BusinessException("FINANCE_ACCESS_DENIED", "viewers cannot modify finance data");
        }
        return ApiResponse.ok(assetService.update(user.userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        var user = AuthContext.currentUser();
        FinanceRole role = financePermissionService.resolveRole(user.userId(), user.phone());
        if (!role.hasAccess()) {
            throw new BusinessException("FINANCE_ACCESS_DENIED", "no access to family finance");
        }
        if (!role.isHead()) {
            throw new BusinessException("FINANCE_ACCESS_DENIED", "viewers cannot modify finance data");
        }
        assetService.delete(user.userId(), id);
        return ApiResponse.ok(null);
    }
}
