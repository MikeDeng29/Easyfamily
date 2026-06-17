import Foundation

@Observable
final class FinanceViewModel {
    var healthReport: FinancialHealthReport? = nil
    var familyStats: FamilyBillStats? = nil
    var selectedMonth: String = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM"
        return formatter.string(from: Date())
    }()
    var isLoading = false
    var error: String? = nil

    func load(token: String) async {
        isLoading = true
        error = nil
        async let reportTask = APIService.getFinancialHealthReport(token: token, month: selectedMonth)
        async let statsTask = APIService.getFamilyBillStats(token: token, month: selectedMonth)
        do {
            let (report, stats) = try await (reportTask, statsTask)
            healthReport = report
            familyStats = stats
        } catch {
            self.error = "加载失败：\(error.localizedDescription)"
        }
        isLoading = false
    }
}
