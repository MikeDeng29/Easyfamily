package com.easyfamily.vehicle.controller;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.security.AuthContext;
import com.easyfamily.vehicle.dto.VehicleDtos.MaintenanceCreateRequest;
import com.easyfamily.vehicle.dto.VehicleDtos.MaintenanceImportResult;
import com.easyfamily.vehicle.dto.VehicleDtos.MaintenanceRecordItem;
import com.easyfamily.vehicle.dto.VehicleDtos.MaintenanceStats;
import com.easyfamily.vehicle.dto.VehicleDtos.VehicleCreateRequest;
import com.easyfamily.vehicle.dto.VehicleDtos.VehicleItem;
import com.easyfamily.vehicle.dto.VehicleDtos.VehicleUpdateRequest;
import com.easyfamily.vehicle.service.MaintenanceService;
import com.easyfamily.vehicle.service.MaintenanceStatsService;
import com.easyfamily.vehicle.service.VehicleImportService;
import com.easyfamily.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final MaintenanceService maintenanceService;
    private final MaintenanceStatsService statsService;
    private final VehicleImportService vehicleImportService;

    public VehicleController(
            VehicleService vehicleService,
            MaintenanceService maintenanceService,
            MaintenanceStatsService statsService,
            VehicleImportService vehicleImportService
    ) {
        this.vehicleService = vehicleService;
        this.maintenanceService = maintenanceService;
        this.statsService = statsService;
        this.vehicleImportService = vehicleImportService;
    }

    // --- Import ---

    @PostMapping("/import-record")
    public ApiResponse<MaintenanceImportResult> importRecord(
            @RequestParam("image") MultipartFile image
    ) throws IOException {
        return ApiResponse.ok(vehicleImportService.importFromImage(image));
    }

    // --- Vehicles ---

    @GetMapping
    public ApiResponse<List<VehicleItem>> listVehicles() {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(vehicleService.listVehicles(user.userId()));
    }

    @PostMapping
    public ApiResponse<VehicleItem> createVehicle(@Valid @RequestBody VehicleCreateRequest request) {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(vehicleService.createVehicle(user.userId(), request));
    }

    @PutMapping("/{vehicleId}")
    public ApiResponse<VehicleItem> updateVehicle(
            @PathVariable Long vehicleId,
            @Valid @RequestBody VehicleUpdateRequest request
    ) {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(vehicleService.updateVehicle(user.userId(), vehicleId, request));
    }

    @DeleteMapping("/{vehicleId}")
    public ApiResponse<Void> deleteVehicle(@PathVariable Long vehicleId) {
        var user = AuthContext.currentUser();
        vehicleService.deleteVehicle(user.userId(), vehicleId);
        return ApiResponse.ok(null);
    }

    // --- Maintenance Records ---

    @GetMapping("/{vehicleId}/records")
    public ApiResponse<List<MaintenanceRecordItem>> listRecords(@PathVariable Long vehicleId) {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(maintenanceService.listRecords(user.userId(), vehicleId));
    }

    @GetMapping("/{vehicleId}/records/{recordId}")
    public ApiResponse<MaintenanceRecordItem> getRecord(
            @PathVariable Long vehicleId,
            @PathVariable Long recordId
    ) {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(maintenanceService.getRecord(user.userId(), vehicleId, recordId));
    }

    @PostMapping("/{vehicleId}/records")
    public ApiResponse<MaintenanceRecordItem> createRecord(
            @PathVariable Long vehicleId,
            @Valid @RequestBody MaintenanceCreateRequest request
    ) {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(maintenanceService.createRecord(user.userId(), vehicleId, request));
    }

    @DeleteMapping("/{vehicleId}/records/{recordId}")
    public ApiResponse<Void> deleteRecord(
            @PathVariable Long vehicleId,
            @PathVariable Long recordId
    ) {
        var user = AuthContext.currentUser();
        maintenanceService.deleteRecord(user.userId(), vehicleId, recordId);
        return ApiResponse.ok(null);
    }

    // --- Stats ---

    @GetMapping("/{vehicleId}/stats")
    public ApiResponse<MaintenanceStats> getStats(@PathVariable Long vehicleId) {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(statsService.getStats(user.userId(), vehicleId));
    }
}
