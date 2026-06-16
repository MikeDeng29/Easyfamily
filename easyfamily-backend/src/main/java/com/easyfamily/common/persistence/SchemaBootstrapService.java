package com.easyfamily.common.persistence;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SchemaBootstrapService {

    private final JdbcTemplate jdbcTemplate;

    public SchemaBootstrapService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void bootstrap() {
        createAuthCaptchaTokens();
        createAuthSlideCaptchaChallenges();
        createAuthSmsCodes();
        createAuthSmsSendLogs();
        createRuntimeSettings();
        createTokenBlacklist();
        createUsers();
        createUserPhones();
        createFamilyMembers();
        createQueryRecords();
        createQueryQuotaRules();
        createFeatureEvents();
        createDailyMetrics();
        createReportEvent();
        createReportMetricDaily();
        seedDefaultQuotaRules();
    }

    private void createAuthCaptchaTokens() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS auth_captcha_tokens (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  token VARCHAR(255) NOT NULL UNIQUE,
                  provider VARCHAR(64) NOT NULL,
                  expire_at TIMESTAMP NOT NULL,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  INDEX idx_auth_captcha_tokens_expire_at (expire_at)
                )
                """);
    }

    private void createAuthSmsCodes() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS auth_sms_codes (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  phone VARCHAR(32) NOT NULL UNIQUE,
                  sms_code VARCHAR(16) NOT NULL,
                  expire_at TIMESTAMP NOT NULL,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  INDEX idx_auth_sms_codes_expire_at (expire_at)
                )
                """);
    }

    private void createAuthSmsSendLogs() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS auth_sms_send_logs (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  phone VARCHAR(32) NOT NULL,
                  client_ip VARCHAR(64),
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  INDEX idx_auth_sms_send_logs_phone_created_at (phone, created_at),
                  INDEX idx_auth_sms_send_logs_ip_created_at (client_ip, created_at)
                )
                """);
    }

    private void createAuthSlideCaptchaChallenges() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS auth_slide_captcha_challenges (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  challenge_id VARCHAR(128) NOT NULL UNIQUE,
                  target_x INT NOT NULL,
                  expire_at TIMESTAMP NOT NULL,
                  consumed TINYINT(1) NOT NULL DEFAULT 0,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  INDEX idx_auth_slide_captcha_expire_at (expire_at)
                )
                """);
    }

    private void createRuntimeSettings() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS runtime_settings (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  setting_key VARCHAR(128) NOT NULL UNIQUE,
                  setting_value VARCHAR(512) NOT NULL,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);
    }

    private void createTokenBlacklist() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS token_blacklist (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  token VARCHAR(2048) NOT NULL UNIQUE,
                  expire_at TIMESTAMP NOT NULL,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  INDEX idx_token_blacklist_expire_at (expire_at)
                )
                """);
    }

    private void createUsers() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id VARCHAR(64) NOT NULL UNIQUE,
                  phone VARCHAR(32) NOT NULL,
                  nickname VARCHAR(64),
                  butler_name VARCHAR(10),
                  butler_avatar_id INT,
                  butler_persona VARCHAR(16),
                  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  INDEX idx_users_phone (phone)
                )
                """);
    }

    private void createUserPhones() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_phones (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id VARCHAR(64) NOT NULL,
                  phone VARCHAR(32) NOT NULL,
                  is_primary TINYINT(1) NOT NULL DEFAULT 0,
                  bind_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                  verified TINYINT(1) NOT NULL DEFAULT 0,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  UNIQUE KEY uk_user_phones_user_phone (user_id, phone),
                  INDEX idx_user_phones_phone (phone)
                )
                """);
    }

    private void createFamilyMembers() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS family_members (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id VARCHAR(64) NOT NULL,
                  member_id VARCHAR(64) NOT NULL UNIQUE,
                  member_name VARCHAR(64) NOT NULL,
                  member_phone VARCHAR(32) NOT NULL,
                  relation_to_user VARCHAR(64) NOT NULL,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  UNIQUE KEY uk_family_members_user_phone (user_id, member_phone),
                  INDEX idx_family_members_user_id (user_id)
                )
                """);
    }

    private void createQueryRecords() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS query_records (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id VARCHAR(64) NOT NULL,
                  login_phone VARCHAR(32),
                  target_phone VARCHAR(32) NOT NULL,
                  query_type VARCHAR(32) NOT NULL,
                  bank_bound TINYINT(1),
                  social_bound TINYINT(1),
                  source VARCHAR(64),
                  cache_hit TINYINT(1) NOT NULL DEFAULT 0,
                  latency_ms INT NOT NULL DEFAULT 0,
                  status_code VARCHAR(64) NOT NULL,
                  request_ip VARCHAR(64),
                  queried_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  INDEX idx_query_records_user_id (user_id),
                  INDEX idx_query_records_target_phone (target_phone),
                  INDEX idx_query_records_queried_at (queried_at)
                )
                """);
    }

    private void createQueryQuotaRules() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS query_quota_rules (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  rule_key VARCHAR(64) NOT NULL UNIQUE,
                  scope_type VARCHAR(32) NOT NULL,
                  scope_value VARCHAR(64) NOT NULL DEFAULT '',
                  quota_value INT NOT NULL,
                  enabled TINYINT(1) NOT NULL DEFAULT 1,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);
    }

    private void createFeatureEvents() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS feature_events (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id VARCHAR(64),
                  feature_name VARCHAR(128) NOT NULL,
                  event_name VARCHAR(128) NOT NULL,
                  event_value VARCHAR(255),
                  request_ip VARCHAR(64),
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  INDEX idx_feature_events_feature_name (feature_name),
                  INDEX idx_feature_events_created_at (created_at)
                )
                """);
    }

    private void createDailyMetrics() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS daily_metrics (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  metric_date DATE NOT NULL,
                  metric_name VARCHAR(64) NOT NULL,
                  metric_value BIGINT NOT NULL DEFAULT 0,
                  dim1 VARCHAR(128) NOT NULL DEFAULT '',
                  dim2 VARCHAR(128) NOT NULL DEFAULT '',
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  UNIQUE KEY uk_daily_metrics_unique (metric_date, metric_name, dim1, dim2)
                )
                """);
    }

    private void createReportEvent() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS report_event (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  event_time TIMESTAMP NOT NULL,
                  event_date DATE NOT NULL,
                  event_name VARCHAR(64) NOT NULL,
                  event_version VARCHAR(32) NOT NULL DEFAULT 'v1',
                  user_id VARCHAR(64),
                  login_phone VARCHAR(32),
                  target_phone VARCHAR(32),
                  ip VARCHAR(64),
                  query_type VARCHAR(32),
                  provider_key VARCHAR(64),
                  cache_hit TINYINT(1) NOT NULL DEFAULT 0,
                  status_code VARCHAR(64) NOT NULL,
                  latency_ms INT NOT NULL DEFAULT 0,
                  ext_json TEXT,
                  INDEX idx_report_event_date (event_date),
                  INDEX idx_report_event_name (event_name)
                )
                """);
    }

    private void createReportMetricDaily() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS report_metric_daily (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  metric_date DATE NOT NULL,
                  metric_name VARCHAR(64) NOT NULL,
                  dim1 VARCHAR(128) NOT NULL DEFAULT '',
                  dim2 VARCHAR(128) NOT NULL DEFAULT '',
                  dim3 VARCHAR(128) NOT NULL DEFAULT '',
                  metric_value BIGINT NOT NULL DEFAULT 0,
                  calculated_at TIMESTAMP NOT NULL,
                  source VARCHAR(64) NOT NULL DEFAULT 'event-aggregation',
                  UNIQUE KEY uk_report_metric_daily_unique (metric_date, metric_name, dim1, dim2, dim3)
                )
                """);
    }

    private void seedDefaultQuotaRules() {
        jdbcTemplate.update(
                """
                        INSERT INTO query_quota_rules(rule_key, scope_type, scope_value, quota_value, enabled, updated_at)
                        VALUES ('default_daily_quota_per_user', 'USER', '*', 3, 1, CURRENT_TIMESTAMP)
                        ON DUPLICATE KEY UPDATE
                          quota_value = VALUES(quota_value),
                          enabled = VALUES(enabled),
                          updated_at = VALUES(updated_at)
                        """
        );
    }
}
