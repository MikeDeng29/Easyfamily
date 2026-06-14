import SwiftUI

struct PhoneView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = PhoneViewModel()
    @State private var phone: String = ""
    @State private var smsCode: String = ""
    @FocusState private var phoneFocused: Bool

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("我的手机号管理")
                    .font(.title3.bold())

                TextField("手机号", text: $phone)
                    .keyboardType(.numberPad)
                    .focused($phoneFocused)
                    .padding(12)
                    .background(AppPalette.background)
                    .cornerRadius(10)
                    .onChange(of: phone) { phone = String(phone.filter(\.isNumber).prefix(11)) }

                TextField("验证码（绑定时需要）", text: $smsCode)
                    .keyboardType(.numberPad)
                    .padding(12)
                    .background(AppPalette.background)
                    .cornerRadius(10)
                    .onChange(of: smsCode) { smsCode = String(smsCode.filter(\.isNumber).prefix(6)) }

                HStack(spacing: 12) {
                    Button("绑定") {
                        guard let token = session.accessToken else { return }
                        Task { await viewModel.bind(token: token, phone: phone, smsCode: smsCode) }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppPalette.coral)
                    .disabled(phone.count != 11 || smsCode.isEmpty || viewModel.loading)

                    Button("解绑") {
                        guard let token = session.accessToken else { return }
                        Task { await viewModel.unbind(token: token, phone: phone) }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppPalette.coralDark)
                    .disabled(phone.count != 11 || viewModel.loading)
                }

                Button("设为主号") {
                    guard let token = session.accessToken else { return }
                    Task { await viewModel.setPrimary(token: token, phone: phone) }
                }
                .disabled(phone.count != 11 || viewModel.loading)

                Divider()

                Text("当前管理的手机号")
                    .font(.subheadline.bold())

                ForEach(viewModel.phones) { item in
                    HStack {
                        Text(item.phone)
                        if item.isPrimary {
                            Text("主号")
                                .font(.caption)
                                .padding(.horizontal, 6)
                                .background(AppPalette.softCoral)
                                .foregroundColor(AppPalette.coral)
                                .cornerRadius(6)
                        }
                        Spacer()
                        Text(item.status)
                            .font(.caption)
                            .foregroundColor(AppPalette.textSecondary)
                    }
                    .padding(.vertical, 4)
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
        .navigationTitle("手机号")
        .task {
            phoneFocused = true
            if let token = session.accessToken {
                await viewModel.refresh(token: token)
            }
        }
    }
}
