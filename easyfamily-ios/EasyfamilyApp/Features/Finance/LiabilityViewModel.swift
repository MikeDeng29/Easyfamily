import Foundation

@MainActor
final class LiabilityViewModel: ObservableObject {
    @Published var liabilities: [LiabilityItem] = []
    @Published var totalBalance: Decimal = 0
    @Published var totalMonthlyPayment: Decimal = 0
    @Published var isLoading: Bool = false
    @Published var error: String?

    func load(token: String) async {
        isLoading = true
        error = nil
        do {
            let response = try await APIService.listLiabilities(token: token)
            liabilities = response.items
            totalBalance = response.totalBalance
            totalMonthlyPayment = response.totalMonthlyPayment
        } catch {
            self.error = "加载失败：\(error.localizedDescription)"
        }
        isLoading = false
    }

    func create(token: String, name: String, liabilityType: String, balance: Decimal,
                monthlyPayment: Decimal?, interestRate: Decimal?, note: String?) async throws {
        let req = LiabilityCreateRequest(
            name: name, liabilityType: liabilityType, balance: balance,
            monthlyPayment: monthlyPayment, interestRate: interestRate,
            note: note?.isEmpty == false ? note : nil
        )
        _ = try await APIService.createLiability(token: token, req: req)
        await load(token: token)
    }

    func update(token: String, id: Int, name: String, liabilityType: String, balance: Decimal,
                monthlyPayment: Decimal?, interestRate: Decimal?, note: String?) async throws {
        let req = LiabilityCreateRequest(
            name: name, liabilityType: liabilityType, balance: balance,
            monthlyPayment: monthlyPayment, interestRate: interestRate,
            note: note?.isEmpty == false ? note : nil
        )
        _ = try await APIService.updateLiability(token: token, id: id, req: req)
        await load(token: token)
    }

    func delete(token: String, id: Int) async throws {
        try await APIService.deleteLiability(token: token, id: id)
        await load(token: token)
    }
}
