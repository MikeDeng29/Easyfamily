import Foundation

@MainActor
final class AuthSession: ObservableObject {
    @Published var accessToken: String?
    @Published var userId: String?

    private let tokenStore = TokenStore()

    var isLoggedIn: Bool { accessToken != nil }

    init() {
        accessToken = tokenStore.load()
    }

    func login(userId: String, accessToken: String) {
        self.userId = userId
        self.accessToken = accessToken
        tokenStore.save(accessToken)
    }

    func logout() {
        userId = nil
        accessToken = nil
        tokenStore.clear()
    }
}
