package com.easyfamily.liability.controller;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.finance.service.FinancePermissionService;
import com.easyfamily.finance.service.FinanceRole;
import com.easyfamily.liability.dto.LiabilityDtos.LiabilityCreateRequest;
import com.easyfamily.liability.dto.LiabilityDtos.LiabilityItem;
import com.easyfamily.liability.dto.LiabilityDtos.LiabilityListResponse;
import com.easyfamily.liability.service.LiabilityService;
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
@RequestMapping("/api/v1/liability")
public class LiabilityController {

    private final LiabilityService liabilityService;
    private final FinancePermissionService financePermissionService;

    public LiabilityController(LiabilityService liabilityService,
                                FinancePermissionService financePermissionService) {
        this.liabilityService = liabilityService;
        this.financePermissionService = financePermissionService;
    }

    @GetMapping
    public ApiResponse<LiabilityListResponse> list() {
        var user = AuthContext.currentUser();
        FinanceRole role = financePermissionService.resolveRole(user.userId(), user.phone());
        if (!role.hasAccess()) {
            throw new BusinessException("FINANCE_ACCESS_DENIED", "no access to family finance");
        }
        String dataUserId = role.dataUserId(user.userId());
        return ApiResponse.ok(liabilityService.list(dataUserId));
    }

    @PostMapping
    public ApiResponse<LiabilityItem> create(@Valid @RequestBody LiabilityCreateRequest request) {
        var user = AuthContext.currentUser();
        FinanceRole role = financePermissionService.resolveRole(user.userId(), user.phone());
        if (!role.hasAccess()) {
            throw new BusinessException("FINANCE_ACCESS_DENIED", "no access to family finance");
        }
        if (!role.isHead()) {
            throw new BusinessException("FINANCE_ACCESS_DENIED", "viewers cannot modify finance data");
        }
        return ApiResponse.ok(liabilityService.create(user.userId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<LiabilityItem> update(
            @PathVariable Long id,
            @Valid @RequestBody LiabilityCreateRequest request
    ) {
        var user = AuthContext.currentUser();
        FinanceRole role = financePermissionService.resolveRole(user.userId(), user.phone());
        if (!role.hasAccess()) {
            throw new BusinessException("FINANCE_ACCESS_DENIED", "no access to family finance");
        }
        if (!role.isHead()) {
            throw new BusinessException("FINANCE_ACCESS_DENIED", "viewers cannot modify finance data");
        }
        return ApiResponse.ok(liabilityService.update(user.userId(), id, request));
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
        liabilityService.delete(user.userId(), id);
        return ApiResponse.ok(null);
    }
}
