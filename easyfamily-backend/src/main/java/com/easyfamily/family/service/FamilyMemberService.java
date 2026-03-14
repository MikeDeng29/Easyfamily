package com.easyfamily.family.service;

import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.family.dto.FamilyDtos.FamilyMemberCreateRequest;
import com.easyfamily.family.dto.FamilyDtos.FamilyMemberItem;
import com.easyfamily.family.dto.FamilyDtos.FamilyMemberUpdateRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FamilyMemberService {

    private final JdbcTemplate jdbcTemplate;

    public FamilyMemberService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FamilyMemberItem> listMembers(String userId) {
        return jdbcTemplate.query(
                """
                        SELECT member_id, member_name, member_phone, relation_to_user
                        FROM family_members
                        WHERE user_id = ?
                        ORDER BY created_at DESC
                        """,
                (rs, rowNum) -> new FamilyMemberItem(
                        rs.getString("member_id"),
                        rs.getString("member_name"),
                        rs.getString("member_phone"),
                        rs.getString("relation_to_user")
                ),
                userId
        );
    }

    public FamilyMemberItem getMember(String userId, String memberId) {
        List<FamilyMemberItem> items = jdbcTemplate.query(
                """
                        SELECT member_id, member_name, member_phone, relation_to_user
                        FROM family_members
                        WHERE user_id = ? AND member_id = ?
                        """,
                (rs, rowNum) -> new FamilyMemberItem(
                        rs.getString("member_id"),
                        rs.getString("member_name"),
                        rs.getString("member_phone"),
                        rs.getString("relation_to_user")
                ),
                userId,
                memberId
        );
        if (items.isEmpty()) {
            throw new BusinessException("FAMILY_MEMBER_NOT_FOUND", "family member not found");
        }
        return items.get(0);
    }

    public FamilyMemberItem createMember(String userId, FamilyMemberCreateRequest request) {
        ensurePhoneNotDuplicated(userId, request.phone(), null);
        String memberId = "FM" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update(
                """
                        INSERT INTO family_members(user_id, member_id, member_name, member_phone, relation_to_user, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                userId,
                memberId,
                request.name(),
                request.phone(),
                request.relation()
        );
        return new FamilyMemberItem(memberId, request.name(), request.phone(), request.relation());
    }

    public FamilyMemberItem updateMember(String userId, String memberId, FamilyMemberUpdateRequest request) {
        getMember(userId, memberId);
        ensurePhoneNotDuplicated(userId, request.phone(), memberId);
        jdbcTemplate.update(
                """
                        UPDATE family_members
                        SET member_name = ?, member_phone = ?, relation_to_user = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE user_id = ? AND member_id = ?
                        """,
                request.name(),
                request.phone(),
                request.relation(),
                userId,
                memberId
        );
        return new FamilyMemberItem(memberId, request.name(), request.phone(), request.relation());
    }

    public void deleteMember(String userId, String memberId) {
        int affected = jdbcTemplate.update(
                "DELETE FROM family_members WHERE user_id = ? AND member_id = ?",
                userId,
                memberId
        );
        if (affected <= 0) {
            throw new BusinessException("FAMILY_MEMBER_NOT_FOUND", "family member not found");
        }
    }

    private void ensurePhoneNotDuplicated(String userId, String phone, String excludeMemberId) {
        Integer count;
        if (excludeMemberId == null) {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM family_members WHERE user_id = ? AND member_phone = ?",
                    Integer.class,
                    userId,
                    phone
            );
        } else {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM family_members WHERE user_id = ? AND member_phone = ? AND member_id <> ?",
                    Integer.class,
                    userId,
                    phone,
                    excludeMemberId
            );
        }
        if (count != null && count > 0) {
            throw new BusinessException("FAMILY_MEMBER_PHONE_EXISTS", "family member phone already exists");
        }
    }
}
