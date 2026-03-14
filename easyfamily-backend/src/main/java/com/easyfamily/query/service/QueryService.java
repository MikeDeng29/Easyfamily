package com.easyfamily.query.service;

import com.easyfamily.query.cache.QueryCacheService.CachedRealName;
import com.easyfamily.query.cache.RedisQueryCacheService;
import com.easyfamily.query.dto.QueryDtos.RealNameVerifyRequest;
import com.easyfamily.query.dto.QueryDtos.RealNameVerifyResponse;
import com.easyfamily.query.provider.BindingQueryProviderRouter;
import com.easyfamily.query.provider.ProviderExecutor;
import org.springframework.stereotype.Service;

@Service
public class QueryService {

    private final RedisQueryCacheService redisQueryCacheService;
    private final BindingQueryProviderRouter providerRouter;
    private final QueryRuntimeSettingsService settingsService;
    private final ProviderExecutor providerExecutor;

    public QueryService(
            RedisQueryCacheService redisQueryCacheService,
            BindingQueryProviderRouter providerRouter,
            QueryRuntimeSettingsService settingsService,
            ProviderExecutor providerExecutor
    ) {
        this.redisQueryCacheService = redisQueryCacheService;
        this.providerRouter = providerRouter;
        this.settingsService = settingsService;
        this.providerExecutor = providerExecutor;
    }

    public RealNameVerifyResponse verifyRealName(RealNameVerifyRequest request, long cacheTtlSeconds) {
        String cacheKey = "real-name:" + request.phone() + ":" + request.name() + ":" + request.idCardNo();

        CachedRealName redisCached = getFromRedisSafely(cacheKey);
        if (redisCached != null) {
            return new RealNameVerifyResponse(
                    request.phone(),
                    request.name(),
                    maskIdCard(request.idCardNo()),
                    redisCached.verified(),
                    "redis-cache",
                    System.currentTimeMillis()
            );
        }

        String providerKey = settingsService.current().providerKey();
        var provider = providerRouter.resolve(providerKey);
        var providerResult = providerExecutor.executeWithGuards(
                providerKey,
                () -> provider.verifyRealName(request.phone(), request.name(), request.idCardNo())
        );
        CachedRealName fresh = new CachedRealName(providerResult.verified());
        putToRedisSafely(cacheKey, fresh, cacheTtlSeconds);
        return new RealNameVerifyResponse(
                request.phone(),
                request.name(),
                maskIdCard(request.idCardNo()),
                providerResult.verified(),
                providerResult.providerName(),
                System.currentTimeMillis()
        );
    }

    private CachedRealName getFromRedisSafely(String cacheKey) {
        if (!settingsService.current().preferRedisCache()) {
            return null;
        }
        try {
            return redisQueryCacheService.get(cacheKey).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void putToRedisSafely(String cacheKey, CachedRealName value, long ttlSeconds) {
        if (!settingsService.current().preferRedisCache()) {
            return;
        }
        try {
            redisQueryCacheService.put(cacheKey, value, ttlSeconds);
        } catch (Exception ignored) {
            // Fallback to in-memory only when Redis is unavailable.
        }
    }

    private String maskIdCard(String idCardNo) {
        if (idCardNo == null || idCardNo.length() < 8) {
            return "****";
        }
        return idCardNo.substring(0, 4) + "**********" + idCardNo.substring(idCardNo.length() - 4);
    }
}
