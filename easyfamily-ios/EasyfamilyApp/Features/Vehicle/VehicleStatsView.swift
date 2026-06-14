import SwiftUI

struct VehicleStatsView: View {
    @ObservedObject var viewModel: VehicleViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("花费统计").font(.title3.bold())

                if let stats = viewModel.stats {
                    HStack(spacing: 12) {
                        statCard(title: "总花费", value: "¥\(String(format: "%.2f", stats.totalCost))", color: AppPalette.coral)
                        statCard(title: "保养次数", value: "\(stats.totalRecords)", color: AppPalette.violet)
                        statCard(title: "项目数", value: "\(stats.totalItems)", color: AppPalette.amber)
                    }

                    Text("按分类统计").font(.headline)

                    let maxCost = stats.byCategory.map(\.totalCost).max() ?? 1
                    ForEach(stats.byCategory) { cat in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text(cat.category).font(.subheadline.bold())
                                if cat.diyCount > 0 {
                                    Text("\(cat.diyCount)项DIY")
                                        .font(.caption)
                                        .foregroundColor(AppPalette.success)
                                }
                                Spacer()
                                Text("¥\(cat.totalCost, specifier: "%.2f")").font(.subheadline.bold())
                            }
                            GeometryReader { geo in
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(AppPalette.softViolet)
                                    .frame(height: 8)
                                    .overlay(alignment: .leading) {
                                        RoundedRectangle(cornerRadius: 4)
                                            .fill(AppPalette.violet)
                                            .frame(width: geo.size.width * CGFloat(maxCost > 0 ? cat.totalCost / maxCost : 0), height: 8)
                                    }
                            }
                            .frame(height: 8)
                            let percentage = stats.totalCost > 0 ? cat.totalCost / stats.totalCost : 0
                            Text("\(cat.itemCount) 项 · 占 \(Int(percentage * 100))%")
                                .font(.caption)
                                .foregroundColor(AppPalette.textSecondary)
                        }
                        .padding(12)
                        .background(AppPalette.surface)
                        .cornerRadius(12)
                    }
                } else {
                    Text("暂无统计数据").foregroundColor(AppPalette.textSecondary)
                }
            }
            .padding(16)
        }
        .background(AppPalette.background)
        .navigationTitle("花费统计")
    }

    private func statCard(title: String, value: String, color: Color) -> some View {
        VStack(spacing: 4) {
            Text(title).font(.caption).foregroundColor(AppPalette.textSecondary)
            Text(value).font(.headline).foregroundColor(color)
        }
        .frame(maxWidth: .infinity)
        .padding(12)
        .background(AppPalette.surface)
        .cornerRadius(12)
    }
}
