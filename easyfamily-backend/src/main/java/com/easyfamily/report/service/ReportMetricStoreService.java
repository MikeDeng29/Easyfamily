package com.easyfamily.report.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportMetricStoreService {

    private final JdbcTemplate jdbcTemplate;

    public ReportMetricStoreService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordQueryEvent(QueryEvent event) {
        jdbcTemplate.update(
                """
                        INSERT INTO report_event
                        (event_time, event_date, event_name, event_version, user_id, login_phone, target_phone,
                         ip, query_type, provider_key, cache_hit, status_code, latency_ms, ext_json)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                Timestamp.from(event.eventTime()),
                Date.valueOf(event.eventDate()),
                event.eventName(),
                event.eventVersion(),
                event.userId(),
                event.loginPhone(),
                event.targetPhone(),
                event.ip(),
                event.queryType(),
                event.providerKey(),
                event.cacheHit(),
                event.statusCode(),
                event.latencyMs(),
                event.extJson()
        );
    }

    public int refreshDailyMetrics(LocalDate metricDate) {
        int rows = 0;
        Integer dau = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(DISTINCT user_id)
                        FROM report_event
                        WHERE event_date = ? AND user_id IS NOT NULL AND user_id <> ''
                        """,
                Integer.class,
                Date.valueOf(metricDate)
        );
        rows += upsertDailyMetric(metricDate, "dau", "", "", "", longOrZero(dau));

        Integer queryTotal = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1) FROM report_event
                        WHERE event_date = ?
                          AND event_name IN ('phone-binding-query', 'real-name-verify')
                        """,
                Integer.class,
                Date.valueOf(metricDate)
        );
        rows += upsertDailyMetric(metricDate, "query_total", "", "", "", longOrZero(queryTotal));

        Integer cacheHit = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1) FROM report_event
                        WHERE event_date = ?
                          AND event_name IN ('phone-binding-query', 'real-name-verify')
                          AND cache_hit = 1
                        """,
                Integer.class,
                Date.valueOf(metricDate)
        );
        rows += upsertDailyMetric(metricDate, "query_cache_hit", "", "", "", longOrZero(cacheHit));

        List<Map<String, Object>> featureRows = jdbcTemplate.queryForList(
                """
                        SELECT event_name AS feature_name, COUNT(1) AS pv
                        FROM report_event
                        WHERE event_date = ?
                        GROUP BY event_name
                        """,
                Date.valueOf(metricDate)
        );
        for (Map<String, Object> row : featureRows) {
            String featureName = stringOrEmpty(row.get("feature_name"));
            long pv = ((Number) row.get("pv")).longValue();
            rows += upsertDailyMetric(metricDate, "feature_pv", featureName, "", "", pv);
        }
        return rows;
    }

    public void backfillFromMemorySnapshots(
            Map<LocalDate, Integer> dauByDate,
            Map<String, Integer> featurePv,
            int totalQueryCount,
            int cacheHitCount
    ) {
        for (Map.Entry<LocalDate, Integer> entry : dauByDate.entrySet()) {
            upsertDailyMetric(entry.getKey(), "dau", "", "", "", entry.getValue().longValue());
        }
        LocalDate today = LocalDate.now();
        for (Map.Entry<String, Integer> entry : featurePv.entrySet()) {
            upsertDailyMetric(today, "feature_pv", entry.getKey(), "", "", entry.getValue().longValue());
        }
        upsertDailyMetric(today, "query_total", "", "", "", totalQueryCount);
        upsertDailyMetric(today, "query_cache_hit", "", "", "", cacheHitCount);
    }

    public Map<LocalDate, Integer> loadDauSnapshot() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT metric_date, metric_value
                        FROM report_metric_daily
                        WHERE metric_name = 'dau'
                        ORDER BY metric_date ASC
                        """
        );
        Map<LocalDate, Integer> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate metricDate = ((Date) row.get("metric_date")).toLocalDate();
            int value = ((Number) row.get("metric_value")).intValue();
            result.put(metricDate, value);
        }
        return result;
    }

    public Map<String, Integer> loadFeaturePvSnapshot() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT dim1 AS feature_name, COALESCE(SUM(metric_value), 0) AS pv
                        FROM report_metric_daily
                        WHERE metric_name = 'feature_pv'
                        GROUP BY dim1
                        ORDER BY pv DESC
                        """
        );
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(stringOrEmpty(row.get("feature_name")), ((Number) row.get("pv")).intValue());
        }
        return result;
    }

    public OverviewMetrics loadOverviewTotals() {
        Integer totalQueryCount = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(metric_value), 0) FROM report_metric_daily WHERE metric_name = 'query_total'",
                Integer.class
        );
        Integer cacheHitCount = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(metric_value), 0) FROM report_metric_daily WHERE metric_name = 'query_cache_hit'",
                Integer.class
        );
        return new OverviewMetrics(intOrZero(totalQueryCount), intOrZero(cacheHitCount));
    }

    public boolean hasMetricData() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM report_metric_daily", Integer.class);
        return intOrZero(count) > 0;
    }

    private int upsertDailyMetric(
            LocalDate metricDate,
            String metricName,
            String dim1,
            String dim2,
            String dim3,
            long metricValue
    ) {
        return jdbcTemplate.update(
                """
                        INSERT INTO report_metric_daily
                        (metric_date, metric_name, dim1, dim2, dim3, metric_value, calculated_at, source)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'event-aggregation')
                        ON DUPLICATE KEY UPDATE
                          metric_value = VALUES(metric_value),
                          calculated_at = VALUES(calculated_at),
                          source = VALUES(source)
                        """,
                Date.valueOf(metricDate),
                metricName,
                dim1,
                dim2,
                dim3,
                metricValue,
                Timestamp.from(Instant.now())
        );
    }

    private long longOrZero(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private int intOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String stringOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    public record QueryEvent(
            Instant eventTime,
            LocalDate eventDate,
            String eventName,
            String eventVersion,
            String userId,
            String loginPhone,
            String targetPhone,
            String ip,
            String queryType,
            String providerKey,
            boolean cacheHit,
            String statusCode,
            int latencyMs,
            String extJson
    ) {
    }

    public record OverviewMetrics(int totalQueryCount, int cacheHitCount) {
    }
}
