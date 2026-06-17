package com.easyfamily.bill.controller;

import com.easyfamily.bill.dto.BillDtos.BillItem;
import com.easyfamily.bill.dto.BillDtos.BillStatsDto;
import com.easyfamily.bill.dto.BillDtos.CreateBillRequest;
import com.easyfamily.bill.dto.BillDtos.FamilyBillStats;
import com.easyfamily.bill.dto.BillDtos.MonthlyTrendItem;
import com.easyfamily.bill.dto.BillDtos.SecurityReportDto;
import com.easyfamily.bill.service.BillService;
import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.security.AuthContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bill")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping
    public ApiResponse<BillItem> create(@Valid @RequestBody CreateBillRequest request) {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(billService.create(user.userId(), request));
    }

    @GetMapping
    public ApiResponse<List<BillItem>> list(@RequestParam(required = false) String month) {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(billService.list(user.userId(), month));
    }

    @PutMapping("/{id}")
    public ApiResponse<BillItem> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateBillRequest request
    ) {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(billService.update(user.userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        var user = AuthContext.currentUser();
        billService.delete(user.userId(), id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/stats")
    public ApiResponse<BillStatsDto> stats(@RequestParam(required = false) String month) {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(billService.stats(user.userId(), month));
    }

    @GetMapping("/monthly-trend")
    public ApiResponse<List<MonthlyTrendItem>> monthlyTrend(
            @RequestParam(defaultValue = "6") int months
    ) {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(billService.getMonthlyTrend(user.userId(), Math.min(months, 12)));
    }

    @GetMapping("/security-report")
    public ApiResponse<SecurityReportDto> securityReport() {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(billService.getSecurityReport(user.userId()));
    }

    @GetMapping("/family-stats")
    public ApiResponse<FamilyBillStats> familyStats(@RequestParam(required = false) String month) {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(billService.familyStats(user.userId(), month));
    }
}
