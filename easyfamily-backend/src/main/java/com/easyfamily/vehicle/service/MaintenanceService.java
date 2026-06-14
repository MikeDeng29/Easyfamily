package com.easyfamily.vehicle.service;

import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.vehicle.dto.VehicleDtos.MaintenanceCreateRequest;
import com.easyfamily.vehicle.dto.VehicleDtos.MaintenanceItemDto;
import com.easyfamily.vehicle.dto.VehicleDtos.MaintenanceRecordItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

@Service
public class MaintenanceService {

    private final JdbcTemplate jdbcTemplate;

    public MaintenanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MaintenanceRecordItem> listRecords(String userId, Long vehicleId) {
        return jdbcTemplate.query(
                """
                        SELECT id, vehicle_id, service_date, mileage_km, shop_name, total_cost, notes, created_at
                        FROM maintenance_records
                        WHERE user_id = ? AND vehicle_id = ?
                        ORDER BY service_date DESC
                        """,
                (rs, rowNum) -> {
                    long recordId = rs.getLong("id");
                    List<MaintenanceItemDto> items = listItems(recordId);
                    return new MaintenanceRecordItem(
                            recordId,
                            rs.getLong("vehicle_id"),
                            rs.getDate("service_date").toLocalDate(),
                            rs.getObject("mileage_km", Integer.class),
                            rs.getString("shop_name"),
                            rs.getBigDecimal("total_cost"),
                            rs.getString("notes"),
                            items,
                            rs.getTimestamp("created_at").toInstant().toString()
                    );
                },
                userId,
                vehicleId
        );
    }

    public MaintenanceRecordItem getRecord(String userId, Long vehicleId, Long recordId) {
        List<MaintenanceRecordItem> records = jdbcTemplate.query(
                """
                        SELECT id, vehicle_id, service_date, mileage_km, shop_name, total_cost, notes, created_at
                        FROM maintenance_records
                        WHERE id = ? AND user_id = ? AND vehicle_id = ?
                        """,
                (rs, rowNum) -> {
                    List<MaintenanceItemDto> items = listItems(recordId);
                    return new MaintenanceRecordItem(
                            recordId,
                            rs.getLong("vehicle_id"),
                            rs.getDate("service_date").toLocalDate(),
                            rs.getObject("mileage_km", Integer.class),
                            rs.getString("shop_name"),
                            rs.getBigDecimal("total_cost"),
                            rs.getString("notes"),
                            items,
                            rs.getTimestamp("created_at").toInstant().toString()
                    );
                },
                recordId,
                userId,
                vehicleId
        );
        if (records.isEmpty()) {
            throw new BusinessException("RECORD_NOT_FOUND", "maintenance record not found");
        }
        return records.get(0);
    }

    @Transactional
    public MaintenanceRecordItem createRecord(String userId, Long vehicleId, MaintenanceCreateRequest request) {
        // Verify vehicle belongs to user
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM vehicles WHERE id = ? AND user_id = ?",
                Integer.class,
                vehicleId,
                userId
        );
        if (count == null || count == 0) {
            throw new BusinessException("VEHICLE_NOT_FOUND", "vehicle not found");
        }

        BigDecimal totalCost = request.items().stream()
                .map(i -> i.cost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    """
                            INSERT INTO maintenance_records(vehicle_id, user_id, service_date, mileage_km, shop_name, total_cost, notes, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, vehicleId);
            ps.setString(2, userId);
            ps.setDate(3, java.sql.Date.valueOf(request.serviceDate()));
            ps.setObject(4, request.mileageKm());
            ps.setString(5, request.shopName());
            ps.setBigDecimal(6, totalCost);
            ps.setString(7, request.notes());
            return ps;
        }, keyHolder);
        long recordId = Objects.requireNonNull(keyHolder.getKey()).longValue();

        for (var item : request.items()) {
            jdbcTemplate.update(
                    "INSERT INTO maintenance_items(record_id, category, item_name, cost, is_diy, notes) VALUES (?, ?, ?, ?, ?, ?)",
                    recordId,
                    item.category(),
                    item.itemName(),
                    item.cost(),
                    item.isDiy(),
                    item.notes()
            );
        }

        List<MaintenanceItemDto> items = listItems(recordId);
        return new MaintenanceRecordItem(
                recordId,
                vehicleId,
                request.serviceDate(),
                request.mileageKm(),
                request.shopName(),
                totalCost,
                request.notes(),
                items,
                java.time.Instant.now().toString()
        );
    }

    public void deleteRecord(String userId, Long vehicleId, Long recordId) {
        jdbcTemplate.update("DELETE FROM maintenance_items WHERE record_id = ?", recordId);
        int affected = jdbcTemplate.update(
                "DELETE FROM maintenance_records WHERE id = ? AND user_id = ? AND vehicle_id = ?",
                recordId, userId, vehicleId
        );
        if (affected == 0) {
            throw new BusinessException("RECORD_NOT_FOUND", "maintenance record not found");
        }
    }

    private List<MaintenanceItemDto> listItems(Long recordId) {
        return jdbcTemplate.query(
                "SELECT id, category, item_name, cost, is_diy, notes FROM maintenance_items WHERE record_id = ? ORDER BY id",
                (rs, rowNum) -> new MaintenanceItemDto(
                        rs.getLong("id"),
                        rs.getString("category"),
                        rs.getString("item_name"),
                        rs.getBigDecimal("cost"),
                        rs.getBoolean("is_diy"),
                        rs.getString("notes")
                ),
                recordId
        );
    }
}
