package com.easyfamily.finance.controller;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.finance.dto.FinanceDtos.FinancialHealthReport;
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

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/health-report")
    public ApiResponse<FinancialHealthReport> healthReport(
            @RequestParam(required = false) String month
    ) {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(financeService.getHealthReport(user.userId(), month));
    }
}
