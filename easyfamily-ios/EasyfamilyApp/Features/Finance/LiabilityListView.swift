import SwiftUI

struct LiabilityListView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = LiabilityViewModel()
    @State private var showAddForm = false
    @State private var editingItem: LiabilityItem?

    private let deepRed   = Color(hex: 0xB71C1C)
    private let red       = Color(hex: 0xE53935)
    private let softRed   = Color(hex: 0xFFEBEE)
    private let deepOrange = Color(hex: 0xE65100)
    private let orange    = Color(hex: 0xFF6D00)
    private let softOrange = Color(hex: 0xFFF3E0)

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // 汇总卡片
                summaryCard

                if let error = viewModel.error {
                    Text(error).font(.caption).foregroundColor(AppPalette.error)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                }

                if viewModel.isLoading {
                    ProgressView().frame(maxWidth: .infinity).padding(.top, 20)
                } else if viewModel.liabilities.isEmpty {
                    emptyStateView
                } else {
                    LazyVStack(spacing: 0) {
                        ForEach(viewModel.liabilities) { liability in
                            liabilityRow(liability)
                                .onTapGesture { editingItem = liability }
                            Divider().padding(.leading, 60)
                        }
                    }
                    .background(AppPalette.surface)
                    .cornerRadius(16)
                    .padding(.horizontal, 16)
                }

                Spacer(minLength: 40)
            }
            .padding(.top, 16)
        }
        .background(AppPalette.background)
        .navigationTitle("家庭负债")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showAddForm = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddForm) {
            LiabilityFormView(viewModel: viewModel)
                .environmentObject(session)
        }
        .sheet(item: $editingItem) { item in
            LiabilityFormView(viewModel: viewModel, item: item)
                .environmentObject(session)
        }
        .task {
            if let token = session.accessToken {
                await viewModel.load(token: token)
            }
        }
    }

    // MARK: - Empty state

    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Image(systemName: "creditcard")
                .font(.system(size: 48))
                .foregroundColor(softRed.opacity(0.8))
                .padding(.top, 40)

            Text("记录家庭负债")
                .font(.headline)
                .foregroundColor(AppPalette.textPrimary)

            Text("录入房贷、车贷、信用卡等负债，\n结合资产一起计算净资产和健康评分")
                .font(.subheadline)
                .foregroundColor(AppPalette.textSecondary)
                .multilineTextAlignment(.center)

            Button {
                showAddForm = true
            } label: {
                Label("添加第一条负债", systemImage: "plus")
                    .font(.subheadline.bold())
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(red)
                    .cornerRadius(24)
            }
            .padding(.top, 4)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 32)
    }

    // MARK: - Summary card

    private var summaryCard: some View {
        VStack(spacing: 12) {
            VStack(spacing: 4) {
                Text("总负债")
                    .font(.subheadline)
                    .foregroundColor(.white.opacity(0.85))
                Text(formatDecimal(viewModel.totalBalance))
                    .font(.system(size: 36, weight: .bold))
                    .foregroundColor(.white)
                Text("元")
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.7))
            }

            Divider().background(Color.white.opacity(0.3))

            HStack {
                Spacer()
                VStack(spacing: 2) {
                    Text("月还款总额")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.75))
                    Text("\(formatDecimal(viewModel.totalMonthlyPayment)) 元/月")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(.white)
                }
                Spacer()
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
        .padding(.horizontal, 16)
        .background(
            LinearGradient(
                colors: [Color(hex: 0xB71C1C), Color(hex: 0xEF5350)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .cornerRadius(16)
        .padding(.horizontal, 16)
    }

    // MARK: - Liability row

    private func liabilityRow(_ liability: LiabilityItem) -> some View {
        let (icon, iconColor, iconBg) = iconInfo(for: liability.liabilityType)
        return HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(iconColor)
                .frame(width: 32, height: 32)
                .background(iconBg)
                .clipShape(RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 2) {
                Text(liability.name)
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(AppPalette.textPrimary)
                HStack(spacing: 8) {
                    Text(liabilityTypeLabel(liability.liabilityType))
                        .font(.caption)
                        .foregroundColor(AppPalette.textSecondary)
                    if let mp = liability.monthlyPayment, mp > 0 {
                        Text("月还 \(formatDecimal(mp))")
                            .font(.caption)
                            .foregroundColor(orange)
                    }
                }
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 2) {
                Text(formatDecimal(liability.balance))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(red)
                if let ir = liability.interestRate, ir > 0 {
                    Text("\(formatDecimal(ir))%/年")
                        .font(.caption)
                        .foregroundColor(AppPalette.textSecondary)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) {
                guard let token = session.accessToken else { return }
                Task { try? await viewModel.delete(token: token, id: liability.id) }
            } label: {
                Label("删除", systemImage: "trash")
            }
        }
    }

    // MARK: - Helpers

    private func iconInfo(for type: String) -> (String, Color, Color) {
        switch type {
        case "mortgage":      return ("house.fill",       red,    softRed)
        case "car_loan":      return ("car.fill",         red,    softRed)
        case "credit_card":   return ("creditcard.fill",  orange, softOrange)
        case "personal_loan": return ("person.fill",      orange, softOrange)
        default:              return ("ellipsis.circle.fill", AppPalette.textSecondary, AppPalette.disabledSurface)
        }
    }

    private func liabilityTypeLabel(_ type: String) -> String {
        switch type {
        case "mortgage":      return "房贷"
        case "car_loan":      return "车贷"
        case "credit_card":   return "信用卡"
        case "personal_loan": return "个人贷款"
        default:              return "其他"
        }
    }

    private func formatDecimal(_ value: Decimal) -> String {
        let ns = value as NSDecimalNumber
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return formatter.string(from: ns) ?? "\(value)"
    }
}
