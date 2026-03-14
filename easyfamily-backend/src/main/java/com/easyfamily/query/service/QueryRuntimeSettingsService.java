package com.easyfamily.query.service;

import com.easyfamily.query.config.QueryProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Service
public class QueryRuntimeSettingsService {

    private final QueryProperties defaults;
    private final JdbcTemplate jdbcTemplate;

    public QueryRuntimeSettingsService(QueryProperties defaults, JdbcTemplate jdbcTemplate) {
        this.defaults = defaults;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initDefaults() {
        upsert("dailyQuotaPerUser", String.valueOf(defaults.dailyQuotaPerUser()));
        upsert("dailyQuotaPerPhone", String.valueOf(defaults.dailyQuotaPerPhone()));
        upsert("dailyQuotaPerIp", String.valueOf(defaults.dailyQuotaPerIp()));
        upsert("preferRedisCache", String.valueOf(defaults.preferRedisCache()));
        upsert("providerKey", defaults.providerKey());
        upsert("providerTimeoutMs", String.valueOf(defaults.providerTimeoutMs()));
        upsert("providerRetryTimes", String.valueOf(defaults.providerRetryTimes()));
        upsert("providerCircuitFailureThreshold", String.valueOf(defaults.providerCircuitFailureThreshold()));
        upsert("providerCircuitOpenSeconds", String.valueOf(defaults.providerCircuitOpenSeconds()));
    }

    public RuntimeSettings current() {
        return new RuntimeSettings(
                getInt("dailyQuotaPerUser", defaults.dailyQuotaPerUser()),
                getInt("dailyQuotaPerPhone", defaults.dailyQuotaPerPhone()),
                getInt("dailyQuotaPerIp", defaults.dailyQuotaPerIp()),
                getBoolean("preferRedisCache", defaults.preferRedisCache()),
                getString("providerKey", defaults.providerKey()),
                getLong("providerTimeoutMs", defaults.providerTimeoutMs()),
                getInt("providerRetryTimes", defaults.providerRetryTimes()),
                getInt("providerCircuitFailureThreshold", defaults.providerCircuitFailureThreshold()),
                getInt("providerCircuitOpenSeconds", defaults.providerCircuitOpenSeconds())
        );
    }

    public RuntimeSettings update(
            Integer dailyQuotaPerUser,
            Integer dailyQuotaPerPhone,
            Integer dailyQuotaPerIp,
            Boolean preferRedisCache,
            String providerKey,
            Long providerTimeoutMs,
            Integer providerRetryTimes,
            Integer providerCircuitFailureThreshold,
            Integer providerCircuitOpenSeconds
    ) {
        RuntimeSettings now = current();
        upsert("dailyQuotaPerUser", String.valueOf(dailyQuotaPerUser != null ? dailyQuotaPerUser : now.dailyQuotaPerUser()));
        upsert("dailyQuotaPerPhone", String.valueOf(dailyQuotaPerPhone != null ? dailyQuotaPerPhone : now.dailyQuotaPerPhone()));
        upsert("dailyQuotaPerIp", String.valueOf(dailyQuotaPerIp != null ? dailyQuotaPerIp : now.dailyQuotaPerIp()));
        upsert("preferRedisCache", String.valueOf(preferRedisCache != null ? preferRedisCache : now.preferRedisCache()));
        upsert("providerKey", providerKey != null && !providerKey.isBlank() ? providerKey : now.providerKey());
        upsert("providerTimeoutMs", String.valueOf(providerTimeoutMs != null ? providerTimeoutMs : now.providerTimeoutMs()));
        upsert("providerRetryTimes", String.valueOf(providerRetryTimes != null ? providerRetryTimes : now.providerRetryTimes()));
        upsert(
                "providerCircuitFailureThreshold",
                String.valueOf(providerCircuitFailureThreshold != null ? providerCircuitFailureThreshold : now.providerCircuitFailureThreshold())
        );
        upsert(
                "providerCircuitOpenSeconds",
                String.valueOf(providerCircuitOpenSeconds != null ? providerCircuitOpenSeconds : now.providerCircuitOpenSeconds())
        );
        return current();
    }

    public Map<String, Object> toMap() {
        RuntimeSettings s = current();
        return Map.of(
                "dailyQuotaPerUser", s.dailyQuotaPerUser(),
                "dailyQuotaPerPhone", s.dailyQuotaPerPhone(),
                "dailyQuotaPerIp", s.dailyQuotaPerIp(),
                "preferRedisCache", s.preferRedisCache(),
                "providerKey", s.providerKey(),
                "providerTimeoutMs", s.providerTimeoutMs(),
                "providerRetryTimes", s.providerRetryTimes(),
                "providerCircuitFailureThreshold", s.providerCircuitFailureThreshold(),
                "providerCircuitOpenSeconds", s.providerCircuitOpenSeconds()
        );
    }

    private void upsert(String settingKey, String settingValue) {
        jdbcTemplate.update(
                """
                        INSERT INTO runtime_settings(setting_key, setting_value, updated_at)
                        VALUES (?, ?, CURRENT_TIMESTAMP)
                        ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = CURRENT_TIMESTAMP
                        """,
                settingKey,
                settingValue
        );
    }

    private String getString(String settingKey, String defaultValue) {
        String value = jdbcTemplate.query(
                "SELECT setting_value FROM runtime_settings WHERE setting_key = ?",
                rs -> rs.next() ? rs.getString(1) : null,
                settingKey
        );
        return value == null ? defaultValue : value;
    }

    private int getInt(String settingKey, int defaultValue) {
        try {
            return Integer.parseInt(getString(settingKey, String.valueOf(defaultValue)));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private long getLong(String settingKey, long defaultValue) {
        try {
            return Long.parseLong(getString(settingKey, String.valueOf(defaultValue)));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private boolean getBoolean(String settingKey, boolean defaultValue) {
        return Boolean.parseBoolean(getString(settingKey, String.valueOf(defaultValue)));
    }

    public record RuntimeSettings(
            int dailyQuotaPerUser,
            int dailyQuotaPerPhone,
            int dailyQuotaPerIp,
            boolean preferRedisCache,
            String providerKey,
            long providerTimeoutMs,
            int providerRetryTimes,
            int providerCircuitFailureThreshold,
            int providerCircuitOpenSeconds
    ) {
    }
}
