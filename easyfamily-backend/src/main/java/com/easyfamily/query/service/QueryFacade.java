package com.easyfamily.query.service;

import com.easyfamily.phone.service.PhoneManagementService;
import com.easyfamily.query.config.QueryProperties;
import com.easyfamily.query.dto.QueryDtos.RealNameVerifyRequest;
import com.easyfamily.query.dto.QueryDtos.RealNameVerifyResponse;
import org.springframework.stereotype.Service;

@Service
public class QueryFacade {

    private final QueryQuotaService queryQuotaService;
    private final QueryService queryService;
    private final QueryRecordService queryRecordService;
    private final QueryProperties queryProperties;
    private final PhoneManagementService phoneManagementService;

    public QueryFacade(
            QueryQuotaService queryQuotaService,
            QueryService queryService,
            QueryRecordService queryRecordService,
            QueryProperties queryProperties,
            PhoneManagementService phoneManagementService
    ) {
        this.queryQuotaService = queryQuotaService;
        this.queryService = queryService;
        this.queryRecordService = queryRecordService;
        this.queryProperties = queryProperties;
        this.phoneManagementService = phoneManagementService;
    }

    public RealNameVerifyResponse verifyRealName(String userId, String loginPhone, String ip, RealNameVerifyRequest request) {
        long start = System.currentTimeMillis();
        phoneManagementService.ensurePhoneOwnedByUser(userId, loginPhone, request.phone());
        queryQuotaService.ensureWithinQuota(userId, request.phone(), ip);
        RealNameVerifyResponse response = queryService.verifyRealName(request, queryProperties.cacheTtlSeconds());
        boolean cacheHit = "redis-cache".equals(response.source());
        int latencyMs = Math.toIntExact(System.currentTimeMillis() - start);
        queryRecordService.recordQuery(
                userId,
                loginPhone,
                request.phone(),
                "REAL_NAME",
                response.source(),
                cacheHit,
                "OK",
                latencyMs,
                ip
        );
        return response;
    }
}
