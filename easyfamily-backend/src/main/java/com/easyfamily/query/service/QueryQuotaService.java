package com.easyfamily.query.service;

import com.easyfamily.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class QueryQuotaService {

    private final JdbcTemplate jdbcTemplate;
    private final QueryRuntimeSettingsService settingsService;

    public QueryQuotaService(
            JdbcTemplate jdbcTemplate,
            QueryRuntimeSettingsService settingsService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.settingsService = settingsService;
    }

    public void ensureWithinQuota(String userId, String phone, String ip) {
        QueryRuntimeSettingsService.RuntimeSettings settings = settingsService.current();
        incrementAndCheck("user", userId, settings.dailyQuotaPerUser());
        incrementAndCheck("phone", phone, settings.dailyQuotaPerPhone());
        incrementAndCheck("ip", ip, settings.dailyQuotaPerIp());
    }

    public int effectiveDailyQuota() {
        return settingsService.current().dailyQuotaPerUser();
    }

    private void incrementAndCheck(String dimension, String value, int limit) {
        Integer used = countTodayUsage(dimension, value);
        int afterUse = (used == null ? 0 : used) + 1;
        if (afterUse > limit) {
            throw new BusinessException("QUOTA_EXCEEDED", "daily query quota exceeded: " + dimension);
        }
    }

    private Integer countTodayUsage(String dimension, String value) {
        if ("user".equals(dimension)) {
            return jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM query_records WHERE user_id = ? AND DATE(queried_at) = CURRENT_DATE",
                    Integer.class,
                    value
            );
        }
        if ("phone".equals(dimension)) {
            return jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM query_records WHERE target_phone = ? AND DATE(queried_at) = CURRENT_DATE",
                    Integer.class,
                    value
            );
        }
        if ("ip".equals(dimension)) {
            return jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM query_records WHERE request_ip = ? AND DATE(queried_at) = CURRENT_DATE",
                    Integer.class,
                    value
            );
        }
        return 0;
    }
}
