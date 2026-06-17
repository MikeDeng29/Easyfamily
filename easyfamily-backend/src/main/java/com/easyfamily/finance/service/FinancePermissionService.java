package com.easyfamily.finance.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FinancePermissionService {

    private final JdbcTemplate jdbcTemplate;

    public FinancePermissionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Resolves the finance role for the authenticated user.
     *
     * <ol>
     *   <li>If the user has any row in {@code family_members} as the owner
     *       ({@code user_id = userId}) they are a <b>head</b>.</li>
     *   <li>Otherwise, if their phone appears in
     *       {@code family_finance_permission.viewer_phone}, they are a
     *       <b>viewer</b> and {@code headUserId} is populated with the
     *       corresponding {@code head_user_id}.</li>
     *   <li>Otherwise the role is <b>none</b>.</li>
     * </ol>
     */
    public FinanceRole resolveRole(String userId, String phone) {
        // 1. Check head: at least one family member record owned by this user
        Integer headCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM family_members WHERE user_id = ? LIMIT 1",
                Integer.class,
                userId
        );
        if (headCount != null && headCount > 0) {
            return new FinanceRole("head", null);
        }

        // 2. Check viewer: this phone is in an authorised viewers list
        List<String> headUserIds = jdbcTemplate.queryForList(
                "SELECT p.head_user_id" +
                " FROM family_finance_permission p" +
                " WHERE p.viewer_phone = ?" +
                " LIMIT 1",
                String.class,
                phone
        );
        if (!headUserIds.isEmpty()) {
            return new FinanceRole("viewer", headUserIds.get(0));
        }

        return new FinanceRole("none", null);
    }

    /**
     * Lists the masked phone numbers authorised to view the given head user's
     * financial data.
     */
    public List<String> listViewers(String headUserId) {
        List<String> raw = jdbcTemplate.queryForList(
                "SELECT viewer_phone FROM family_finance_permission WHERE head_user_id = ? ORDER BY created_at ASC",
                String.class,
                headUserId
        );
        return raw.stream().map(FinancePermissionService::maskPhone).toList();
    }

    /**
     * Grants access to the given phone number.  Does nothing if the permission
     * already exists ({@code INSERT IGNORE}).
     */
    public void grantViewer(String headUserId, String viewerPhone) {
        jdbcTemplate.update(
                "INSERT IGNORE INTO family_finance_permission(head_user_id, viewer_phone, created_at)" +
                " VALUES (?, ?, CURRENT_TIMESTAMP)",
                headUserId,
                viewerPhone
        );
    }

    /**
     * Revokes access for the given phone number.
     */
    public void revokeViewer(String headUserId, String viewerPhone) {
        jdbcTemplate.update(
                "DELETE FROM family_finance_permission WHERE head_user_id = ? AND viewer_phone = ?",
                headUserId,
                viewerPhone
        );
    }

    // e.g. "13800138000" -> "138****8000"
    static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
