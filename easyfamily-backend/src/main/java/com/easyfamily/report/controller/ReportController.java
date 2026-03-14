package com.easyfamily.report.controller;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.auth.service.LoginAuditLogService;
import com.easyfamily.query.service.QueryQuotaService;
import com.easyfamily.query.service.QueryRuntimeSettingsService;
import com.easyfamily.report.service.ReportMetricStoreService;
import com.easyfamily.report.service.ReportReadService;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
public class ReportController {

    private final ReportReadService reportReadService;
    private final QueryQuotaService queryQuotaService;
    private final QueryRuntimeSettingsService settingsService;
    private final LoginAuditLogService loginAuditLogService;

    public ReportController(
            ReportReadService reportReadService,
            QueryQuotaService queryQuotaService,
            QueryRuntimeSettingsService settingsService,
            LoginAuditLogService loginAuditLogService
    ) {
        this.reportReadService = reportReadService;
        this.queryQuotaService = queryQuotaService;
        this.settingsService = settingsService;
        this.loginAuditLogService = loginAuditLogService;
    }

    @GetMapping("/reports/dau")
    public ApiResponse<List<Map<String, Object>>> getDauReport() {
        List<Map<String, Object>> rows = reportReadService.dauSnapshot().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(LocalDate::compareTo))
                .map(entry -> Map.of("date", entry.getKey().toString(), "dau", (Object) entry.getValue()))
                .collect(Collectors.toList());
        return ApiResponse.ok(rows);
    }

    @GetMapping("/reports/feature-hot")
    public ApiResponse<List<Map<String, Object>>> getFeatureHotReport() {
        List<Map<String, Object>> rows = reportReadService.featurePvSnapshot().entrySet().stream()
                .map(entry -> Map.of("feature", entry.getKey(), "pv", (Object) entry.getValue()))
                .collect(Collectors.toList());
        return ApiResponse.ok(rows);
    }

    @GetMapping("/reports/query-overview")
    public ApiResponse<Map<String, Object>> queryOverview() {
        ReportMetricStoreService.OverviewMetrics overviewMetrics = reportReadService.overviewTotals();
        int total = overviewMetrics.totalQueryCount();
        int hits = overviewMetrics.cacheHitCount();
        double hitRate = total == 0 ? 0.0 : (double) hits / total;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("totalQueryCount", total);
        payload.put("cacheHitCount", hits);
        payload.put("cacheHitRate", hitRate);
        payload.put("loginUserCount", loginAuditLogService.countDistinctSuccessfulLoginsToday("USER_PHONE"));
        payload.put("dailyQuotaPerUser", queryQuotaService.effectiveDailyQuota());
        payload.put("dailyQuotaPerPhone", settingsService.current().dailyQuotaPerPhone());
        payload.put("dailyQuotaPerIp", settingsService.current().dailyQuotaPerIp());
        payload.put("preferRedisCache", settingsService.current().preferRedisCache());
        payload.put("providerKey", settingsService.current().providerKey());
        payload.put("providerTimeoutMs", settingsService.current().providerTimeoutMs());
        payload.put("providerRetryTimes", settingsService.current().providerRetryTimes());
        payload.put("providerCircuitFailureThreshold", settingsService.current().providerCircuitFailureThreshold());
        payload.put("providerCircuitOpenSeconds", settingsService.current().providerCircuitOpenSeconds());
        return ApiResponse.ok(payload);
    }

    @GetMapping("/reports/login-users-weekly")
    public ApiResponse<List<Map<String, Object>>> loginUsersWeekly() {
        List<Map<String, Object>> rows = loginAuditLogService.countDistinctSuccessfulLoginsLastDays("USER_PHONE", 7)
                .entrySet()
                .stream()
                .map(entry -> Map.of(
                        "date", (Object) entry.getKey().toString(),
                        "loginUserCount", (Object) entry.getValue()
                ))
                .collect(Collectors.toList());
        return ApiResponse.ok(rows);
    }

    @GetMapping("/query-settings")
    public ApiResponse<Map<String, Object>> querySettings() {
        return ApiResponse.ok(settingsService.toMap());
    }

    @PatchMapping("/quota")
    public ApiResponse<Map<String, Object>> updateDailyQuota(
            @RequestParam(required = false) @Min(1) Integer dailyQuotaPerUser,
            @RequestParam(required = false) @Min(1) Integer dailyQuotaPerPhone,
            @RequestParam(required = false) @Min(1) Integer dailyQuotaPerIp,
            @RequestParam(required = false) Boolean preferRedisCache,
            @RequestParam(required = false) String providerKey,
            @RequestParam(required = false) @Min(100) Long providerTimeoutMs,
            @RequestParam(required = false) @Min(0) Integer providerRetryTimes,
            @RequestParam(required = false) @Min(1) Integer providerCircuitFailureThreshold,
            @RequestParam(required = false) @Min(1) Integer providerCircuitOpenSeconds
    ) {
        settingsService.update(
                dailyQuotaPerUser,
                dailyQuotaPerPhone,
                dailyQuotaPerIp,
                preferRedisCache,
                providerKey,
                providerTimeoutMs,
                providerRetryTimes,
                providerCircuitFailureThreshold,
                providerCircuitOpenSeconds
        );
        return ApiResponse.ok(settingsService.toMap());
    }
}
