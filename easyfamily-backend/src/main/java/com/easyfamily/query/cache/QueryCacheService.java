package com.easyfamily.query.cache;

import java.util.Optional;

public interface QueryCacheService {

    Optional<CachedRealName> get(String key);

    void put(String key, CachedRealName value, long ttlSeconds);

    record CachedRealName(boolean verified) {
    }
}
