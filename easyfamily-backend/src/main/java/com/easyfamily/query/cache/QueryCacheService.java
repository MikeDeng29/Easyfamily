package com.easyfamily.query.cache;

import java.util.Optional;

public interface QueryCacheService {

    Optional<CachedBinding> get(String key);

    void put(String key, CachedBinding value, long ttlSeconds);

    record CachedBinding(boolean bankBound, boolean socialBound) {
    }
}
