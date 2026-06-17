import SwiftUI

struct AssetListView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = AssetViewModel()
    @State private var showAddForm = false
    @State private var editingItem: AssetItem?

    private let green = Color(hex: 0x2E7D32)
    private let softGreen = Color(hex: 0xE8F5E9)
    private let lightGreen = Color(hex: 0x4CAF50)
    private let tealBlue = Color(hex: 0x1565C0)
    private let softBlue = Color(hex: 0xE3F2FD)
    private let orange = Color(hex: 0xE65100)
    private let softOrange = Color(hex: 0xFFF3E0)

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // 总资产卡片
                summaryCard

                if let error = viewModel.error {
                    Text(error).font(.caption).foregroundColor(AppPalette.error)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                }

                if viewModel.isLoading {
                    ProgressView().frame(maxWidth: .infinity).padding(.top, 20)
                } else if viewModel.assets.isEmpty {
                    emptyStateView
                } else {
                    LazyVStack(spacing: 0) {
                        ForEach(viewModel.assets) { asset in
                            assetRow(asset)
                                .onTapGesture { editingItem = asset }
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
        .navigationTitle("家庭资产")
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
            AssetFormView(viewModel: viewModel)
                .environmentObject(session)
        }
        .sheet(item: $editingItem) { item in
            AssetFormView(viewModel: viewModel, item: item)
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
            Image(systemName: "building.columns")
                .font(.system(size: 48))
                .foregroundColor(softGreen.opacity(0.6))
                .padding(.top, 40)

            Text("开始记录家庭资产")
                .font(.headline)
                .foregroundColor(AppPalette.textPrimary)

            Text("录入房产、存款、基金、车辆价值等，\n帮助计算净资产和财务健康评分")
                .font(.subheadline)
                .foregroundColor(AppPalette.textSecondary)
                .multilineTextAlignment(.center)

            Button {
                showAddForm = true
            } label: {
                Label("添加第一条资产", systemImage: "plus")
                    .font(.subheadline.bold())
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(green)
                    .cornerRadius(24)
            }
            .padding(.top, 4)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 32)
    }

    // MARK: - Summary card

    private var summaryCard: some View {
        VStack(spacing: 4) {
            Text("总资产")
                .font(.subheadline)
                .foregroundColor(.white.opacity(0.85))
            Text(formatDecimal(viewModel.totalValue))
                .font(.system(size: 36, weight: .bold))
                .foregroundColor(.white)
            Text("元")
                .font(.caption)
                .foregroundColor(.white.opacity(0.7))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
        .background(
            LinearGradient(
                colors: [Color(hex: 0x2E7D32), Color(hex: 0x66BB6A)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .cornerRadius(16)
        .padding(.horizontal, 16)
    }

    // MARK: - Asset row

    private func assetRow(_ asset: AssetItem) -> some View {
        let (icon, iconColor, iconBg) = iconInfo(for: asset.assetType)
        return HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(iconColor)
                .frame(width: 32, height: 32)
                .background(iconBg)
                .clipShape(RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 2) {
                Text(asset.name)
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(AppPalette.textPrimary)
                Text(assetTypeLabel(asset.assetType))
                    .font(.caption)
                    .foregroundColor(AppPalette.textSecondary)
            }

            Spacer()

            Text(formatDecimal(asset.value))
                .font(.subheadline.weight(.semibold))
                .foregroundColor(green)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) {
                guard let token = session.accessToken else { return }
                Task { try? await viewModel.delete(token: token, id: asset.id) }
            } label: {
                Label("删除", systemImage: "trash")
            }
        }
    }

    // MARK: - Helpers

    private func iconInfo(for type: String) -> (String, Color, Color) {
        switch type {
        case "cash":     return ("banknote.fill",              lightGreen, softGreen)
        case "savings":  return ("building.columns.fill",      green,      softGreen)
        case "fund":     return ("chart.line.uptrend.xyaxis",  tealBlue,   softBlue)
        case "stock":    return ("chart.bar.fill",             tealBlue,   softBlue)
        case "property": return ("house.fill",                 orange,     softOrange)
        case "vehicle":  return ("car.fill",                   orange,     softOrange)
        default:         return ("ellipsis.circle.fill",       AppPalette.textSecondary, AppPalette.disabledSurface)
        }
    }

    private func assetTypeLabel(_ type: String) -> String {
        switch type {
        case "cash":     return "现金"
        case "savings":  return "存款"
        case "fund":     return "基金"
        case "stock":    return "股票"
        case "property": return "房产"
        case "vehicle":  return "车辆"
        default:         return "其他"
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
