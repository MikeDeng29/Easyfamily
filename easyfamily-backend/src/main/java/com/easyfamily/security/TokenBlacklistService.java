package com.easyfamily.security;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.sql.Timestamp;

@Service
public class TokenBlacklistService {

    private final JdbcTemplate jdbcTemplate;

    public TokenBlacklistService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS token_blacklist (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  token VARCHAR(512) NOT NULL UNIQUE,
                  expire_at TIMESTAMP NOT NULL,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  INDEX idx_token_blacklist_expire_at (expire_at)
                )
                """);
    }

    public void revoke(String token, Instant expireAt) {
        if (token == null || token.isBlank() || expireAt == null) {
            return;
        }
        cleanupExpired();
        jdbcTemplate.update(
                """
                        INSERT INTO token_blacklist(token, expire_at, created_at)
                        VALUES (?, ?, CURRENT_TIMESTAMP)
                        ON DUPLICATE KEY UPDATE expire_at = VALUES(expire_at)
                        """,
                token,
                Timestamp.from(expireAt)
        );
    }

    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        cleanupExpired();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM token_blacklist WHERE token = ? AND expire_at > CURRENT_TIMESTAMP",
                Integer.class,
                token
        );
        return count != null && count > 0;
    }

    private void cleanupExpired() {
        jdbcTemplate.update("DELETE FROM token_blacklist WHERE expire_at <= CURRENT_TIMESTAMP");
    }
}
