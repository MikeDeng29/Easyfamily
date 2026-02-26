package com.easyfamily.query.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisQueryCacheService implements QueryCacheService {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisQueryCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Optional<CachedBinding> get(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null || !value.contains(":")) {
            return Optional.empty();
        }
        String[] parts = value.split(":");
        if (parts.length != 2) {
            return Optional.empty();
        }
        return Optional.of(new CachedBinding("1".equals(parts[0]), "1".equals(parts[1])));
    }

    @Override
    public void put(String key, CachedBinding value, long ttlSeconds) {
        String packed = (value.bankBound() ? "1" : "0") + ":" + (value.socialBound() ? "1" : "0");
        stringRedisTemplate.opsForValue().set(key, packed, Duration.ofSeconds(ttlSeconds));
    }
}
