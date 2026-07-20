import SwiftUI

struct WeeklyMenuView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = WeeklyMenuViewModel()

    private let vegGreen = Color(hex: 0x2E7D32)
    private let softGreen = Color(hex: 0xE8F5E9)

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if viewModel.loading {
                    VStack(spacing: 12) {
                        ProgressView()
                        Text("正在为您生成本周菜单…")
                            .font(.subheadline)
                            .foregroundColor(AppPalette.textSecondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 60)
                } else if let error = viewModel.error {
                    VStack(spacing: 12) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 32))
                            .foregroundColor(AppPalette.amber)
                        Text(error)
                            .font(.subheadline)
                            .foregroundColor(AppPalette.textSecondary)
                            .multilineTextAlignment(.center)
                        Button("重试") {
                            guard let token = session.accessToken else { return }
                            Task { await viewModel.refresh(token: token) }
                        }
                        .foregroundColor(vegGreen)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 60)
                } else if let menu = viewModel.menu {
                    headerCard(menu: menu)
                    ForEach(menu.days, id: \.date) { day in
                        dayCard(day: day)
                    }
                    Spacer(minLength: 32)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
        }
        .background(AppPalette.background)
        .navigationTitle("每周菜单")
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    guard let token = session.accessToken else { return }
                    Task { await viewModel.refresh(token: token) }
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
                .disabled(viewModel.loading)
            }
        }
        .task {
            if viewModel.menu == nil, let token = session.accessToken {
                await viewModel.load(token: token)
            }
        }
    }

    private func headerCard(menu: WeeklyMenuResponse) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Image(systemName: "leaf.fill")
                    .foregroundColor(vegGreen)
                Text(menu.city.isEmpty ? "本周菜单" : "\(menu.city) · 本周菜单")
                    .font(.headline)
                    .foregroundColor(AppPalette.textPrimary)
                Spacer()
                Text(weekLabel(from: menu.weekOf))
                    .font(.caption)
                    .foregroundColor(AppPalette.textSecondary)
            }
            if !menu.seasonTip.isEmpty {
                Text(menu.seasonTip)
                    .font(.subheadline)
                    .foregroundColor(AppPalette.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(16)
        .background(softGreen)
        .cornerRadius(14)
    }

    private func dayCard(day: DayMenuDto) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(day.dayLabel)
                    .font(.headline)
                    .foregroundColor(AppPalette.textPrimary)
                Spacer()
                Text(shortDate(from: day.date))
                    .font(.caption)
                    .foregroundColor(AppPalette.textSecondary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)

            Divider()

            mealRow(icon: "sunrise.fill", color: AppPalette.amber, label: "早餐", content: day.breakfast)
            Divider().padding(.leading, 52)
            mealRow(icon: "sun.max.fill", color: AppPalette.coral, label: "午餐", content: day.lunch)
            Divider().padding(.leading, 52)
            mealRow(icon: "moon.fill", color: AppPalette.violet, label: "晚餐", content: day.dinner)

            if !day.keyVegetables.isEmpty {
                Divider()
                HStack(spacing: 6) {
                    Image(systemName: "leaf")
                        .font(.caption)
                        .foregroundColor(vegGreen)
                    Text("当季时蔬：")
                        .font(.caption)
                        .foregroundColor(AppPalette.textSecondary)
                    Text(day.keyVegetables.joined(separator: "、"))
                        .font(.caption)
                        .foregroundColor(vegGreen)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
            }
        }
        .background(AppPalette.surface)
        .cornerRadius(14)
    }

    private func mealRow(icon: String, color: Color, label: String, content: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 14))
                .foregroundColor(color)
                .frame(width: 28)
                .padding(.top, 1)
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.caption)
                    .foregroundColor(AppPalette.textSecondary)
                Text(content)
                    .font(.subheadline)
                    .foregroundColor(AppPalette.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
    }

    private func weekLabel(from weekOf: String) -> String {
        guard !weekOf.isEmpty else { return "" }
        return weekOf.prefix(10).description
    }

    private func shortDate(from date: String) -> String {
        guard date.count >= 10 else { return date }
        let mmdd = date.dropFirst(5).prefix(5)
        return String(mmdd).replacingOccurrences(of: "-", with: "/")
    }
}
