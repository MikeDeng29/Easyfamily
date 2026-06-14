import SwiftUI

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
                    HStack {
                        VStack(alignment: .leading) {
                            Text("本月支出").font(.caption).foregroundColor(AppPalette.textSecondary)
                            Text("¥\(stats.totalAmount, specifier: "%.2f")")
                                .font(.title2.bold())
                                .foregroundColor(AppPalette.coral)
                        }
                        Spacer()
                        VStack(alignment: .trailing) {
                            Text("账单数").font(.caption).foregroundColor(AppPalette.textSecondary)
                            Text("\(stats.count)").font(.title3.bold())
                        }
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
                        HStack {
                            Text(BillCategoryIcon.emoji(for: bill.category))
                                .font(.title2)
                            VStack(alignment: .leading) {
                                Text(bill.note?.isEmpty == false ? bill.note! : bill.category)
                                    .font(.subheadline.bold())
                                Text("\(bill.category) · \(bill.billedAt)")
                                    .font(.caption)
                                    .foregroundColor(AppPalette.textSecondary)
                            }
                            Spacer()
                            Text("¥\(bill.amount, specifier: "%.2f")")
                                .font(.subheadline.bold())
                                .foregroundColor(AppPalette.coral)
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
                await viewModel.load(token: token)
            }
        }
    }
}
