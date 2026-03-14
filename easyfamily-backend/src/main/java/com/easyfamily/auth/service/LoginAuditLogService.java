package com.easyfamily.auth.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoginAuditLogService {

    private static final int MAX_USER_AGENT_LENGTH = 500;
    private final JdbcTemplate jdbcTemplate;

    public LoginAuditLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS login_audit_logs (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  login_type VARCHAR(32) NOT NULL,
                  principal VARCHAR(128) NOT NULL,
                  result VARCHAR(16) NOT NULL,
                  reason_code VARCHAR(64),
                  client_ip VARCHAR(64),
                  user_agent VARCHAR(500),
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    public void record(
            String loginType,
            String principal,
            String result,
            String reasonCode,
            String clientIp,
            String userAgent
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO login_audit_logs
                        (login_type, principal, result, reason_code, client_ip, user_agent, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                loginType,
                principal,
                result,
                reasonCode,
                clientIp,
                normalizeUserAgent(userAgent)
        );
    }

    private String normalizeUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        if (userAgent.length() <= MAX_USER_AGENT_LENGTH) {
            return userAgent;
        }
        return userAgent.substring(0, MAX_USER_AGENT_LENGTH);
    }

    public int countDistinctSuccessfulLoginsToday(String loginType) {
        LocalDate today = LocalDate.now();
        Timestamp start = Timestamp.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Timestamp end = Timestamp.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(DISTINCT principal)
                        FROM login_audit_logs
                        WHERE login_type = ?
                          AND result = 'SUCCESS'
                          AND created_at >= ?
                          AND created_at < ?
                        """,
                Integer.class,
                loginType,
                start,
                end
        );
        return count == null ? 0 : count;
    }

    public Map<LocalDate, Integer> countDistinctSuccessfulLoginsLastDays(String loginType, int days) {
        int normalizedDays = Math.max(1, days);
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(normalizedDays - 1L);
        Timestamp start = Timestamp.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Timestamp end = Timestamp.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT DATE(created_at) AS login_date, COUNT(DISTINCT principal) AS login_user_count
                        FROM login_audit_logs
                        WHERE login_type = ?
                          AND result = 'SUCCESS'
                          AND created_at >= ?
                          AND created_at < ?
                        GROUP BY DATE(created_at)
                        ORDER BY DATE(created_at)
                        """,
                loginType,
                start,
                end
        );

        Map<LocalDate, Integer> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate date = LocalDate.parse(String.valueOf(row.get("login_date")));
            int count = ((Number) row.get("login_user_count")).intValue();
            grouped.put(date, count);
        }

        Map<LocalDate, Integer> fullRange = new LinkedHashMap<>();
        for (int i = 0; i < normalizedDays; i++) {
            LocalDate date = startDate.plusDays(i);
            fullRange.put(date, grouped.getOrDefault(date, 0));
        }
        return fullRange;
    }
}
