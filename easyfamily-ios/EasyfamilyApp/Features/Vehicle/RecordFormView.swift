import SwiftUI
import PhotosUI
import UIKit

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

    // Import from maintenance slip
    @State private var showingImagePicker = false
    @State private var selectedPhoto: PhotosPickerItem? = nil
    @State private var isImporting = false
    @State private var importError: String? = nil

    private var isDateValid: Bool {
        serviceDate.range(of: #"^\d{4}-\d{2}-\d{2}$"#, options: .regularExpression) != nil
    }
    private var canSave: Bool { isDateValid && !items.isEmpty }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    if isImporting {
                        HStack {
                            ProgressView()
                            Text("正在识别保养单...").foregroundColor(.secondary)
                        }
                    } else {
                        Button {
                            showingImagePicker = true
                        } label: {
                            Label("从保养单导入", systemImage: "doc.viewfinder")
                        }
                        .disabled(isImporting)
                    }
                }

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
            .photosPicker(isPresented: $showingImagePicker, selection: $selectedPhoto, matching: .images)
            .onChange(of: selectedPhoto) {
                guard let photo = selectedPhoto else { return }
                isImporting = true
                importError = nil
                Task {
                    defer { isImporting = false }
                    do {
                        guard let data = try await photo.loadTransferable(type: Data.self) else { return }
                        let imageData: Data
                        if data.count > 1_000_000,
                           let uiImage = UIImage(data: data),
                           let compressed = uiImage.jpegData(compressionQuality: 0.8) {
                            imageData = compressed
                        } else {
                            imageData = data
                        }
                        guard let token = session.accessToken else {
                            throw ApiError(message: "未登录")
                        }
                        let result = try await APIService.importMaintenanceRecord(
                            token: token, imageData: imageData, mimeType: "image/jpeg"
                        )
                        if let d = result.serviceDate { serviceDate = d }
                        if let m = result.mileageKm { mileageKm = String(m) }
                        if let s = result.shopName { shopName = s }
                        if let n = result.notes { notes = n }
                        for item in result.items {
                            let cat = Self.categories.contains(item.category) ? item.category : "其他"
                            let cost = item.cost.map { String(format: "%.2f", $0) } ?? ""
                            items.append(MaintenanceItemInput(
                                category: cat,
                                itemName: item.itemName,
                                cost: Double(cost) ?? 0,
                                isDiy: false
                            ))
                        }
                    } catch {
                        importError = error.localizedDescription
                    }
                }
            }
            .alert("导入失败", isPresented: Binding(
                get: { importError != nil },
                set: { if !$0 { importError = nil } }
            )) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(importError ?? "")
            }
        }
    }
}
