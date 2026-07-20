import Foundation

@MainActor
final class AuthSession: ObservableObject {
    @Published var accessToken: String?
    @Published var userId: String?

    private var refreshToken: String?
    private let tokenStore = TokenStore()

    var isLoggedIn: Bool { accessToken != nil }

    init() {
        accessToken = tokenStore.load()
        refreshToken = tokenStore.loadRefreshToken()
    }

    func login(userId: String, accessToken: String, refreshToken: String? = nil) {
        self.userId = userId
        self.accessToken = accessToken
        self.refreshToken = refreshToken
        tokenStore.save(accessToken)
        if let rt = refreshToken { tokenStore.saveRefreshToken(rt) }
    }

    func logout() {
        userId = nil
        accessToken = nil
        refreshToken = nil
        tokenStore.clear()
    }

    /// Refreshes the access token using the stored refresh token.
    /// Logs out only when the refresh token is confirmed invalid (401 or token-revoked error).
    /// Transient network or server errors leave the existing session intact.
    @discardableResult
    func refreshAccessToken() async -> String? {
        guard let rt = refreshToken else { logout(); return nil }
        do {
            let result = try await APIService.refreshToken(refreshToken: rt)
            accessToken = result.accessToken
            if let newRt = result.refreshToken {
                refreshToken = newRt
                tokenStore.saveRefreshToken(newRt)
            }
            tokenStore.save(result.accessToken)
            return result.accessToken
        } catch let error as ApiError {
            if isTokenInvalidError(error) { logout() }
            return nil
        } catch {
            // Network / URLSession errors — keep session alive
            return nil
        }
    }

    private func isTokenInvalidError(_ error: ApiError) -> Bool {
        if error.message.contains("401") { return true }
        let invalidCodes = ["TOKEN_REVOKED", "TOKEN_EXPIRED", "INVALID_TOKEN", "UNAUTHORIZED"]
        return invalidCodes.contains(error.code ?? "")
    }
}
