package com.easyfamily.query.service;

import com.easyfamily.query.config.QueryProperties;
import com.easyfamily.query.dto.QueryDtos.BindingQueryRequest;
import com.easyfamily.query.dto.QueryDtos.BindingQueryResponse;
import org.springframework.stereotype.Service;

@Service
public class QueryFacade {

    private final QueryQuotaService queryQuotaService;
    private final QueryService queryService;
    private final QueryRecordService queryRecordService;
    private final QueryProperties queryProperties;

    public QueryFacade(
            QueryQuotaService queryQuotaService,
            QueryService queryService,
            QueryRecordService queryRecordService,
            QueryProperties queryProperties
    ) {
        this.queryQuotaService = queryQuotaService;
        this.queryService = queryService;
        this.queryRecordService = queryRecordService;
        this.queryProperties = queryProperties;
    }

    public BindingQueryResponse queryBinding(String userId, String ip, BindingQueryRequest request) {
        queryQuotaService.ensureWithinQuota(userId, request.phone(), ip);
        BindingQueryResponse response = queryService.queryBinding(request, queryProperties.cacheTtlSeconds());
        queryRecordService.recordQuery(
                userId,
                "in-memory-cache".equals(response.source()) || "redis-cache".equals(response.source())
        );
        return response;
    }
}
