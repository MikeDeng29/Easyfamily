import Foundation

@MainActor
class WeeklyMenuViewModel: ObservableObject {
    @Published var menu: WeeklyMenuResponse?
    @Published var loading = false
    @Published var error: String?
    @Published var likedDishes: Set<String> = []

    private let defaultsKey: String = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-'W'ww"
        f.locale = Locale(identifier: "zh_CN")
        return "likedDishes-\(f.string(from: Date()))"
    }()

    init() {
        // Restore likes for the current ISO week from UserDefaults.
        let stored = UserDefaults.standard.stringArray(forKey: defaultsKey) ?? []
        likedDishes = Set(stored)
    }

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

    func toggleLike(token: String, dishName: String) {
        guard !likedDishes.contains(dishName) else { return }
        likedDishes.insert(dishName)
        UserDefaults.standard.set(Array(likedDishes), forKey: defaultsKey)
        Task {
            try? await APIService.markDishPreference(token: token, dishName: dishName)
        }
    }
}
