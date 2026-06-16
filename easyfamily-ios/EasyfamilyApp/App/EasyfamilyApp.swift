import SwiftUI

@main
struct EasyfamilyApp: App {
    @StateObject private var session = AuthSession()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(session)
                .preferredColorScheme(.light)
                .task { APIClient.shared.authSession = session }
        }
    }
}
