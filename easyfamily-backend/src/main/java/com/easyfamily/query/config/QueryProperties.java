package com.easyfamily.query.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "easyfamily.query")
public record QueryProperties(
        long cacheTtlSeconds,
        int dailyQuotaPerUser,
        int dailyQuotaPerPhone,
        int dailyQuotaPerIp,
        boolean preferRedisCache,
        String providerKey,
        long providerTimeoutMs,
        int providerRetryTimes,
        int providerCircuitFailureThreshold,
        int providerCircuitOpenSeconds
) {
}
