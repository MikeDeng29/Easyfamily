import SwiftUI

struct AssetFormView: View {
    @EnvironmentObject private var session: AuthSession
    @ObservedObject var viewModel: AssetViewModel
    @Environment(\.dismiss) private var dismiss

    /// Pass an existing item to edit; nil means create.
    var item: AssetItem?

    @State private var name: String = ""
    @State private var assetType: String = "cash"
    @State private var valueText: String = ""
    @State private var note: String = ""
    @State private var isSaving: Bool = false
    @State private var saveError: String?

    private let assetTypes: [(key: String, label: String)] = [
        ("cash",     "现金"),
        ("savings",  "存款"),
        ("fund",     "基金"),
        ("stock",    "股票"),
        ("property", "房产"),
        ("vehicle",  "车辆"),
        ("other",    "其他"),
    ]

    private var isEditing: Bool { item != nil }

    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty
            && (Decimal(string: valueText) ?? -1) >= 0
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("基本信息") {
                    TextField("资产名称", text: $name)
                    Picker("类型", selection: $assetType) {
                        ForEach(assetTypes, id: \.key) { type in
                            Text(type.label).tag(type.key)
                        }
                    }
                }

                Section("金额") {
                    TextField("金额（元）", text: $valueText)
                        .keyboardType(.decimalPad)
                }

                Section("备注（可选）") {
                    TextField("备注", text: $note, axis: .vertical)
                        .lineLimit(3...6)
                }

                if let saveError {
                    Section {
                        Text(saveError).foregroundColor(AppPalette.error).font(.caption)
                    }
                }
            }
            .navigationTitle(isEditing ? "编辑资产" : "添加资产")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isSaving {
                        ProgressView()
                    } else {
                        Button("保存") { save() }
                            .disabled(!canSave)
                    }
                }
            }
            .onAppear {
                if let item {
                    name = item.name
                    assetType = item.assetType
                    valueText = "\(item.value)"
                    note = item.note ?? ""
                }
            }
        }
    }

    private func save() {
        guard let token = session.accessToken else { return }
        guard let value = Decimal(string: valueText) else { return }
        isSaving = true
        saveError = nil
        Task {
            do {
                if let item {
                    try await viewModel.update(
                        token: token, id: item.id, name: name.trimmingCharacters(in: .whitespaces),
                        assetType: assetType, value: value, note: note
                    )
                } else {
                    try await viewModel.create(
                        token: token, name: name.trimmingCharacters(in: .whitespaces),
                        assetType: assetType, value: value, note: note
                    )
                }
                dismiss()
            } catch {
                saveError = "保存失败：\(error.localizedDescription)"
            }
            isSaving = false
        }
    }
}
