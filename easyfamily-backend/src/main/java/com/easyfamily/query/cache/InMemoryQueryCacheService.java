package com.easyfamily.query.cache;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryQueryCacheService implements QueryCacheService {

    private final Map<String, CachedEntry> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<CachedBinding> get(String key) {
        CachedEntry entry = cache.get(key);
        if (entry == null || Instant.now().isAfter(entry.expiredAt())) {
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void put(String key, CachedBinding value, long ttlSeconds) {
        cache.put(key, new CachedEntry(value, Instant.now().plusSeconds(ttlSeconds)));
    }

    private record CachedEntry(CachedBinding value, Instant expiredAt) {
    }
}
