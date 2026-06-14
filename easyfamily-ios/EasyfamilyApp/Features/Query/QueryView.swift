import SwiftUI

struct QueryView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = QueryViewModel()

    @State private var phone: String = ""
    @State private var name: String = ""
    @State private var idCardNo: String = ""
    @State private var showPhonePicker = false

    private var isPhoneValid: Bool { phone.count == 11 }
    private var isNameValid: Bool { name.trimmingCharacters(in: .whitespaces).count >= 2 }
    private var isIdCardValid: Bool { idCardNo.isEmpty || idCardNo.count == 15 || idCardNo.count == 18 }
    private var canVerify: Bool { !viewModel.loading && isPhoneValid && isNameValid && isIdCardValid }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("手机号实名校验")
                    .font(.title3.bold())

                HStack {
                    TextField("手机号", text: $phone)
                        .keyboardType(.numberPad)
                        .padding(12)
                        .background(AppPalette.background)
                        .cornerRadius(10)
                        .onChange(of: phone) { phone = String(phone.filter(\.isNumber).prefix(11)) }

                    if !viewModel.phones.isEmpty {
                        Button("选择") { showPhonePicker = true }
                            .confirmationDialog("选择手机号", isPresented: $showPhonePicker) {
                                ForEach(viewModel.phones) { item in
                                    Button(item.phone) { phone = item.phone }
                                }
                                Button("新建手机号（手动输入）") { phone = "" }
                            }
                    }
                }

                TextField("请输入真实姓名", text: $name)
                    .padding(12)
                    .background(AppPalette.background)
                    .cornerRadius(10)
                    .onChange(of: name) { name = String(name.prefix(30)) }

                TextField("可不填；填写时请输入15或18位", text: $idCardNo)
                    .padding(12)
                    .background(AppPalette.background)
                    .cornerRadius(10)
                    .onChange(of: idCardNo) {
                        idCardNo = String(idCardNo.filter { $0.isNumber || $0 == "X" || $0 == "x" }.prefix(18)).uppercased()
                    }

                Button {
                    guard let token = session.accessToken else { return }
                    Task { await viewModel.verifyRealName(token: token, phone: phone, name: name.trimmingCharacters(in: .whitespaces), idCardNo: idCardNo) }
                } label: {
                    if viewModel.loading {
                        ProgressView().frame(maxWidth: .infinity)
                    } else {
                        Text("开始校验").frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(AppPalette.coral)
                .disabled(!canVerify)

                if let result = viewModel.queryResult {
                    VStack(alignment: .leading, spacing: 6) {
                        resultRow("手机号", result.phone)
                        resultRow("姓名", result.name)
                        resultRow("身份证号", result.idCardMasked ?? "-")
                        resultRow("实名结果", result.verified ? "通过" : "未通过")
                        resultRow("结果来源", result.source ?? "-")
                    }
                    .padding(14)
                    .background(AppPalette.surface)
                    .cornerRadius(14)
                }

                if !viewModel.info.isEmpty {
                    Text(viewModel.info)
                        .font(.caption)
                        .foregroundColor(AppPalette.textSecondary)
                }
            }
            .padding(16)
        }
        .background(AppPalette.background)
        .navigationTitle("查询")
        .task {
            if let token = session.accessToken {
                await viewModel.loadPhones(token: token)
                if phone.isEmpty {
                    phone = viewModel.defaultPhone
                }
            }
        }
    }

    private func resultRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label).foregroundColor(AppPalette.textSecondary)
            Spacer()
            Text(value).foregroundColor(AppPalette.textPrimary)
        }
        .font(.subheadline)
    }
}
