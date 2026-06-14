import SwiftUI

struct MainTabView: View {
    var body: some View {
        TabView {
            ChatView()
                .tabItem { Label("对话", systemImage: "bubble.left.and.bubble.right.fill") }

            MineView()
                .tabItem { Label("我的", systemImage: "person.crop.circle") }
        }
        .tint(AppPalette.coral)
    }
}
