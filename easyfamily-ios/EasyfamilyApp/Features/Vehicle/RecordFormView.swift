import SwiftUI

struct RecordFormView: View {
    @EnvironmentObject private var session: AuthSession
    @ObservedObject var viewModel: VehicleViewModel
    let vehicle: VehicleItemDto
    @Environment(\.dismiss) private var dismiss

    private static let categories = ["机油", "刹车", "轮胎", "空调", "滤芯", "火花塞", "蓄电池", "其他"]

    @State private var serviceDate: String = ""
    @State private var mileageKm: String = ""
    @State private var shopName: String = ""
    @State private var notes: String = ""

    @State private var items: [MaintenanceItemInput] = []
    @State private var newItemCategory: String = categories[0]
    @State private var newItemName: String = ""
    @State private var newItemCost: String = ""

    private var isDateValid: Bool {
        serviceDate.range(of: #"^\d{4}-\d{2}-\d{2}$"#, options: .regularExpression) != nil
    }
    private var canSave: Bool { isDateValid && !items.isEmpty }

    var body: some View {
        NavigationStack {
            Form {
                Section("基本信息") {
                    TextField("服务日期 YYYY-MM-DD", text: $serviceDate)
                        .onChange(of: serviceDate) { serviceDate = String(serviceDate.prefix(10)) }
                    TextField("里程（km，可选）", text: $mileageKm)
                        .keyboardType(.numberPad)
                        .onChange(of: mileageKm) { mileageKm = String(mileageKm.filter(\.isNumber).prefix(7)) }
                    TextField("门店名称（可选）", text: $shopName)
                        .onChange(of: shopName) { shopName = String(shopName.prefix(128)) }
                }

                Section("保养项目") {
                    ForEach(Array(items.enumerated()), id: \.offset) { index, item in
                        HStack {
                            Text("[\(item.category)] \(item.itemName)")
                            Spacer()
                            Text("¥\(item.cost, specifier: "%.2f")")
                            Button {
                                items.remove(at: index)
                            } label: {
                                Image(systemName: "trash").foregroundColor(AppPalette.error)
                            }
                            .buttonStyle(.borderless)
                        }
                    }

                    Picker("分类", selection: $newItemCategory) {
                        ForEach(Self.categories, id: \.self) { Text($0) }
                    }
                    TextField("项目名称", text: $newItemName)
                        .onChange(of: newItemName) { newItemName = String(newItemName.prefix(32)) }
                    TextField("费用", text: $newItemCost)
                        .keyboardType(.decimalPad)
                        .onChange(of: newItemCost) {
                            newItemCost = newItemCost.filter { $0.isNumber || $0 == "." }
                        }
                    Button("+ 添加项目") {
                        guard let cost = Double(newItemCost), !newItemName.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                        items.append(MaintenanceItemInput(category: newItemCategory, itemName: newItemName, cost: cost, isDiy: false))
                        newItemName = ""
                        newItemCost = ""
                    }
                }

                Section("备注") {
                    TextField("备注（可选）", text: $notes)
                        .onChange(of: notes) { notes = String(notes.prefix(200)) }
                }
            }
            .navigationTitle("添加保养记录")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") {
                        guard let token = session.accessToken else { return }
                        Task {
                            await viewModel.createRecord(
                                token: token, vehicleId: vehicle.id, serviceDate: serviceDate,
                                mileageKm: mileageKm.isEmpty ? nil : Int(mileageKm),
                                shopName: shopName.isEmpty ? nil : shopName,
                                notes: notes.isEmpty ? nil : notes,
                                items: items
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
