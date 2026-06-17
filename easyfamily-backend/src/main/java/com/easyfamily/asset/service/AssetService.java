package com.easyfamily.asset.service;

import com.easyfamily.asset.dto.AssetDtos.AssetCreateRequest;
import com.easyfamily.asset.dto.AssetDtos.AssetItem;
import com.easyfamily.asset.dto.AssetDtos.AssetListResponse;
import com.easyfamily.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class AssetService {

    private static final Set<String> VALID_ASSET_TYPES =
            Set.of("cash", "savings", "fund", "stock", "property", "vehicle", "other");

    private final JdbcTemplate jdbcTemplate;

    public AssetService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AssetListResponse list(String userId) {
        List<AssetItem> items = jdbcTemplate.query(
                "SELECT id, name, asset_type, asset_value, note, created_at" +
                " FROM family_asset WHERE user_id = ? ORDER BY created_at DESC",
                (rs, rowNum) -> new AssetItem(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("asset_type"),
                        rs.getBigDecimal("asset_value"),
                        rs.getString("note"),
                        rs.getTimestamp("created_at").toLocalDateTime().toLocalDate().toString()
                ),
                userId
        );
        BigDecimal totalValue = items.stream()
                .map(AssetItem::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AssetListResponse(items, totalValue);
    }

    public AssetItem create(String userId, AssetCreateRequest req) {
        validateAssetType(req.assetType());
        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO family_asset(user_id, name, asset_type, asset_value, note, created_at, updated_at)" +
                    " VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, userId);
            ps.setString(2, req.name());
            ps.setString(3, req.assetType());
            ps.setBigDecimal(4, req.value());
            ps.setString(5, req.note());
            return ps;
        }, keyHolder);
        long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        String now = java.time.LocalDate.now().toString();
        return new AssetItem(id, req.name(), req.assetType(), req.value(), req.note(), now);
    }

    public AssetItem update(String userId, Long id, AssetCreateRequest req) {
        validateAssetType(req.assetType());
        int affected = jdbcTemplate.update(
                "UPDATE family_asset SET name = ?, asset_type = ?, asset_value = ?, note = ?," +
                " updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?",
                req.name(), req.assetType(), req.value(), req.note(), id, userId
        );
        if (affected == 0) {
            throw new BusinessException("ASSET_NOT_FOUND", "asset not found or access denied");
        }
        String now = java.time.LocalDate.now().toString();
        return new AssetItem(id, req.name(), req.assetType(), req.value(), req.note(), now);
    }

    public void delete(String userId, Long id) {
        int affected = jdbcTemplate.update(
                "DELETE FROM family_asset WHERE id = ? AND user_id = ?", id, userId
        );
        if (affected == 0) {
            throw new BusinessException("ASSET_NOT_FOUND", "asset not found or access denied");
        }
    }

    private void validateAssetType(String assetType) {
        if (!VALID_ASSET_TYPES.contains(assetType)) {
            throw new BusinessException("ASSET_INVALID_TYPE",
                    "asset_type must be one of: cash, savings, fund, stock, property, vehicle, other");
        }
    }
}
