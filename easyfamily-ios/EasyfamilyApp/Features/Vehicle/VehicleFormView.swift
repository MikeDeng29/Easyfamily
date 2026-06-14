import SwiftUI

struct VehicleFormView: View {
    @EnvironmentObject private var session: AuthSession
    @ObservedObject var viewModel: VehicleViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var plate: String = ""
    @State private var brand: String = ""
    @State private var model: String = ""
    @State private var year: String = ""

    private var canSave: Bool {
        !plate.trimmingCharacters(in: .whitespaces).isEmpty
            && !brand.trimmingCharacters(in: .whitespaces).isEmpty
            && !model.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                TextField("例: 京A12345", text: $plate)
                    .onChange(of: plate) { plate = String(plate.prefix(16)) }
                TextField("例: 丰田", text: $brand)
                    .onChange(of: brand) { brand = String(brand.prefix(32)) }
                TextField("例: 卡罗拉", text: $model)
                    .onChange(of: model) { model = String(model.prefix(64)) }
                TextField("例: 2023（可选）", text: $year)
                    .keyboardType(.numberPad)
                    .onChange(of: year) { year = String(year.filter(\.isNumber).prefix(4)) }
            }
            .navigationTitle("添加车辆")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") {
                        guard let token = session.accessToken else { return }
                        Task {
                            await viewModel.createVehicle(
                                token: token, plate: plate, brand: brand, model: model,
                                year: year.isEmpty ? nil : Int(year)
                            )
                            dismiss()
                        }
                    }
                    .disabled(!canSave)
                }
            }
        }
    }
}
