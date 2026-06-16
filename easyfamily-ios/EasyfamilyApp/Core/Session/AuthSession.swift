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

    /// Attempts to get a new access token using the stored refresh token.
    /// Returns true and updates `accessToken` on success; calls `logout()` on failure.
    @discardableResult
    func refreshAccessToken() async -> Bool {
        guard let rt = refreshToken else {
            logout()
            return false
        }
        do {
            let result = try await APIService.refreshToken(refreshToken: rt)
            accessToken = result.accessToken
            if let newRt = result.refreshToken {
                refreshToken = newRt
                tokenStore.saveRefreshToken(newRt)
            }
            tokenStore.save(result.accessToken)
            return true
        } catch {
            logout()
            return false
        }
    }
}
