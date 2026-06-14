import SwiftUI

struct BillStatsView: View {
    @ObservedObject var viewModel: BillViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let stats = viewModel.stats {
                    VStack(spacing: 4) {
                        Text("¥\(stats.totalAmount, specifier: "%.2f")")
                            .font(.largeTitle.bold())
                            .foregroundColor(AppPalette.coral)
                        Text("共 \(stats.count) 笔支出")
                            .font(.subheadline)
                            .foregroundColor(AppPalette.textSecondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(20)
                    .background(AppPalette.surface)
                    .cornerRadius(16)

                    Text("分类明细").font(.headline)

                    ForEach(stats.byCategory) { cat in
                        let percentage = stats.totalAmount > 0 ? cat.amount / stats.totalAmount : 0
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text("\(BillCategoryIcon.emoji(for: cat.category)) \(cat.category)")
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
                                    .fill(AppPalette.softCoral)
                                    .frame(height: 8)
                                    .overlay(alignment: .leading) {
                                        RoundedRectangle(cornerRadius: 4)
                                            .fill(AppPalette.coral)
                                            .frame(width: geo.size.width * CGFloat(percentage), height: 8)
                                    }
                            }
                            .frame(height: 8)
                        }
                        .padding(12)
                        .background(AppPalette.surface)
                        .cornerRadius(12)
                    }
                } else {
                    Text("暂无统计数据")
                        .foregroundColor(AppPalette.textSecondary)
                }
            }
            .padding(16)
        }
        .background(AppPalette.background)
        .navigationTitle("支出统计")
    }
}
