import SwiftUI

private let incomeGreen = Color(red: 0.2, green: 0.7, blue: 0.4)

struct FinancialHealthView: View {
    @EnvironmentObject private var session: AuthSession
    @State private var viewModel = FinanceViewModel()
    @State private var selectedTab: Int = 0

    var body: some View {
        VStack(spacing: 0) {
            // Tab picker
            Picker("视图", selection: $selectedTab) {
                Text("财务健康").tag(0)
                Text("家庭账单").tag(1)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(AppPalette.surface)

            if selectedTab == 0 {
                healthTab
            } else {
                FamilyBillView(viewModel: viewModel)
            }
        }
        .background(AppPalette.background)
        .navigationTitle("财务健康")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                monthPicker
            }
        }
        .task {
            await loadData()
        }
        .onChange(of: viewModel.selectedMonth) {
            Task { await loadData() }
        }
    }

    // MARK: - Month Picker

    private var monthPicker: some View {
        Menu {
            ForEach(recentMonths(), id: \.self) { month in
                Button(month) {
                    viewModel.selectedMonth = month
                }
            }
        } label: {
            HStack(spacing: 4) {
                Text(viewModel.selectedMonth)
                    .font(.subheadline)
                Image(systemName: "chevron.down")
                    .font(.caption2)
            }
            .foregroundColor(AppPalette.violet)
        }
    }

    // MARK: - Health Tab

    private var healthTab: some View {
        ScrollView {
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.top, 40)
            } else if let report = viewModel.healthReport {
                VStack(spacing: 16) {
                    healthScoreCard(report: report)
                    netWorthCard(report: report)
                    cashFlowCard(report: report)
                    debtPressureCard(report: report)
                    if !report.suggestions.isEmpty {
                        suggestionsCard(suggestions: report.suggestions)
                    }
                }
                .padding(16)
            } else if let error = viewModel.error {
                VStack(spacing: 12) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.largeTitle)
                        .foregroundColor(AppPalette.coral)
                    Text(error)
                        .font(.subheadline)
                        .foregroundColor(AppPalette.textSecondary)
                        .multilineTextAlignment(.center)
                    Button("重试") {
                        Task { await loadData() }
                    }
                    .font(.subheadline)
                    .foregroundColor(AppPalette.violet)
                }
                .padding(.top, 60)
                .padding(.horizontal, 32)
            } else {
                Text("暂无财务健康数据")
                    .font(.subheadline)
                    .foregroundColor(AppPalette.textSecondary)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 60)
            }
        }
        .background(AppPalette.background)
    }

    // MARK: - Health Score Card

    private func healthScoreCard(report: FinancialHealthReport) -> some View {
        let scoreColor = levelColor(report.healthLevel)
        return VStack(spacing: 16) {
            ZStack {
                Circle()
                    .stroke(scoreColor.opacity(0.15), lineWidth: 12)
                    .frame(width: 120, height: 120)
                Circle()
                    .trim(from: 0, to: CGFloat(report.healthScore) / 100.0)
                    .stroke(scoreColor, style: StrokeStyle(lineWidth: 12, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .frame(width: 120, height: 120)
                    .animation(.easeOut(duration: 0.8), value: report.healthScore)
                VStack(spacing: 4) {
                    Text("\(report.healthScore)")
                        .font(.system(size: 36, weight: .bold))
                        .foregroundColor(scoreColor)
                    Text("分")
                        .font(.caption)
                        .foregroundColor(AppPalette.textSecondary)
                }
            }
            Text(report.healthLevel)
                .font(.headline)
                .foregroundColor(scoreColor)
                .padding(.horizontal, 16)
                .padding(.vertical, 6)
                .background(scoreColor.opacity(0.1))
                .clipShape(Capsule())
        }
        .frame(maxWidth: .infinity)
        .padding(20)
        .background(AppPalette.surface)
        .cornerRadius(16)
    }

    // MARK: - Net Worth Card

    private func netWorthCard(report: FinancialHealthReport) -> some View {
        let netWorthColor = report.netWorth >= 0 ? incomeGreen : AppPalette.coral
        let totalAssets = (report.totalAssets as NSDecimalNumber).doubleValue
        let totalLiabilities = (report.totalLiabilities as NSDecimalNumber).doubleValue
        let totalForRatio = totalAssets + totalLiabilities
        let assetsRatio = totalForRatio > 0 ? totalAssets / totalForRatio : 1.0

        return VStack(alignment: .leading, spacing: 14) {
            Text("净资产").font(.headline)
            VStack(spacing: 4) {
                Text("¥\(formatDecimal(report.netWorth))")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(netWorthColor)
                    .frame(maxWidth: .infinity, alignment: .center)
            }
            // Assets vs Liabilities bar
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Circle().fill(incomeGreen).frame(width: 8, height: 8)
                    Text("总资产").font(.caption).foregroundColor(AppPalette.textSecondary)
                    Spacer()
                    Text("¥\(formatDecimal(report.totalAssets))")
                        .font(.subheadline.bold())
                        .foregroundColor(incomeGreen)
                }
                HStack {
                    Circle().fill(AppPalette.coral).frame(width: 8, height: 8)
                    Text("总负债").font(.caption).foregroundColor(AppPalette.textSecondary)
                    Spacer()
                    Text("¥\(formatDecimal(report.totalLiabilities))")
                        .font(.subheadline.bold())
                        .foregroundColor(AppPalette.coral)
                }
                GeometryReader { geo in
                    HStack(spacing: 2) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(incomeGreen)
                            .frame(width: geo.size.width * CGFloat(assetsRatio), height: 10)
                        RoundedRectangle(cornerRadius: 4)
                            .fill(AppPalette.coral)
                            .frame(width: geo.size.width * CGFloat(1 - assetsRatio), height: 10)
                    }
                }
                .frame(height: 10)
            }
        }
        .padding(16)
        .background(AppPalette.surface)
        .cornerRadius(16)
    }

    // MARK: - Cash Flow Card

    private func cashFlowCard(report: FinancialHealthReport) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("现金流").font(.headline)
            HStack(spacing: 0) {
                cashFlowColumn(title: "月收入", amount: report.monthlyIncome, color: incomeGreen)
                Divider().frame(height: 44)
                cashFlowColumn(title: "月支出", amount: report.monthlyExpense, color: AppPalette.coral)
                Divider().frame(height: 44)
                cashFlowColumn(
                    title: "净结余",
                    amount: report.netSavings,
                    color: report.netSavings >= 0 ? incomeGreen : AppPalette.coral
                )
            }
            Divider()
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("储蓄率").font(.caption).foregroundColor(AppPalette.textSecondary)
                    if let rate = report.savingsRate {
                        Text("\(Int((rate as NSDecimalNumber).doubleValue * 100))%")
                            .font(.subheadline.bold())
                            .foregroundColor(incomeGreen)
                    } else {
                        Text("--").font(.subheadline.bold()).foregroundColor(AppPalette.textSecondary)
                    }
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 4) {
                    Text("应急基金").font(.caption).foregroundColor(AppPalette.textSecondary)
                    Text(String(format: "%.1f 个月", report.emergencyFundMonths))
                        .font(.subheadline.bold())
                        .foregroundColor(emergencyFundColor(report.emergencyFundMonths))
                }
            }
        }
        .padding(16)
        .background(AppPalette.surface)
        .cornerRadius(16)
    }

    private func cashFlowColumn(title: String, amount: Decimal, color: Color) -> some View {
        VStack(spacing: 4) {
            Text(title).font(.caption).foregroundColor(AppPalette.textSecondary)
            Text("¥\(formatDecimal(amount))").font(.footnote.bold()).foregroundColor(color)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Debt Pressure Card

    private func debtPressureCard(report: FinancialHealthReport) -> some View {
        let ratio = report.debtToIncomeRatio
        let ratioColor: Color = ratio > 0.5 ? AppPalette.error : ratio > 0.3 ? AppPalette.amber : incomeGreen

        return VStack(alignment: .leading, spacing: 14) {
            Text("负债压力").font(.headline)
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("月还款额").font(.caption).foregroundColor(AppPalette.textSecondary)
                    Text("¥\(formatDecimal(report.totalMonthlyPayment))")
                        .font(.subheadline.bold())
                        .foregroundColor(AppPalette.textPrimary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 4) {
                    Text("负债收入比").font(.caption).foregroundColor(AppPalette.textSecondary)
                    Text("\(Int(ratio * 100))%")
                        .font(.subheadline.bold())
                        .foregroundColor(ratioColor)
                }
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(ratioColor.opacity(0.15))
                        .frame(height: 10)
                    RoundedRectangle(cornerRadius: 6)
                        .fill(ratioColor)
                        .frame(width: geo.size.width * CGFloat(min(ratio, 1.0)), height: 10)
                        .animation(.easeOut(duration: 0.6), value: ratio)
                }
            }
            .frame(height: 10)
            if ratio > 0.5 {
                HStack(spacing: 6) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundColor(AppPalette.error)
                        .font(.caption)
                    Text("负债收入比超过50%，建议控制新增负债")
                        .font(.caption)
                        .foregroundColor(AppPalette.error)
                }
            }
        }
        .padding(16)
        .background(AppPalette.surface)
        .cornerRadius(16)
    }

    // MARK: - Suggestions Card

    private func suggestionsCard(suggestions: [String]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("AI 建议").font(.headline)
            ForEach(Array(suggestions.enumerated()), id: \.offset) { _, suggestion in
                HStack(alignment: .top, spacing: 10) {
                    Image(systemName: "lightbulb.fill")
                        .font(.subheadline)
                        .foregroundColor(AppPalette.amber)
                    Text(suggestion)
                        .font(.subheadline)
                        .foregroundColor(AppPalette.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                if suggestion != suggestions.last {
                    Divider()
                }
            }
        }
        .padding(16)
        .background(AppPalette.surface)
        .cornerRadius(16)
    }

    // MARK: - Helpers

    private func loadData() async {
        guard let token = session.accessToken else { return }
        await viewModel.load(token: token)
    }

    private func levelColor(_ level: String) -> Color {
        switch level {
        case "危险": return AppPalette.error
        case "警告": return AppPalette.amber
        case "良好": return incomeGreen
        case "优秀": return AppPalette.violet
        default: return incomeGreen
        }
    }

    private func emergencyFundColor(_ months: Double) -> Color {
        if months >= 6 { return incomeGreen }
        if months >= 3 { return AppPalette.amber }
        return AppPalette.error
    }

    private func formatDecimal(_ value: Decimal) -> String {
        let d = (value as NSDecimalNumber).doubleValue
        if abs(d) >= 10000 {
            return String(format: "%.1f万", d / 10000)
        }
        return String(format: "%.0f", d)
    }

    private func recentMonths() -> [String] {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM"
        var months: [String] = []
        let calendar = Calendar.current
        let now = Date()
        for i in 0..<12 {
            if let date = calendar.date(byAdding: .month, value: -i, to: now) {
                months.append(formatter.string(from: date))
            }
        }
        return months
    }
}
