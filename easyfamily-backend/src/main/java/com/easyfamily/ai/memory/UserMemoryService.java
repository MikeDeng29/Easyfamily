package com.easyfamily.ai.memory;

import com.easyfamily.ai.memory.MemoryDtos.MemoryItem;
import com.easyfamily.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.sql.Statement;
import java.util.List;
import java.util.Objects;

@Service
public class UserMemoryService {

    private static final int MAX_MEMORIES = 30;

    private final JdbcTemplate jdbcTemplate;

    public UserMemoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MemoryItem> list(String userId) {
        return jdbcTemplate.query(
                "SELECT id, content, created_at FROM user_memory WHERE user_id = ? ORDER BY id DESC",
                (rs, rowNum) -> new MemoryItem(
                        rs.getLong("id"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at").toInstant().toEpochMilli()
                ),
                userId
        );
    }

    public void add(String userId, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        String trimmed = content.trim();

        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_memory WHERE user_id = ? AND content = ?",
                Integer.class, userId, trimmed
        );
        if (existing != null && existing > 0) {
            return;
        }

        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO user_memory(user_id, content, created_at, updated_at)" +
                    " VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, userId);
            ps.setString(2, trimmed);
            return ps;
        }, keyHolder);
        Objects.requireNonNull(keyHolder.getKeys());

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_memory WHERE user_id = ?",
                Integer.class, userId
        );
        if (total != null && total > MAX_MEMORIES) {
            int toRemove = total - MAX_MEMORIES;
            jdbcTemplate.update(
                    "DELETE FROM user_memory WHERE id IN (" +
                    "SELECT id FROM (SELECT id FROM user_memory WHERE user_id = ? ORDER BY id ASC LIMIT ?) AS oldest)",
                    userId, toRemove
            );
        }
    }

    public void delete(String userId, Long id) {
        int affected = jdbcTemplate.update(
                "DELETE FROM user_memory WHERE id = ? AND user_id = ?", id, userId
        );
        if (affected == 0) {
            throw new BusinessException("MEMORY_NOT_FOUND", "memory not found or access denied");
        }
    }

    public List<String> recentForPrompt(String userId, int limit) {
        return jdbcTemplate.query(
                "SELECT content FROM user_memory WHERE user_id = ? ORDER BY id DESC LIMIT ?",
                (rs, rowNum) -> rs.getString("content"),
                userId, limit
        );
    }
}
