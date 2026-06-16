import Foundation

@MainActor
final class BillViewModel: ObservableObject {
    @Published var bills: [BillItemDto] = []
    @Published var stats: BillStatsDto?
    @Published var trend: [MonthlyTrendItemDto] = []
    @Published var securityReport: SecurityReportDto?
    @Published var loading: Bool = false
    @Published var error: String?

    // Keep last-used token so deleteBill can trigger a full reload
    private var lastToken: String?

    func load(token: String, month: String? = nil) {
        lastToken = token
        Task {
            loading = true
            error = nil
            async let billsResult = APIService.listBills(token: token, month: month)
            async let statsResult = APIService.getBillStats(token: token, month: month)
            async let trendResult = APIService.getMonthlyTrend(token: token, months: 6)
            async let reportResult = APIService.getSecurityReport(token: token)
            do {
                bills = try await billsResult
                stats = try await statsResult
                trend = (try? await trendResult) ?? []
                securityReport = try? await reportResult
            } catch {
                self.error = "加载失败：\(error.localizedDescription)"
            }
            loading = false
        }
    }

    func deleteBill(token: String, id: Int64) async {
        do {
            try await APIService.deleteBill(token: token, id: id)
            load(token: token)
        } catch {
            self.error = "删除失败：\(error.localizedDescription)"
        }
    }
}

enum BillCategoryIcon {
    static func sfSymbol(for category: String, direction: String) -> String {
        if direction == "income" {
            return "arrow.down.circle.fill"
        }
        return "arrow.up.circle.fill"
    }
}
