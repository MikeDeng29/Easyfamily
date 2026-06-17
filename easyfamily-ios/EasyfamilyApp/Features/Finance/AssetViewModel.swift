import Foundation

@MainActor
final class AssetViewModel: ObservableObject {
    @Published var assets: [AssetItem] = []
    @Published var totalValue: Decimal = 0
    @Published var isLoading: Bool = false
    @Published var error: String?

    func load(token: String) async {
        isLoading = true
        error = nil
        do {
            let response = try await APIService.listAssets(token: token)
            assets = response.items
            totalValue = response.totalValue
        } catch {
            self.error = "加载失败：\(error.localizedDescription)"
        }
        isLoading = false
    }

    func create(token: String, name: String, assetType: String, value: Decimal, note: String?) async throws {
        let req = AssetCreateRequest(name: name, assetType: assetType, value: value, note: note?.isEmpty == false ? note : nil)
        _ = try await APIService.createAsset(token: token, req: req)
        await load(token: token)
    }

    func update(token: String, id: Int, name: String, assetType: String, value: Decimal, note: String?) async throws {
        let req = AssetCreateRequest(name: name, assetType: assetType, value: value, note: note?.isEmpty == false ? note : nil)
        _ = try await APIService.updateAsset(token: token, id: id, req: req)
        await load(token: token)
    }

    func delete(token: String, id: Int) async throws {
        try await APIService.deleteAsset(token: token, id: id)
        await load(token: token)
    }
}
