import Foundation

@MainActor
final class BillViewModel: ObservableObject {
    @Published var bills: [BillItemDto] = []
    @Published var stats: BillStatsDto?
    @Published var loading: Bool = false
    @Published var error: String?

    func load(token: String, month: String? = nil) async {
        loading = true
        error = nil
        do {
            async let billsResult = APIService.listBills(token: token, month: month)
            async let statsResult = APIService.getBillStats(token: token, month: month)
            bills = try await billsResult
            stats = try await statsResult
        } catch {
            self.error = "加载失败：\(error.localizedDescription)"
        }
        loading = false
    }

    func deleteBill(token: String, id: Int64) async {
        do {
            try await APIService.deleteBill(token: token, id: id)
            await load(token: token)
        } catch {
            self.error = "删除失败：\(error.localizedDescription)"
        }
    }
}

enum BillCategoryIcon {
    static func emoji(for category: String) -> String {
        switch category {
        case "餐饮": return "🍜"
        case "住房": return "🏠"
        case "交通": return "🚗"
        case "购物": return "🛍️"
        case "娱乐": return "🎮"
        case "医疗": return "💊"
        case "教育": return "📚"
        default: return "💰"
        }
    }
}
