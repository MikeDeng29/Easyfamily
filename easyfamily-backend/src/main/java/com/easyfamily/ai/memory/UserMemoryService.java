package com.easyfamily.ai.memory;

import com.easyfamily.ai.embedding.EmbeddingProvider;
import com.easyfamily.ai.memory.MemoryDtos.MemoryItem;
import com.easyfamily.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserMemoryService {

    private static final Logger log = LoggerFactory.getLogger(UserMemoryService.class);

    private static final int MAX_MEMORIES = 30;
    private static final double SEMANTIC_DEDUP_THRESHOLD = 0.92;
    private static final Set<String> VALID_CATEGORIES = Set.of("family", "vehicle", "preference", "habit", "other");
    private static final String DEFAULT_CATEGORY = "other";

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingProvider embeddingProvider;
    private final ObjectMapper objectMapper;

    public UserMemoryService(JdbcTemplate jdbcTemplate, EmbeddingProvider embeddingProvider, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingProvider = embeddingProvider;
        this.objectMapper = objectMapper;
    }

    public List<MemoryItem> list(String userId) {
        return jdbcTemplate.query(
                "SELECT id, content, category, created_at FROM user_memory WHERE user_id = ? ORDER BY id DESC",
                (rs, rowNum) -> new MemoryItem(
                        rs.getLong("id"),
                        rs.getString("content"),
                        rs.getString("category"),
                        rs.getTimestamp("created_at").toInstant().toEpochMilli()
                ),
                userId
        );
    }

    public void add(String userId, String content) {
        add(userId, content, DEFAULT_CATEGORY);
    }

    public void add(String userId, String content, String category) {
        if (content == null || content.isBlank()) {
            return;
        }
        String trimmed = content.trim();
        String normalizedCategory = normalizeCategory(category);

        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_memory WHERE user_id = ? AND content = ?",
                Integer.class, userId, trimmed
        );
        if (existing != null && existing > 0) {
            return;
        }

        Optional<List<Float>> embedding = embeddingProvider.embed(trimmed);
        if (embedding.isPresent() && isSemanticDuplicate(userId, embedding.get())) {
            return;
        }
        String embeddingJson = embedding.map(this::serializeEmbedding).orElse(null);

        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO user_memory(user_id, content, category, embedding, created_at, updated_at)" +
                    " VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, userId);
            ps.setString(2, trimmed);
            ps.setString(3, normalizedCategory);
            ps.setString(4, embeddingJson);
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

    /**
     * Returns the memories most relevant to {@code queryText} for injection into the
     * chat prompt: the union of the top semantically-similar memories and the most
     * recent ones, capped at {@code limit}. Falls back to {@link #recentForPrompt} if
     * embeddings are unavailable for the query or for all of the user's memories.
     */
    public List<String> relevantForPrompt(String userId, String queryText, int limit) {
        List<MemoryRow> rows = loadRows(userId);
        if (rows.isEmpty()) {
            return List.of();
        }

        Optional<List<Float>> queryEmbedding = embeddingProvider.embed(queryText);
        boolean anyEmbeddings = rows.stream().anyMatch(r -> r.embedding != null);
        if (queryEmbedding.isEmpty() || !anyEmbeddings) {
            return recentForPrompt(userId, limit);
        }

        List<Float> queryVector = queryEmbedding.get();
        List<MemoryRow> bySimilarity = rows.stream()
                .filter(r -> r.embedding != null)
                .sorted((a, b) -> Double.compare(cosineSimilarity(queryVector, b.embedding), cosineSimilarity(queryVector, a.embedding)))
                .limit(limit)
                .toList();

        // rows is already ordered by id DESC (most recent first)
        List<MemoryRow> mostRecent = rows.stream().limit(limit).toList();

        LinkedHashMap<Long, MemoryRow> merged = new LinkedHashMap<>();
        for (MemoryRow row : bySimilarity) {
            merged.put(row.id, row);
        }
        for (MemoryRow row : mostRecent) {
            merged.putIfAbsent(row.id, row);
        }

        return merged.values().stream().limit(limit).map(r -> r.content).collect(Collectors.toList());
    }

    private boolean isSemanticDuplicate(String userId, List<Float> embedding) {
        for (MemoryRow row : loadRows(userId)) {
            if (row.embedding != null && cosineSimilarity(embedding, row.embedding) >= SEMANTIC_DEDUP_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    private List<MemoryRow> loadRows(String userId) {
        return jdbcTemplate.query(
                "SELECT id, content, embedding FROM user_memory WHERE user_id = ? ORDER BY id DESC",
                (rs, rowNum) -> new MemoryRow(
                        rs.getLong("id"),
                        rs.getString("content"),
                        deserializeEmbedding(rs.getString("embedding"))
                ),
                userId
        );
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return DEFAULT_CATEGORY;
        }
        String normalized = category.trim().toLowerCase();
        return VALID_CATEGORIES.contains(normalized) ? normalized : DEFAULT_CATEGORY;
    }

    private String serializeEmbedding(List<Float> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception e) {
            log.warn("Failed to serialize memory embedding: {}", e.getMessage());
            return null;
        }
    }

    private List<Float> deserializeEmbedding(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Float>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize memory embedding: {}", e.getMessage());
            return null;
        }
    }

    private double cosineSimilarity(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.size() != b.size() || a.isEmpty()) {
            return 0;
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private record MemoryRow(Long id, String content, List<Float> embedding) {}
}
