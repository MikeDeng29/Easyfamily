import SwiftUI

struct RecordListView: View {
    @EnvironmentObject private var session: AuthSession
    @ObservedObject var viewModel: VehicleViewModel
    let vehicle: VehicleItemDto

    @State private var showAddRecord = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("\(vehicle.plateNumber) 保养记录").font(.title3.bold())
                    Spacer()
                    NavigationLink("统计") {
                        VehicleStatsView(viewModel: viewModel)
                    }
                    Button("+ 记录") { showAddRecord = true }
                }

                if let stats = viewModel.stats {
                    HStack {
                        VStack(alignment: .leading) {
                            Text("累计花费").font(.caption).foregroundColor(AppPalette.textSecondary)
                            Text("¥\(stats.totalCost, specifier: "%.2f")")
                                .font(.title2.bold())
                                .foregroundColor(AppPalette.coral)
                        }
                        Spacer()
                        Text("\(stats.totalRecords) 次保养 · \(stats.totalItems) 个项目")
                            .font(.caption)
                            .foregroundColor(AppPalette.textSecondary)
                    }
                    .padding(14)
                    .background(AppPalette.surface)
                    .cornerRadius(14)
                }

                if viewModel.loading {
                    ProgressView().frame(maxWidth: .infinity).padding(.top, 20)
                } else if viewModel.records.isEmpty {
                    Text("暂无保养记录")
                        .font(.subheadline)
                        .foregroundColor(AppPalette.textSecondary)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 40)
                } else {
                    ForEach(viewModel.records) { record in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text(record.serviceDate).font(.subheadline.bold())
                                Spacer()
                                Text("¥\(record.totalCost, specifier: "%.2f")")
                                    .font(.subheadline.bold())
                                    .foregroundColor(AppPalette.coral)
                            }
                            if let mileage = record.mileageKm, mileage > 0 {
                                Text("里程: \(mileage)km").font(.caption).foregroundColor(AppPalette.textSecondary)
                            }
                            if let shop = record.shopName, !shop.isEmpty {
                                Text(shop).font(.caption).foregroundColor(AppPalette.textSecondary)
                            }
                            ForEach(record.items) { item in
                                HStack {
                                    Text("[\(item.category)] \(item.itemName)").font(.caption)
                                    Spacer()
                                    Text("¥\(item.cost, specifier: "%.2f")").font(.caption)
                                }
                            }
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
        .navigationTitle(vehicle.plateNumber)
        .sheet(isPresented: $showAddRecord) {
            RecordFormView(viewModel: viewModel, vehicle: vehicle)
        }
        .task {
            if let token = session.accessToken {
                await viewModel.selectVehicle(token: token, vehicleId: vehicle.id)
            }
        }
    }
}
