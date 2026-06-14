import SwiftUI

struct VehicleListView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = VehicleViewModel()
    @State private var showAddForm = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("我的车辆").font(.title3.bold())
                    Spacer()
                    Button("+ 添加") { showAddForm = true }
                }

                if let error = viewModel.error {
                    Text(error).font(.caption).foregroundColor(AppPalette.error)
                }

                if viewModel.loading {
                    ProgressView().frame(maxWidth: .infinity).padding(.top, 20)
                } else if viewModel.vehicles.isEmpty {
                    Text("暂无车辆，点击右上角添加")
                        .font(.subheadline)
                        .foregroundColor(AppPalette.textSecondary)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 40)
                } else {
                    ForEach(viewModel.vehicles) { vehicle in
                        NavigationLink {
                            RecordListView(viewModel: viewModel, vehicle: vehicle)
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(vehicle.plateNumber)
                                        .font(.headline)
                                        .foregroundColor(AppPalette.textPrimary)
                                    let yearSuffix = (vehicle.year ?? 0) > 0 ? " (\(vehicle.year!))" : ""
                                    Text("\(vehicle.brand) \(vehicle.model)\(yearSuffix)")
                                        .font(.caption)
                                        .foregroundColor(AppPalette.textSecondary)
                                }
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .foregroundColor(AppPalette.textSecondary)
                            }
                            .padding(14)
                            .background(AppPalette.surface)
                            .cornerRadius(14)
                        }
                    }
                }
            }
            .padding(16)
        }
        .background(AppPalette.background)
        .navigationTitle("车辆")
        .sheet(isPresented: $showAddForm) {
            VehicleFormView(viewModel: viewModel)
        }
        .task {
            if let token = session.accessToken {
                await viewModel.loadVehicles(token: token)
            }
        }
    }
}
