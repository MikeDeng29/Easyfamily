package com.easyfamily.query.service;

import com.easyfamily.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QueryQuotaService {

    private final Map<String, Integer> userQuotaCounter = new ConcurrentHashMap<>();
    private final Map<String, Integer> phoneQuotaCounter = new ConcurrentHashMap<>();
    private final Map<String, Integer> ipQuotaCounter = new ConcurrentHashMap<>();
    private final QueryRuntimeSettingsService settingsService;
    private final RedisQuotaCounterService redisQuotaCounterService;

    public QueryQuotaService(
            QueryRuntimeSettingsService settingsService,
            RedisQuotaCounterService redisQuotaCounterService
    ) {
        this.settingsService = settingsService;
        this.redisQuotaCounterService = redisQuotaCounterService;
    }

    public void ensureWithinQuota(String userId, String phone, String ip) {
        QueryRuntimeSettingsService.RuntimeSettings settings = settingsService.current();
        incrementAndCheck(userQuotaCounter, "user", userId, settings.dailyQuotaPerUser());
        incrementAndCheck(phoneQuotaCounter, "phone", phone, settings.dailyQuotaPerPhone());
        incrementAndCheck(ipQuotaCounter, "ip", ip, settings.dailyQuotaPerIp());
    }

    public int effectiveDailyQuota() {
        return settingsService.current().dailyQuotaPerUser();
    }

    private void incrementAndCheck(Map<String, Integer> bucket, String dimension, String value, int limit) {
        Integer used = incrementFromRedisSafely(dimension, value);
        if (used == null) {
            String key = LocalDate.now() + ":" + value;
            int localUsed = bucket.getOrDefault(key, 0) + 1;
            bucket.put(key, localUsed);
            used = localUsed;
        }

        if (used > limit) {
            throw new BusinessException("QUOTA_EXCEEDED", "daily query quota exceeded: " + dimension);
        }
    }

    private Integer incrementFromRedisSafely(String dimension, String value) {
        try {
            return redisQuotaCounterService.increment(dimension, value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
