package com.easyfamily.query.service;

import com.easyfamily.query.cache.InMemoryQueryCacheService;
import com.easyfamily.query.cache.QueryCacheService.CachedBinding;
import com.easyfamily.query.cache.RedisQueryCacheService;
import com.easyfamily.query.dto.QueryDtos.BindingQueryRequest;
import com.easyfamily.query.dto.QueryDtos.BindingQueryResponse;
import com.easyfamily.query.provider.BindingQueryProviderRouter;
import com.easyfamily.query.provider.ProviderExecutor;
import org.springframework.stereotype.Service;

@Service
public class QueryService {

    private final RedisQueryCacheService redisQueryCacheService;
    private final InMemoryQueryCacheService inMemoryQueryCacheService;
    private final BindingQueryProviderRouter providerRouter;
    private final QueryRuntimeSettingsService settingsService;
    private final ProviderExecutor providerExecutor;

    public QueryService(
            RedisQueryCacheService redisQueryCacheService,
            InMemoryQueryCacheService inMemoryQueryCacheService,
            BindingQueryProviderRouter providerRouter,
            QueryRuntimeSettingsService settingsService,
            ProviderExecutor providerExecutor
    ) {
        this.redisQueryCacheService = redisQueryCacheService;
        this.inMemoryQueryCacheService = inMemoryQueryCacheService;
        this.providerRouter = providerRouter;
        this.settingsService = settingsService;
        this.providerExecutor = providerExecutor;
    }

    public BindingQueryResponse queryBinding(BindingQueryRequest request, long cacheTtlSeconds) {
        String cacheKey = "binding:" + request.phone() + ":" + request.queryType();

        CachedBinding redisCached = getFromRedisSafely(cacheKey);
        if (redisCached != null) {
            return new BindingQueryResponse(
                    request.phone(),
                    redisCached.bankBound(),
                    redisCached.socialBound(),
                    "redis-cache",
                    System.currentTimeMillis()
            );
        }

        var memoryCached = inMemoryQueryCacheService.get(cacheKey);
        if (memoryCached.isPresent()) {
            CachedBinding value = memoryCached.get();
            return new BindingQueryResponse(
                    request.phone(),
                    value.bankBound(),
                    value.socialBound(),
                    "in-memory-cache",
                    System.currentTimeMillis()
            );
        }

        String providerKey = settingsService.current().providerKey();
        var provider = providerRouter.resolve(providerKey);
        var providerResult = providerExecutor.executeWithGuards(
                providerKey,
                () -> provider.queryBinding(request.phone(), request.queryType())
        );
        CachedBinding fresh = new CachedBinding(providerResult.bankBound(), providerResult.socialBound());
        putToRedisSafely(cacheKey, fresh, cacheTtlSeconds);
        inMemoryQueryCacheService.put(cacheKey, fresh, cacheTtlSeconds);
        return new BindingQueryResponse(
                request.phone(),
                providerResult.bankBound(),
                providerResult.socialBound(),
                providerResult.providerName(),
                System.currentTimeMillis()
        );
    }

    private CachedBinding getFromRedisSafely(String cacheKey) {
        if (!settingsService.current().preferRedisCache()) {
            return null;
        }
        try {
            return redisQueryCacheService.get(cacheKey).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void putToRedisSafely(String cacheKey, CachedBinding value, long ttlSeconds) {
        if (!settingsService.current().preferRedisCache()) {
            return;
        }
        try {
            redisQueryCacheService.put(cacheKey, value, ttlSeconds);
        } catch (Exception ignored) {
            // Fallback to in-memory only when Redis is unavailable.
        }
    }
}
