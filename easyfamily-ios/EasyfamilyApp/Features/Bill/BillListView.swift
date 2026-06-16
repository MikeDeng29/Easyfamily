import SwiftUI

private let incomeGreen = Color(red: 0.2, green: 0.7, blue: 0.4)

struct BillListView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = BillViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("账单").font(.title3.bold())
                    Spacer()
                    NavigationLink("查看统计") {
                        BillStatsView(viewModel: viewModel)
                    }
                    .font(.subheadline)
                }

                if let stats = viewModel.stats {
                    HStack(spacing: 0) {
                        summaryColumn(title: "收入", amount: stats.totalIncome, color: incomeGreen)
                        Divider().frame(height: 40)
                        summaryColumn(title: "支出", amount: stats.totalExpense, color: AppPalette.coral)
                        Divider().frame(height: 40)
                        summaryColumn(
                            title: "结余",
                            amount: stats.netSavings,
                            color: stats.netSavings >= 0 ? incomeGreen : AppPalette.coral
                        )
                    }
                    .padding(14)
                    .background(AppPalette.surface)
                    .cornerRadius(14)
                }

                if let error = viewModel.error {
                    Text(error).font(.caption).foregroundColor(AppPalette.error)
                }

                if viewModel.loading {
                    ProgressView().frame(maxWidth: .infinity).padding(.top, 20)
                } else if viewModel.bills.isEmpty {
                    Text("暂无账单记录\n在「对话」页告诉 AI 你的消费，即可自动记录")
                        .font(.subheadline)
                        .foregroundColor(AppPalette.textSecondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 40)
                } else {
                    ForEach(viewModel.bills) { bill in
                        let isIncome = bill.direction == "income"
                        let amountColor = isIncome ? incomeGreen : AppPalette.coral
                        let amountPrefix = isIncome ? "+" : "-"

                        HStack {
                            Image(systemName: isIncome ? "arrow.down.circle.fill" : "arrow.up.circle.fill")
                                .font(.title2)
                                .foregroundColor(amountColor)
                            VStack(alignment: .leading) {
                                Text(bill.note?.isEmpty == false ? bill.note! : bill.category)
                                    .font(.subheadline.bold())
                                Text("\(bill.category) · \(bill.billedAt)")
                                    .font(.caption)
                                    .foregroundColor(AppPalette.textSecondary)
                            }
                            Spacer()
                            Text("\(amountPrefix)¥\(bill.amount, specifier: "%.2f")")
                                .font(.subheadline.bold())
                                .foregroundColor(amountColor)
                            Button {
                                guard let token = session.accessToken else { return }
                                Task { await viewModel.deleteBill(token: token, id: bill.id) }
                            } label: {
                                Image(systemName: "trash")
                                    .foregroundColor(AppPalette.textSecondary)
                            }
                            .buttonStyle(.borderless)
                        }
                        .padding(12)
                        .background(AppPalette.surface)
                        .cornerRadius(12)
                    }
                }
            }
            .padding(16)
        }
        .background(AppPalette.background)
        .navigationTitle("账单")
        .task {
            if let token = session.accessToken {
                viewModel.load(token: token)
            }
        }
    }

    private func summaryColumn(title: String, amount: Double, color: Color) -> some View {
        VStack(spacing: 4) {
            Text(title)
                .font(.caption)
                .foregroundColor(AppPalette.textSecondary)
            Text("¥\(amount, specifier: "%.2f")")
                .font(.subheadline.bold())
                .foregroundColor(color)
        }
        .frame(maxWidth: .infinity)
    }
}
