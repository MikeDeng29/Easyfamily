package com.easyfamily.query.service;

import com.easyfamily.query.config.QueryProperties;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class QueryRuntimeSettingsService {

    private final QueryProperties defaults;
    private final AtomicReference<RuntimeSettings> runtime;

    public QueryRuntimeSettingsService(QueryProperties defaults) {
        this.defaults = defaults;
        this.runtime = new AtomicReference<>(new RuntimeSettings(
                defaults.dailyQuotaPerUser(),
                defaults.dailyQuotaPerPhone(),
                defaults.dailyQuotaPerIp(),
                defaults.preferRedisCache(),
                defaults.providerKey(),
                defaults.providerTimeoutMs(),
                defaults.providerRetryTimes(),
                defaults.providerCircuitFailureThreshold(),
                defaults.providerCircuitOpenSeconds()
        ));
    }

    public RuntimeSettings current() {
        return runtime.get();
    }

    public RuntimeSettings update(
            Integer dailyQuotaPerUser,
            Integer dailyQuotaPerPhone,
            Integer dailyQuotaPerIp,
            Boolean preferRedisCache,
            String providerKey,
            Long providerTimeoutMs,
            Integer providerRetryTimes,
            Integer providerCircuitFailureThreshold,
            Integer providerCircuitOpenSeconds
    ) {
        RuntimeSettings current = runtime.get();
        RuntimeSettings next = new RuntimeSettings(
                dailyQuotaPerUser != null ? dailyQuotaPerUser : current.dailyQuotaPerUser(),
                dailyQuotaPerPhone != null ? dailyQuotaPerPhone : current.dailyQuotaPerPhone(),
                dailyQuotaPerIp != null ? dailyQuotaPerIp : current.dailyQuotaPerIp(),
                preferRedisCache != null ? preferRedisCache : current.preferRedisCache(),
                providerKey != null && !providerKey.isBlank() ? providerKey : current.providerKey(),
                providerTimeoutMs != null ? providerTimeoutMs : current.providerTimeoutMs(),
                providerRetryTimes != null ? providerRetryTimes : current.providerRetryTimes(),
                providerCircuitFailureThreshold != null ? providerCircuitFailureThreshold : current.providerCircuitFailureThreshold(),
                providerCircuitOpenSeconds != null ? providerCircuitOpenSeconds : current.providerCircuitOpenSeconds()
        );
        runtime.set(next);
        return next;
    }

    public Map<String, Object> toMap() {
        RuntimeSettings s = runtime.get();
        return Map.of(
                "dailyQuotaPerUser", s.dailyQuotaPerUser(),
                "dailyQuotaPerPhone", s.dailyQuotaPerPhone(),
                "dailyQuotaPerIp", s.dailyQuotaPerIp(),
                "preferRedisCache", s.preferRedisCache(),
                "providerKey", s.providerKey(),
                "providerTimeoutMs", s.providerTimeoutMs(),
                "providerRetryTimes", s.providerRetryTimes(),
                "providerCircuitFailureThreshold", s.providerCircuitFailureThreshold(),
                "providerCircuitOpenSeconds", s.providerCircuitOpenSeconds()
        );
    }

    public record RuntimeSettings(
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
}
