import SwiftUI

struct MineView: View {
    @EnvironmentObject private var session: AuthSession

    private struct Destination: Identifiable {
        let id: String
        let title: String
        let icon: String
        let color: Color
    }

    private let destinations: [Destination] = [
        Destination(id: "family", title: "大家庭", icon: "house.fill", color: AppPalette.coral),
        Destination(id: "phone", title: "手机号", icon: "phone.fill", color: AppPalette.violet),
        Destination(id: "query", title: "查询", icon: "magnifyingglass", color: AppPalette.coral),
        Destination(id: "vehicle", title: "车辆", icon: "car.fill", color: AppPalette.amber),
        Destination(id: "bill", title: "账单", icon: "yensign.circle.fill", color: AppPalette.violet)
    ]

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(destinations) { destination in
                        NavigationLink(value: destination.id) {
                            Label(destination.title, systemImage: destination.icon)
                                .foregroundColor(AppPalette.textPrimary)
                                .symbolRenderingMode(.multicolor)
                        }
                    }
                }

                Section {
                    Button(role: .destructive) {
                        session.logout()
                    } label: {
                        Text("退出登录")
                            .foregroundColor(AppPalette.error)
                    }
                }
            }
            .navigationTitle("我的")
            .navigationDestination(for: String.self) { id in
                switch id {
                case "family": FamilyView()
                case "phone": PhoneView()
                case "query": QueryView()
                case "vehicle": VehicleListView()
                case "bill": BillListView()
                default: EmptyView()
                }
            }
        }
    }
}
