import SwiftUI

private let incomeGreen = Color(red: 0.2, green: 0.7, blue: 0.4)
private let statusYellow = Color(red: 0.95, green: 0.77, blue: 0.06)

struct BillStatsView: View {
    @ObservedObject var viewModel: BillViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // ── 区块一：本月收支概览 ──
                if let stats = viewModel.stats {
                    overviewSection(stats: stats)
                    categorySection(stats: stats)
                } else {
                    Text("暂无统计数据")
                        .foregroundColor(AppPalette.textSecondary)
                }

                // ── 区块三：财务安全 ──
                if let report = viewModel.securityReport {
                    securitySection(report: report)
                }
            }
            .padding(16)
        }
        .background(AppPalette.background)
        .navigationTitle("收支统计")
    }

    // MARK: - 区块一：本月收支概览

    private func overviewSection(stats: BillStatsDto) -> some View {
        VStack(spacing: 8) {
            let savingsColor = stats.netSavings >= 0 ? incomeGreen : AppPalette.coral
            Text("¥\(stats.netSavings, specifier: "%.2f")")
                .font(.largeTitle.bold())
                .foregroundColor(savingsColor)
            Text("收入 ¥\(stats.totalIncome, specifier: "%.2f") · 支出 ¥\(stats.totalExpense, specifier: "%.2f")")
                .font(.subheadline)
                .foregroundColor(AppPalette.textSecondary)
            if let rate = stats.savingsRate {
                Text("储蓄率 \(Int(rate * 100))%")
                    .font(.footnote)
                    .foregroundColor(AppPalette.textSecondary)
            } else {
                Text("暂无收入数据")
                    .font(.footnote)
                    .foregroundColor(AppPalette.textSecondary)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(20)
        .background(AppPalette.surface)
        .cornerRadius(16)
    }

    // MARK: - 区块二：分类明细

    private func categorySection(stats: BillStatsDto) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            let expenseItems = stats.byCategory.filter { $0.direction == "expense" }
            let incomeItems = stats.byCategory.filter { $0.direction == "income" }

            if !expenseItems.isEmpty {
                Text("支出明细").font(.headline)
                ForEach(expenseItems) { cat in
                    categoryRow(cat: cat, total: stats.totalExpense, color: AppPalette.coral)
                }
            }

            if !incomeItems.isEmpty {
                Text("收入明细").font(.headline)
                ForEach(incomeItems) { cat in
                    categoryRow(cat: cat, total: stats.totalIncome, color: incomeGreen)
                }
            }

            if expenseItems.isEmpty && incomeItems.isEmpty {
                Text("暂无分类数据")
                    .foregroundColor(AppPalette.textSecondary)
            }
        }
    }

    private func categoryRow(cat: BillCategoryStatDto, total: Double, color: Color) -> some View {
        let percentage = total > 0 ? cat.amount / total : 0
        return VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(cat.category)
                    .font(.subheadline.bold())
                Text("(\(cat.count))")
                    .font(.caption)
                    .foregroundColor(AppPalette.textSecondary)
                Spacer()
                Text("¥\(cat.amount, specifier: "%.2f")")
                    .font(.subheadline.bold())
                Text("\(Int(percentage * 100))%")
                    .font(.caption)
                    .foregroundColor(AppPalette.textSecondary)
            }
            GeometryReader { geo in
                RoundedRectangle(cornerRadius: 4)
                    .fill(color.opacity(0.15))
                    .frame(height: 8)
                    .overlay(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(color)
                            .frame(width: geo.size.width * CGFloat(percentage), height: 8)
                    }
            }
            .frame(height: 8)
        }
        .padding(12)
        .background(AppPalette.surface)
        .cornerRadius(12)
    }

    // MARK: - 区块三：财务安全

    private func securitySection(report: SecurityReportDto) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("财务安全").font(.headline)

            if !report.hasEnoughData {
                Text("记录 2 个月以上的收入数据后，财务安全报告将自动生成")
                    .font(.subheadline)
                    .foregroundColor(AppPalette.textSecondary)
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(AppPalette.surface)
                    .cornerRadius(14)
            } else {
                VStack(spacing: 12) {
                    // 健康评分
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("健康评分").font(.subheadline).foregroundColor(AppPalette.textSecondary)
                            Text(report.healthLevel).font(.caption).foregroundColor(AppPalette.textSecondary)
                        }
                        Spacer()
                        Text("\(report.healthScore)")
                            .font(.system(size: 36, weight: .bold))
                            .foregroundColor(statusColor(report.savingsRateStatus))
                    }
                    .padding(16)
                    .background(AppPalette.surface)
                    .cornerRadius(14)

                    // 储蓄率
                    securityMetricRow(
                        label: "储蓄率",
                        value: report.savingsRate.map { "\(Int($0 * 100))%" } ?? "--",
                        status: report.savingsRateStatus,
                        note: report.period
                    )

                    // 备用金覆盖
                    securityMetricRow(
                        label: "备用金覆盖",
                        value: String(format: "%.1f个月", report.emergencyFundMonths),
                        status: report.emergencyFundStatus,
                        note: "月均支出 ¥\(String(format: "%.0f", report.avgMonthlyExpense))"
                    )

                    // 月均收支
                    HStack(spacing: 8) {
                        metricMiniCard(title: "月均收入", amount: report.avgMonthlyIncome, color: incomeGreen)
                        metricMiniCard(title: "月均支出", amount: report.avgMonthlyExpense, color: AppPalette.coral)
                        metricMiniCard(title: "月均结余", amount: report.avgMonthlySavings, color: report.avgMonthlySavings >= 0 ? incomeGreen : AppPalette.coral)
                    }
                }
            }
        }
    }

    private func securityMetricRow(label: String, value: String, status: Int, note: String) -> some View {
        HStack {
            Circle()
                .fill(statusColor(status))
                .frame(width: 10, height: 10)
            Text(label)
                .font(.subheadline)
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(value).font(.subheadline.bold())
                Text(note).font(.caption).foregroundColor(AppPalette.textSecondary)
            }
        }
        .padding(14)
        .background(AppPalette.surface)
        .cornerRadius(12)
    }

    private func metricMiniCard(title: String, amount: Double, color: Color) -> some View {
        VStack(spacing: 4) {
            Text(title).font(.caption).foregroundColor(AppPalette.textSecondary)
            Text("¥\(String(format: "%.0f", amount))").font(.footnote.bold()).foregroundColor(color)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(AppPalette.surface)
        .cornerRadius(10)
    }

    // MARK: - Helpers

    private func statusColor(_ status: Int) -> Color {
        switch status {
        case 0: return AppPalette.coral
        case 1: return statusYellow
        default: return incomeGreen
        }
    }
}
