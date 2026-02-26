package com.easyfamily.query.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class QueryRecordService {

    private final AtomicInteger totalQueryCount = new AtomicInteger();
    private final AtomicInteger cacheHitCount = new AtomicInteger();
    private final Map<String, AtomicInteger> featurePv = new ConcurrentHashMap<>();
    private final Map<LocalDate, AtomicInteger> dauByDate = new ConcurrentHashMap<>();
    private final Map<LocalDate, Map<String, Boolean>> activeUsers = new ConcurrentHashMap<>();

    public void recordFeatureVisit(String userId, String feature) {
        featurePv.computeIfAbsent(feature, key -> new AtomicInteger()).incrementAndGet();
        LocalDate today = LocalDate.now();
        activeUsers.computeIfAbsent(today, key -> new ConcurrentHashMap<>()).put(userId, true);
        dauByDate.put(today, new AtomicInteger(activeUsers.get(today).size()));
    }

    public void recordQuery(String userId, boolean cacheHit) {
        totalQueryCount.incrementAndGet();
        if (cacheHit) {
            cacheHitCount.incrementAndGet();
        }
        recordFeatureVisit(userId, "phone-binding-query");
    }

    public int totalQueryCount() {
        return totalQueryCount.get();
    }

    public int cacheHitCount() {
        return cacheHitCount.get();
    }

    public Map<String, Integer> featurePvSnapshot() {
        return featurePv.entrySet().stream().collect(
                java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())
        );
    }

    public Map<LocalDate, Integer> dauSnapshot() {
        return dauByDate.entrySet().stream().collect(
                java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())
        );
    }
}
