package com.easyfamily.query.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

@Service
public class RedisQuotaCounterService {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisQuotaCounterService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Integer increment(String dimension, String value) {
        String key = "quota:" + LocalDate.now() + ":" + dimension + ":" + value;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofDays(2));
        }
        return count == null ? null : count.intValue();
    }
}
