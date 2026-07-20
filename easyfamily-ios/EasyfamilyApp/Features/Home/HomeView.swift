import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = HomeViewModel()
    @Binding var selectedTab: Int

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    greetingSection
                    shortcutsCard
                    billCard
                    menuCard
                    vehicleCard
                    Spacer(minLength: 40)
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }
            .background(AppPalette.background)
            .navigationBarHidden(true)
            .task {
                guard let token = session.accessToken else { return }
                await viewModel.load(token: token)
            }
        }
    }

    // MARK: - Greeting

    private var greetingSection: some View {
        HStack(spacing: 12) {
            let avatarId = viewModel.userProfile?.butlerAvatarId ?? 1
            Image(systemName: ButlerAvatar.icon(for: avatarId))
                .font(.system(size: 22, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: 48, height: 48)
                .background(ButlerAvatar.color(for: avatarId))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text("\(greetingText)，\(viewModel.userProfile?.nickname ?? "青鸟家庭")")
                    .font(.headline)
                    .foregroundColor(AppPalette.textPrimary)
                Text(todayLabel)
                    .font(.subheadline)
                    .foregroundColor(AppPalette.textSecondary)
            }
            Spacer()
        }
    }

    private var greetingText: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 6..<12: return "早上好"
        case 12..<18: return "下午好"
        case 18..<21: return "晚上好"
        default: return "夜深了"
        }
    }

    private var todayLabel: String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "zh_CN")
        f.dateFormat = "M月d日 EEEE"
        return f.string(from: Date())
    }

    // MARK: - Shortcuts

    private var shortcutsCard: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 12) {
            NavigationLink(destination: FamilyView().environmentObject(session)) {
                shortcutCell(title: "大家庭", icon: "house.fill", color: AppPalette.coral, bg: AppPalette.softCoral)
            }
            .buttonStyle(.plain)

            NavigationLink(destination: VehicleListView().environmentObject(session)) {
                shortcutCell(title: "车辆", icon: "car.fill", color: AppPalette.amber, bg: AppPalette.softAmber)
            }
            .buttonStyle(.plain)

            NavigationLink(destination: BillListView().environmentObject(session)) {
                shortcutCell(title: "账单", icon: "yensign.circle.fill", color: AppPalette.violet, bg: AppPalette.softViolet)
            }
            .buttonStyle(.plain)

            NavigationLink(destination: WeeklyMenuView().environmentObject(session)) {
                shortcutCell(title: "每周菜单", icon: "leaf.fill", color: AppPalette.green, bg: AppPalette.softGreen)
            }
            .buttonStyle(.plain)

            Button { selectedTab = 1 } label: {
                shortcutCell(title: "AI管家", icon: "sparkles", color: AppPalette.violet, bg: AppPalette.softViolet)
            }
            .buttonStyle(.plain)

            NavigationLink(destination: PhoneView().environmentObject(session)) {
                shortcutCell(title: "手机号", icon: "phone.fill", color: AppPalette.coral, bg: AppPalette.softCoral)
            }
            .buttonStyle(.plain)
        }
        .padding(16)
        .background(AppPalette.surface)
        .cornerRadius(16)
    }

    private func shortcutCell(title: String, icon: String, color: Color, bg: Color) -> some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 20, weight: .semibold))
                .foregroundColor(color)
                .frame(width: 44, height: 44)
                .background(bg)
                .cornerRadius(12)
            Text(title)
                .font(.caption)
                .foregroundColor(AppPalette.textPrimary)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Bill Card

    private var billCard: some View {
        NavigationLink(destination: BillListView().environmentObject(session)) {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: "yensign.circle.fill").foregroundColor(AppPalette.coral)
                    Text("本月支出").font(.subheadline).foregroundColor(AppPalette.textPrimary)
                    Spacer()
                    Image(systemName: "chevron.right").font(.caption).foregroundColor(AppPalette.textSecondary)
                }
                if viewModel.billLoading {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(AppPalette.coral.opacity(0.2))
                        .frame(width: 120, height: 36)
                } else if let stats = viewModel.billStats {
                    Text("¥ \(stats.totalExpense, specifier: "%.0f")")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(AppPalette.coral)
                    Text("本月结余 ¥\(stats.netSavings, specifier: "%.0f")")
                        .font(.caption)
                        .foregroundColor(AppPalette.textSecondary)
                } else {
                    Text("暂无账单，去记一笔？")
                        .font(.subheadline)
                        .foregroundColor(AppPalette.textSecondary)
                }
            }
            .padding(16)
            .background(AppPalette.softCoral)
            .cornerRadius(16)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Menu Card

    private var menuCard: some View {
        NavigationLink(destination: WeeklyMenuView().environmentObject(session)) {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Image(systemName: "leaf.fill").foregroundColor(AppPalette.green)
                    Text(menuTitle).font(.subheadline).bold().foregroundColor(AppPalette.green)
                    Spacer()
                    Image(systemName: "chevron.right").font(.caption).foregroundColor(AppPalette.textSecondary)
                }
                if viewModel.menuLoading {
                    VStack(spacing: 6) {
                        ForEach(0..<3, id: \.self) { _ in
                            RoundedRectangle(cornerRadius: 4)
                                .fill(AppPalette.green.opacity(0.15))
                                .frame(maxWidth: .infinity)
                                .frame(height: 16)
                        }
                    }
                } else if let menu = viewModel.todayMenu {
                    mealRow(icon: "sunrise.fill", iconColor: AppPalette.amber, label: "早", content: menu.breakfast)
                    mealRow(icon: "sun.max.fill", iconColor: AppPalette.coral, label: "午", content: menu.lunch)
                    mealRow(icon: "moon.fill", iconColor: AppPalette.violet, label: "晚", content: menu.dinner)
                } else {
                    HStack(spacing: 8) {
                        Image(systemName: "fork.knife").foregroundColor(AppPalette.green.opacity(0.5))
                        Text("点击生成本周菜单").font(.subheadline).foregroundColor(AppPalette.textSecondary)
                    }
                }
            }
            .padding(16)
            .background(AppPalette.softGreen)
            .cornerRadius(16)
        }
        .buttonStyle(.plain)
    }

    private var menuTitle: String {
        if let menu = viewModel.todayMenu {
            return "今日菜单 · \(menu.dayLabel)"
        }
        return "本周菜单"
    }

    private func mealRow(icon: String, iconColor: Color, label: String, content: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon).font(.caption).foregroundColor(iconColor).frame(width: 14)
            Text(label).font(.caption).foregroundColor(AppPalette.textSecondary).frame(width: 14)
            Text(content).font(.subheadline).foregroundColor(AppPalette.textPrimary).lineLimit(1)
        }
    }

    // MARK: - Vehicle Card

    @ViewBuilder
    private var vehicleCard: some View {
        if viewModel.vehicleLoading {
            RoundedRectangle(cornerRadius: 16)
                .fill(AppPalette.softAmber)
                .frame(height: 80)
        } else if viewModel.vehicles.isEmpty {
            NavigationLink(destination: VehicleListView().environmentObject(session)) {
                HStack(spacing: 12) {
                    Image(systemName: "car.fill")
                        .font(.system(size: 22))
                        .foregroundColor(AppPalette.amber.opacity(0.5))
                    Text("添加车辆，轻松跟踪保养记录")
                        .font(.subheadline)
                        .foregroundColor(AppPalette.textSecondary)
                    Spacer()
                }
                .padding(16)
                .background(AppPalette.softAmber)
                .cornerRadius(16)
            }
            .buttonStyle(.plain)
        } else {
            NavigationLink(destination: VehicleListView().environmentObject(session)) {
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Image(systemName: "car.fill").foregroundColor(AppPalette.amber)
                        Text(viewModel.vehicles[0].plateNumber)
                            .font(.subheadline).bold()
                            .foregroundColor(AppPalette.textPrimary)
                        Spacer()
                        if viewModel.vehicles.count > 1 {
                            Text("共 \(viewModel.vehicles.count) 辆")
                                .font(.caption)
                                .foregroundColor(AppPalette.textSecondary)
                        }
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundColor(AppPalette.textSecondary)
                    }
                    if let record = viewModel.latestRecord {
                        let days = daysSince(record.serviceDate)
                        HStack(spacing: 6) {
                            Circle().fill(warningColor(days: days)).frame(width: 8, height: 8)
                            Text("距上次保养已 \(days) 天")
                                .font(.subheadline)
                                .foregroundColor(AppPalette.textPrimary)
                        }
                        Text("上次保养：\(record.serviceDate)\(record.shopName.map { " · \($0)" } ?? "")")
                            .font(.caption)
                            .foregroundColor(AppPalette.textSecondary)
                    } else {
                        Text("暂无保养记录")
                            .font(.subheadline)
                            .foregroundColor(AppPalette.textSecondary)
                    }
                }
                .padding(16)
                .background(AppPalette.softAmber)
                .cornerRadius(16)
            }
            .buttonStyle(.plain)
        }
    }

    private func daysSince(_ dateStr: String) -> Int {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        guard let date = f.date(from: dateStr) else { return 0 }
        return Calendar.current.dateComponents([.day], from: date, to: Date()).day ?? 0
    }

    private func warningColor(days: Int) -> Color {
        if days > 180 { return AppPalette.error }
        if days > 90 { return AppPalette.amber }
        return AppPalette.success
    }
}
