package com.easyfamily.vehicle.service;

import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.vehicle.dto.VehicleDtos.VehicleCreateRequest;
import com.easyfamily.vehicle.dto.VehicleDtos.VehicleItem;
import com.easyfamily.vehicle.dto.VehicleDtos.VehicleUpdateRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.sql.Statement;
import java.util.List;
import java.util.Objects;

@Service
public class VehicleService {

    private final JdbcTemplate jdbcTemplate;

    public VehicleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<VehicleItem> listVehicles(String userId) {
        return jdbcTemplate.query(
                "SELECT id, plate_number, brand, model, year FROM vehicles WHERE user_id = ? ORDER BY created_at DESC",
                (rs, rowNum) -> new VehicleItem(
                        rs.getLong("id"),
                        rs.getString("plate_number"),
                        rs.getString("brand"),
                        rs.getString("model"),
                        rs.getObject("year", Integer.class)
                ),
                userId
        );
    }

    public VehicleItem createVehicle(String userId, VehicleCreateRequest request) {
        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO vehicles(user_id, plate_number, brand, model, year, created_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, userId);
            ps.setString(2, request.plateNumber());
            ps.setString(3, request.brand());
            ps.setString(4, request.model());
            ps.setObject(5, request.year());
            return ps;
        }, keyHolder);
        long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        return new VehicleItem(id, request.plateNumber(), request.brand(), request.model(), request.year());
    }

    public VehicleItem updateVehicle(String userId, Long vehicleId, VehicleUpdateRequest request) {
        int affected = jdbcTemplate.update(
                "UPDATE vehicles SET plate_number = ?, brand = ?, model = ?, year = ? WHERE id = ? AND user_id = ?",
                request.plateNumber(), request.brand(), request.model(), request.year(), vehicleId, userId
        );
        if (affected == 0) {
            throw new BusinessException("VEHICLE_NOT_FOUND", "vehicle not found");
        }
        return new VehicleItem(vehicleId, request.plateNumber(), request.brand(), request.model(), request.year());
    }

    public void deleteVehicle(String userId, Long vehicleId) {
        jdbcTemplate.update("DELETE FROM maintenance_items WHERE record_id IN (SELECT id FROM maintenance_records WHERE vehicle_id = ? AND user_id = ?)", vehicleId, userId);
        jdbcTemplate.update("DELETE FROM maintenance_records WHERE vehicle_id = ? AND user_id = ?", vehicleId, userId);
        int affected = jdbcTemplate.update("DELETE FROM vehicles WHERE id = ? AND user_id = ?", vehicleId, userId);
        if (affected == 0) {
            throw new BusinessException("VEHICLE_NOT_FOUND", "vehicle not found");
        }
    }
}
