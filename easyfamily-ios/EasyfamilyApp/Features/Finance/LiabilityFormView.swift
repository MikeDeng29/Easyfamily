import SwiftUI

struct LiabilityFormView: View {
    @EnvironmentObject private var session: AuthSession
    @ObservedObject var viewModel: LiabilityViewModel
    @Environment(\.dismiss) private var dismiss

    /// Pass an existing item to edit; nil means create.
    var item: LiabilityItem?

    @State private var name: String = ""
    @State private var liabilityType: String = "personal_loan"
    @State private var balanceText: String = ""
    @State private var monthlyPaymentText: String = ""
    @State private var interestRateText: String = ""
    @State private var note: String = ""
    @State private var isSaving: Bool = false
    @State private var saveError: String?

    private let liabilityTypes: [(key: String, label: String)] = [
        ("mortgage",      "房贷"),
        ("car_loan",      "车贷"),
        ("credit_card",   "信用卡"),
        ("personal_loan", "个人贷款"),
        ("other",         "其他"),
    ]

    private var isEditing: Bool { item != nil }

    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty
            && (Decimal(string: balanceText) ?? -1) >= 0
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("基本信息") {
                    TextField("负债名称", text: $name)
                    Picker("类型", selection: $liabilityType) {
                        ForEach(liabilityTypes, id: \.key) { type in
                            Text(type.label).tag(type.key)
                        }
                    }
                }

                Section("金额") {
                    TextField("剩余余额（元）", text: $balanceText)
                        .keyboardType(.decimalPad)
                    TextField("月还款额（元，可选）", text: $monthlyPaymentText)
                        .keyboardType(.decimalPad)
                    TextField("年利率 %（可选）", text: $interestRateText)
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
            .navigationTitle(isEditing ? "编辑负债" : "添加负债")
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
                    liabilityType = item.liabilityType
                    balanceText = "\(item.balance)"
                    if let mp = item.monthlyPayment { monthlyPaymentText = "\(mp)" }
                    if let ir = item.interestRate { interestRateText = "\(ir)" }
                    note = item.note ?? ""
                }
            }
        }
    }

    private func save() {
        guard let token = session.accessToken else { return }
        guard let balance = Decimal(string: balanceText) else { return }
        let monthlyPayment = monthlyPaymentText.isEmpty ? nil : Decimal(string: monthlyPaymentText)
        let interestRate = interestRateText.isEmpty ? nil : Decimal(string: interestRateText)
        isSaving = true
        saveError = nil
        Task {
            do {
                if let item {
                    try await viewModel.update(
                        token: token, id: item.id,
                        name: name.trimmingCharacters(in: .whitespaces),
                        liabilityType: liabilityType,
                        balance: balance,
                        monthlyPayment: monthlyPayment ?? nil,
                        interestRate: interestRate ?? nil,
                        note: note
                    )
                } else {
                    try await viewModel.create(
                        token: token,
                        name: name.trimmingCharacters(in: .whitespaces),
                        liabilityType: liabilityType,
                        balance: balance,
                        monthlyPayment: monthlyPayment ?? nil,
                        interestRate: interestRate ?? nil,
                        note: note
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
