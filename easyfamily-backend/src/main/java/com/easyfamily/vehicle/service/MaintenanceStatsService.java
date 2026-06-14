package com.easyfamily.vehicle.service;

import com.easyfamily.vehicle.dto.VehicleDtos.CategoryStat;
import com.easyfamily.vehicle.dto.VehicleDtos.MaintenanceStats;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MaintenanceStatsService {

    private final JdbcTemplate jdbcTemplate;

    public MaintenanceStatsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public MaintenanceStats getStats(String userId, Long vehicleId) {
        // Verify ownership
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM vehicles WHERE id = ? AND user_id = ?",
                Integer.class,
                vehicleId,
                userId
        );
        if (count == null || count == 0) {
            return new MaintenanceStats(BigDecimal.ZERO, 0, 0, java.util.List.of());
        }

        // Total stats
        var summary = jdbcTemplate.queryForObject(
                """
                        SELECT
                            COALESCE(SUM(r.total_cost), 0) AS total_cost,
                            COUNT(DISTINCT r.id) AS total_records,
                            COUNT(i.id) AS total_items
                        FROM maintenance_records r
                        LEFT JOIN maintenance_items i ON i.record_id = r.id
                        WHERE r.user_id = ? AND r.vehicle_id = ?
                        """,
                (rs, rowNum) -> new Object[]{
                        rs.getBigDecimal("total_cost"),
                        rs.getLong("total_records"),
                        rs.getLong("total_items")
                },
                userId,
                vehicleId
        );

        BigDecimal totalCost = (BigDecimal) summary[0];
        long totalRecords = (long) summary[1];
        long totalItems = (long) summary[2];

        // By category
        var categories = jdbcTemplate.query(
                """
                        SELECT
                            i.category,
                            COALESCE(SUM(i.cost), 0) AS total_cost,
                            COUNT(i.id) AS item_count,
                            SUM(CASE WHEN i.is_diy = 1 THEN 1 ELSE 0 END) AS diy_count
                        FROM maintenance_items i
                        JOIN maintenance_records r ON r.id = i.record_id
                        WHERE r.user_id = ? AND r.vehicle_id = ?
                        GROUP BY i.category
                        ORDER BY total_cost DESC
                        """,
                (rs, rowNum) -> new CategoryStat(
                        rs.getString("category"),
                        rs.getBigDecimal("total_cost"),
                        rs.getLong("item_count"),
                        rs.getLong("diy_count")
                ),
                userId,
                vehicleId
        );

        return new MaintenanceStats(totalCost, totalRecords, totalItems, categories);
    }
}
