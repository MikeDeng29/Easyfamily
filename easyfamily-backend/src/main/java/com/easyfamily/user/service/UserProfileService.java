package com.easyfamily.user.service;

import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.user.dto.UserProfileDtos.UpdateButlerRequest;
import com.easyfamily.user.dto.UserProfileDtos.UserProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserProfileService {

    public static final String DEFAULT_BUTLER_NAME = "青鸟管家";
    public static final int DEFAULT_BUTLER_AVATAR_ID = 1;
    public static final String DEFAULT_BUTLER_PERSONA = "warm";

    private static final int MIN_BUTLER_AVATAR_ID = 1;
    private static final int MAX_BUTLER_AVATAR_ID = 8;
    private static final Set<String> VALID_BUTLER_PERSONAS = Set.of("warm", "strict", "humorous");

    private final JdbcTemplate jdbcTemplate;

    public UserProfileService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserProfile getProfile(String userId, String loginPhone) {
        ensureSeeded(userId, loginPhone);
        return jdbcTemplate.queryForObject(
                "SELECT user_id, phone, nickname, butler_name, butler_avatar_id, butler_persona FROM users WHERE user_id = ?",
                (rs, rowNum) -> toUserProfile(rs),
                userId
        );
    }

    public UserProfile updateNickname(String userId, String loginPhone, String nickname) {
        ensureSeeded(userId, loginPhone);
        jdbcTemplate.update(
                "UPDATE users SET nickname = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                nickname,
                userId
        );
        return getProfile(userId, loginPhone);
    }

    /**
     * Partial update of AI butler identity fields. Only non-null fields in the request
     * are validated and persisted; omitted fields are left unchanged.
     */
    public UserProfile updateButler(String userId, String loginPhone, UpdateButlerRequest request) {
        ensureSeeded(userId, loginPhone);

        if (request.butlerName() != null) {
            String trimmed = request.butlerName().trim();
            if (trimmed.isEmpty() || trimmed.length() > 10) {
                throw new BusinessException("INVALID_BUTLER_NAME", "butler name length must be between 1 and 10");
            }
            jdbcTemplate.update(
                    "UPDATE users SET butler_name = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                    trimmed,
                    userId
            );
        }

        if (request.butlerAvatarId() != null) {
            int avatarId = request.butlerAvatarId();
            if (avatarId < MIN_BUTLER_AVATAR_ID || avatarId > MAX_BUTLER_AVATAR_ID) {
                throw new BusinessException("INVALID_BUTLER_AVATAR_ID", "butler avatar id must be between 1 and 8");
            }
            jdbcTemplate.update(
                    "UPDATE users SET butler_avatar_id = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                    avatarId,
                    userId
            );
        }

        if (request.butlerPersona() != null) {
            String persona = request.butlerPersona().trim().toLowerCase();
            if (!VALID_BUTLER_PERSONAS.contains(persona)) {
                throw new BusinessException("INVALID_BUTLER_PERSONA", "butler persona must be one of warm/strict/humorous");
            }
            jdbcTemplate.update(
                    "UPDATE users SET butler_persona = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                    persona,
                    userId
            );
        }

        return getProfile(userId, loginPhone);
    }

    private UserProfile toUserProfile(java.sql.ResultSet rs) throws java.sql.SQLException {
        String butlerName = rs.getString("butler_name");
        if (butlerName == null || butlerName.isBlank()) {
            butlerName = DEFAULT_BUTLER_NAME;
        }

        int butlerAvatarId = rs.getInt("butler_avatar_id");
        if (rs.wasNull() || butlerAvatarId < MIN_BUTLER_AVATAR_ID || butlerAvatarId > MAX_BUTLER_AVATAR_ID) {
            butlerAvatarId = DEFAULT_BUTLER_AVATAR_ID;
        }

        String butlerPersona = rs.getString("butler_persona");
        if (butlerPersona == null || !VALID_BUTLER_PERSONAS.contains(butlerPersona.toLowerCase())) {
            butlerPersona = DEFAULT_BUTLER_PERSONA;
        } else {
            butlerPersona = butlerPersona.toLowerCase();
        }

        return new UserProfile(
                rs.getString("user_id"),
                rs.getString("phone"),
                rs.getString("nickname"),
                butlerName,
                butlerAvatarId,
                butlerPersona
        );
    }

    private void ensureSeeded(String userId, String loginPhone) {
        jdbcTemplate.update(
                """
                        INSERT INTO users(user_id, phone, status, created_at, updated_at)
                        VALUES (?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        ON DUPLICATE KEY UPDATE phone = VALUES(phone), updated_at = CURRENT_TIMESTAMP
                        """,
                userId,
                loginPhone
        );
    }
}
