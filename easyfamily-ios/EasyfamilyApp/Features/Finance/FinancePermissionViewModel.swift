import Foundation

@Observable
final class FinancePermissionViewModel {
    var role: String = "none"         // "head" | "viewer" | "none"
    var headName: String? = nil
    var viewers: [String] = []        // 仅 head 时有值
    var isLoading = false
    var error: String? = nil
    var newPhone: String = ""         // 添加授权时的输入

    var hasFinanceAccess: Bool { role == "head" || role == "viewer" }
    var isHead: Bool { role == "head" }

    func loadRole(token: String) async {
        isLoading = true
        error = nil
        do {
            let result = try await APIService.getFinanceRole(token: token)
            role = result.role
            headName = result.headName
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func loadViewers(token: String) async {
        guard isHead else { return }
        isLoading = true
        error = nil
        do {
            let result = try await APIService.listFinancePermissions(token: token)
            viewers = result.viewers
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func grant(token: String) async throws {
        let phone = newPhone.trimmingCharacters(in: .whitespaces)
        guard !phone.isEmpty else { return }
        try await APIService.grantFinancePermission(token: token, phone: phone)
        newPhone = ""
        await loadViewers(token: token)
    }

    func revoke(token: String, phone: String) async throws {
        try await APIService.revokeFinancePermission(token: token, phone: phone)
        await loadViewers(token: token)
    }
}
