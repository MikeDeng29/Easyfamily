import SwiftUI

struct RootView: View {
    @EnvironmentObject private var session: AuthSession

    var body: some View {
        if session.isLoggedIn {
            MainTabView()
        } else {
            LoginView()
        }
    }
}
