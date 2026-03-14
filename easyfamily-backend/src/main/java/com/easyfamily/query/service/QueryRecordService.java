package com.easyfamily.query.service;

import com.easyfamily.report.service.ReportMetricStoreService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueryRecordService {

    private final JdbcTemplate jdbcTemplate;
    private final ReportMetricStoreService reportMetricStoreService;

    public QueryRecordService(JdbcTemplate jdbcTemplate, ReportMetricStoreService reportMetricStoreService) {
        this.jdbcTemplate = jdbcTemplate;
        this.reportMetricStoreService = reportMetricStoreService;
    }

    public void recordFeatureVisit(String userId, String feature) {
        jdbcTemplate.update(
                """
                        INSERT INTO feature_events(user_id, feature_name, event_name, event_value, request_ip, created_at)
                        VALUES (?, ?, 'visit', NULL, NULL, CURRENT_TIMESTAMP)
                        """,
                userId,
                feature
        );
    }

    public void recordQuery(
            String userId,
            String loginPhone,
            String targetPhone,
            String queryType,
            String providerKey,
            boolean cacheHit,
            String statusCode,
            int latencyMs,
            String ip
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO query_records
                        (user_id, login_phone, target_phone, query_type, bank_bound, social_bound, source, cache_hit,
                         latency_ms, status_code, request_ip, queried_at)
                        VALUES (?, ?, ?, ?, NULL, NULL, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                userId,
                loginPhone,
                targetPhone,
                queryType,
                providerKey,
                cacheHit ? 1 : 0,
                latencyMs,
                statusCode,
                ip
        );
        recordFeatureVisit(userId, "real-name-verify");
        Instant now = Instant.now();
        reportMetricStoreService.recordQueryEvent(new ReportMetricStoreService.QueryEvent(
                now,
                LocalDate.now(),
                "real-name-verify",
                "v1",
                userId,
                loginPhone,
                targetPhone,
                ip,
                queryType,
                providerKey,
                cacheHit,
                statusCode,
                latencyMs,
                null
        ));
    }

    public int totalQueryCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM query_records", Integer.class);
        return count == null ? 0 : count;
    }

    public int cacheHitCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM query_records WHERE cache_hit = 1", Integer.class);
        return count == null ? 0 : count;
    }

    public Map<String, Integer> featurePvSnapshot() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT feature_name, COUNT(1) AS pv
                        FROM feature_events
                        GROUP BY feature_name
                        ORDER BY pv DESC
                        """
        );
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(String.valueOf(row.get("feature_name")), ((Number) row.get("pv")).intValue());
        }
        return result;
    }

    public Map<LocalDate, Integer> dauSnapshot() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT DATE(queried_at) AS d, COUNT(DISTINCT user_id) AS dau
                        FROM query_records
                        GROUP BY DATE(queried_at)
                        ORDER BY d ASC
                        """
        );
        Map<LocalDate, Integer> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object dateObj = row.get("d");
            LocalDate date;
            if (dateObj instanceof java.sql.Date sqlDate) {
                date = sqlDate.toLocalDate();
            } else {
                date = LocalDate.parse(String.valueOf(dateObj));
            }
            result.put(date, ((Number) row.get("dau")).intValue());
        }
        return result;
    }
}
