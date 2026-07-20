import Foundation

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var userProfile: UserProfile?
    @Published var billStats: BillStatsDto?
    @Published var todayMenu: DayMenuDto?
    @Published var vehicles: [VehicleItemDto] = []
    @Published var latestRecord: MaintenanceRecordDto?

    @Published var billLoading = true
    @Published var menuLoading = true
    @Published var vehicleLoading = true

    func load(token: String) async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { await self.loadBill(token: token) }
            group.addTask { await self.loadMenu(token: token) }
            group.addTask { await self.loadVehicle(token: token) }
            group.addTask { await self.loadProfile(token: token) }
        }
    }

    private func loadProfile(token: String) async {
        userProfile = try? await APIService.getUserProfile(token: token)
    }

    private func loadBill(token: String) async {
        defer { billLoading = false }
        let month = currentMonthString()
        billStats = try? await APIService.getBillStats(token: token, month: month)
    }

    private func loadMenu(token: String) async {
        defer { menuLoading = false }
        let response = try? await APIService.weeklyMenu(token: token)
        let todayStr = todayDateString()
        todayMenu = response?.days.first { $0.date == todayStr }
            ?? response?.days.first
    }

    private func loadVehicle(token: String) async {
        defer { vehicleLoading = false }
        guard let list = try? await APIService.listVehicles(token: token), !list.isEmpty else { return }
        vehicles = list
        if let records = try? await APIService.listRecords(token: token, vehicleId: list[0].id) {
            latestRecord = records.sorted { $0.serviceDate > $1.serviceDate }.first
        }
    }

    private func currentMonthString() -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM"
        return f.string(from: Date())
    }

    private func todayDateString() -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: Date())
    }
}
