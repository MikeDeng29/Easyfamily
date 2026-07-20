import SwiftUI

struct MainTabView: View {
    @State private var selectedTab: Int = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeView(selectedTab: $selectedTab)
                .tabItem { Label("首页", systemImage: "house.fill") }
                .tag(0)

            ChatView()
                .tabItem { Label("对话", systemImage: "bubble.left.and.bubble.right.fill") }
                .tag(1)

            MineView()
                .tabItem { Label("我的", systemImage: "person.crop.circle") }
                .tag(2)
        }
        .tint(AppPalette.coral)
    }
}
