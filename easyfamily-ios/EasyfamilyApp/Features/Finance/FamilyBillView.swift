import SwiftUI

private let incomeGreen = Color(red: 0.2, green: 0.7, blue: 0.4)

struct FamilyBillView: View {
    @Bindable var viewModel: FinanceViewModel

    var body: some View {
        ScrollView {
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.top, 40)
            } else if let stats = viewModel.familyStats {
                if isOnlySelf(stats) {
                    emptyState
                } else {
                    VStack(spacing: 16) {
                        familySummaryCard(stats: stats)
                        membersSection(stats: stats)
                    }
                    .padding(16)
                }
            } else if let error = viewModel.error {
                VStack(spacing: 12) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.largeTitle)
                        .foregroundColor(AppPalette.coral)
                    Text(error)
                        .font(.subheadline)
                        .foregroundColor(AppPalette.textSecondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 60)
                .padding(.horizontal, 32)
            } else {
                emptyState
            }
        }
        .background(AppPalette.background)
    }

    // MARK: - Empty State

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "person.3.fill")
                .font(.system(size: 44))
                .foregroundColor(AppPalette.textSecondary.opacity(0.4))
            Text("在「大家庭」中添加家庭成员后，\n即可查看家庭财务汇总")
                .font(.subheadline)
                .foregroundColor(AppPalette.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 60)
        .padding(.horizontal, 32)
    }

    // MARK: - Family Summary Card

    private func familySummaryCard(stats: FamilyBillStats) -> some View {
        VStack(spacing: 8) {
            Text("家庭总览").font(.headline).frame(maxWidth: .infinity, alignment: .leading)
            HStack(spacing: 0) {
                summaryColumn(title: "总收入", amount: stats.totalIncome, color: incomeGreen)
                Divider().frame(height: 44)
                summaryColumn(title: "总支出", amount: stats.totalExpense, color: AppPalette.coral)
                Divider().frame(height: 44)
                summaryColumn(
                    title: "净结余",
                    amount: stats.netSavings,
                    color: stats.netSavings >= 0 ? incomeGreen : AppPalette.coral
                )
            }
            if let rate = stats.savingsRate {
                Text("家庭储蓄率 \(Int((rate as NSDecimalNumber).doubleValue * 100))%")
                    .font(.caption)
                    .foregroundColor(AppPalette.textSecondary)
            }
        }
        .padding(16)
        .background(AppPalette.surface)
        .cornerRadius(16)
    }

    // MARK: - Members Section

    private func membersSection(stats: FamilyBillStats) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("成员明细").font(.headline)
            let familyNetSavings = (stats.netSavings as NSDecimalNumber).doubleValue
            ForEach(stats.members, id: \.memberId) { member in
                memberCard(member: member, familyNetSavings: familyNetSavings)
            }
        }
    }

    private func memberCard(member: MemberStats, familyNetSavings: Double) -> some View {
        let memberNetSavings = (member.netSavings as NSDecimalNumber).doubleValue
        let savingsRatio: Double = familyNetSavings != 0 ? memberNetSavings / familyNetSavings : 0
        let clampedRatio = max(0, min(1, savingsRatio))
        let savingsColor = memberNetSavings >= 0 ? incomeGreen : AppPalette.coral

        return VStack(alignment: .leading, spacing: 12) {
            // Header: name + relation
            HStack {
                Text(member.memberName)
                    .font(.subheadline.bold())
                    .foregroundColor(AppPalette.textPrimary)
                Text(member.relation)
                    .font(.caption)
                    .foregroundColor(AppPalette.violet)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(AppPalette.softViolet)
                    .clipShape(Capsule())
                Spacer()
            }

            // Income / Expense / Savings
            HStack(spacing: 0) {
                memberAmountColumn(title: "收入", amount: member.income, color: incomeGreen)
                Divider().frame(height: 36)
                memberAmountColumn(title: "支出", amount: member.expense, color: AppPalette.coral)
                Divider().frame(height: 36)
                memberAmountColumn(title: "结余", amount: member.netSavings, color: savingsColor)
            }

            // Savings share bar
            if familyNetSavings > 0 {
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text("结余占比")
                            .font(.caption)
                            .foregroundColor(AppPalette.textSecondary)
                        Spacer()
                        Text("\(Int(clampedRatio * 100))%")
                            .font(.caption.bold())
                            .foregroundColor(savingsColor)
                    }
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            RoundedRectangle(cornerRadius: 4)
                                .fill(savingsColor.opacity(0.15))
                                .frame(height: 6)
                            RoundedRectangle(cornerRadius: 4)
                                .fill(savingsColor)
                                .frame(width: geo.size.width * CGFloat(clampedRatio), height: 6)
                        }
                    }
                    .frame(height: 6)
                }
            }
        }
        .padding(14)
        .background(AppPalette.surface)
        .cornerRadius(14)
    }

    private func memberAmountColumn(title: String, amount: Decimal, color: Color) -> some View {
        VStack(spacing: 4) {
            Text(title).font(.caption).foregroundColor(AppPalette.textSecondary)
            Text("¥\(formatDecimal(amount))").font(.footnote.bold()).foregroundColor(color)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Helpers

    private func isOnlySelf(_ stats: FamilyBillStats) -> Bool {
        stats.members.count <= 1 && (stats.members.first?.memberId == "self" || stats.members.isEmpty)
    }

    private func summaryColumn(title: String, amount: Decimal, color: Color) -> some View {
        VStack(spacing: 4) {
            Text(title).font(.caption).foregroundColor(AppPalette.textSecondary)
            Text("¥\(formatDecimal(amount))").font(.subheadline.bold()).foregroundColor(color)
        }
        .frame(maxWidth: .infinity)
    }

    private func formatDecimal(_ value: Decimal) -> String {
        let d = (value as NSDecimalNumber).doubleValue
        if abs(d) >= 10000 {
            return String(format: "%.1f万", d / 10000)
        }
        return String(format: "%.0f", d)
    }
}
