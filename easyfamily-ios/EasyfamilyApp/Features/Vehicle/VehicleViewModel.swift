import Foundation

@MainActor
final class VehicleViewModel: ObservableObject {
    @Published var vehicles: [VehicleItemDto] = []
    @Published var records: [MaintenanceRecordDto] = []
    @Published var stats: VehicleStatsDto?
    @Published var selectedVehicleId: Int64?
    @Published var loading: Bool = false
    @Published var error: String?

    func loadVehicles(token: String) async {
        loading = true
        error = nil
        do {
            vehicles = try await APIService.listVehicles(token: token)
        } catch {
            self.error = "加载失败：\(error.localizedDescription)"
        }
        loading = false
    }

    func createVehicle(token: String, plate: String, brand: String, model: String, year: Int?) async {
        do {
            _ = try await APIService.createVehicle(token: token, plateNumber: plate, brand: brand, model: model, year: year)
            await loadVehicles(token: token)
        } catch {
            self.error = "创建失败：\(error.localizedDescription)"
        }
    }

    func deleteVehicle(token: String, vehicleId: Int64) async {
        do {
            try await APIService.deleteVehicle(token: token, vehicleId: vehicleId)
            await loadVehicles(token: token)
        } catch {
            self.error = "删除失败：\(error.localizedDescription)"
        }
    }

    func selectVehicle(token: String, vehicleId: Int64) async {
        selectedVehicleId = vehicleId
        records = []
        stats = nil
        loading = true
        async let recordsResult = try? APIService.listRecords(token: token, vehicleId: vehicleId)
        async let statsResult = try? APIService.getVehicleStats(token: token, vehicleId: vehicleId)
        records = await recordsResult ?? []
        stats = await statsResult
        loading = false
    }

    func createRecord(
        token: String,
        vehicleId: Int64,
        serviceDate: String,
        mileageKm: Int?,
        shopName: String?,
        notes: String?,
        items: [MaintenanceItemInput]
    ) async {
        do {
            _ = try await APIService.createRecord(
                token: token, vehicleId: vehicleId, serviceDate: serviceDate,
                mileageKm: mileageKm, shopName: shopName, notes: notes, items: items
            )
            await selectVehicle(token: token, vehicleId: vehicleId)
        } catch {
            self.error = "创建失败：\(error.localizedDescription)"
        }
    }

    func deleteRecord(token: String, vehicleId: Int64, recordId: Int64) async {
        do {
            try await APIService.deleteRecord(token: token, vehicleId: vehicleId, recordId: recordId)
            await selectVehicle(token: token, vehicleId: vehicleId)
        } catch {
            self.error = "删除失败：\(error.localizedDescription)"
        }
    }
}
