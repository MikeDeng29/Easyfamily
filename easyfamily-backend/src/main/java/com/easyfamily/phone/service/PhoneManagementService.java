package com.easyfamily.phone.service;

import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.phone.dto.PhoneDtos.PhoneItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PhoneManagementService {

    private final JdbcTemplate jdbcTemplate;

    public PhoneManagementService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PhoneItem> listMyPhones(String userId, String loginPhone) {
        ensureSeeded(userId, loginPhone);
        return jdbcTemplate.query(
                """
                        SELECT phone, is_primary, bind_status
                        FROM user_phones
                        WHERE user_id = ?
                        ORDER BY id ASC
                        """,
                (rs, rowNum) -> new PhoneItem(
                        rs.getString("phone"),
                        rs.getBoolean("is_primary"),
                        rs.getString("bind_status")
                ),
                userId
        );
    }

    public void bindPhone(String userId, String phone, String loginPhone) {
        ensureSeeded(userId, loginPhone);
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_phones WHERE user_id = ? AND phone = ?",
                Integer.class,
                userId,
                phone
        );
        if (exists != null && exists > 0) {
            throw new BusinessException("PHONE_ALREADY_BOUND", "phone already bound");
        }
        jdbcTemplate.update(
                """
                        INSERT INTO user_phones(user_id, phone, is_primary, bind_status, verified, created_at, updated_at)
                        VALUES (?, ?, 0, 'ACTIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                userId,
                phone
        );
    }

    public void unbindPhone(String userId, String phone, String loginPhone) {
        ensureSeeded(userId, loginPhone);
        PhoneItem target = findPhoneItem(userId, phone);
        if (target == null) {
            throw new BusinessException("PHONE_NOT_FOUND", "phone not found");
        }
        if (target.isPrimary()) {
            throw new BusinessException("PRIMARY_PHONE_CANNOT_UNBIND", "primary phone cannot be unbound");
        }
        jdbcTemplate.update("DELETE FROM user_phones WHERE user_id = ? AND phone = ?", userId, phone);
    }

    public void setPrimaryPhone(String userId, String phone, String loginPhone) {
        ensureSeeded(userId, loginPhone);
        PhoneItem target = findPhoneItem(userId, phone);
        if (target == null) {
            throw new BusinessException("PHONE_NOT_FOUND", "phone not found");
        }
        if (target.isPrimary()) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE user_phones SET is_primary = CASE WHEN phone = ? THEN 1 ELSE 0 END WHERE user_id = ?",
                phone,
                userId
        );
    }

    public void ensurePhoneOwnedByUser(String userId, String loginPhone, String phone) {
        ensureSeeded(userId, loginPhone);
        Integer owned = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_phones WHERE user_id = ? AND phone = ?",
                Integer.class,
                userId,
                phone
        );
        if (owned == null || owned <= 0) {
            throw new BusinessException("PHONE_NOT_BOUND_TO_USER", "phone is not bound to current user");
        }
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
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_phones WHERE user_id = ?",
                Integer.class,
                userId
        );
        if (count == null || count == 0) {
            jdbcTemplate.update(
                    """
                            INSERT INTO user_phones(user_id, phone, is_primary, bind_status, verified, created_at, updated_at)
                            VALUES (?, ?, 1, 'ACTIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                            """,
                    userId,
                    loginPhone
            );
        }
    }

    private PhoneItem findPhoneItem(String userId, String phone) {
        List<PhoneItem> items = jdbcTemplate.query(
                """
                        SELECT phone, is_primary, bind_status
                        FROM user_phones
                        WHERE user_id = ? AND phone = ?
                        """,
                (rs, rowNum) -> new PhoneItem(
                        rs.getString("phone"),
                        rs.getBoolean("is_primary"),
                        rs.getString("bind_status")
                ),
                userId,
                phone
        );
        return items.isEmpty() ? null : items.get(0);
    }
}
