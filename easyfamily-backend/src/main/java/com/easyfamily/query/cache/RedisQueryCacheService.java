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
    public Optional<CachedRealName> get(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new CachedRealName("1".equals(value.trim())));
    }

    @Override
    public void put(String key, CachedRealName value, long ttlSeconds) {
        String packed = value.verified() ? "1" : "0";
        stringRedisTemplate.opsForValue().set(key, packed, Duration.ofSeconds(ttlSeconds));
    }
}
