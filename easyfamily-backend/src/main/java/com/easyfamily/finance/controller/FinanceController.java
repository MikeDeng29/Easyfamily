package com.easyfamily.finance.controller;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.finance.dto.FinanceDtos.FinancialHealthReport;
import com.easyfamily.finance.service.FinancePermissionService;
import com.easyfamily.finance.service.FinanceRole;
import com.easyfamily.finance.service.FinanceService;
import com.easyfamily.security.AuthContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {

    private final FinanceService financeService;
    private final FinancePermissionService financePermissionService;

    public FinanceController(FinanceService financeService,
                             FinancePermissionService financePermissionService) {
        this.financeService = financeService;
        this.financePermissionService = financePermissionService;
    }

    @GetMapping("/health-report")
    public ApiResponse<FinancialHealthReport> healthReport(
            @RequestParam(required = false) String month
    ) {
        var user = AuthContext.currentUser();
        FinanceRole role = financePermissionService.resolveRole(user.userId(), user.phone());
        if (!role.hasAccess()) {
            throw new BusinessException("FINANCE_ACCESS_DENIED", "no access to family finance");
        }
        String dataUserId = role.dataUserId(user.userId());
        return ApiResponse.ok(financeService.getHealthReport(dataUserId, month));
    }
}
