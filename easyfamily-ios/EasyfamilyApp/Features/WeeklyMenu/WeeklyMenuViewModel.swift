import Foundation

@MainActor
class WeeklyMenuViewModel: ObservableObject {
    @Published var menu: WeeklyMenuResponse?
    @Published var loading = false
    @Published var error: String?

    func load(token: String) async {
        loading = true
        error = nil
        defer { loading = false }
        do {
            menu = try await APIService.weeklyMenu(token: token)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func refresh(token: String) async {
        await load(token: token)
    }
}
