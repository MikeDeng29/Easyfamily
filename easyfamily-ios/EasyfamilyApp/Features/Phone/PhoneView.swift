import SwiftUI

struct PhoneView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = PhoneViewModel()
    @State private var phone: String = ""
    @State private var smsCode: String = ""
    @State private var unbindTarget: PhoneItem?
    @FocusState private var phoneFocused: Bool

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("我的手机号管理")
                    .font(.title3.bold())

                VStack(alignment: .leading, spacing: 12) {
                    Text("绑定新手机号")
                        .font(.subheadline.bold())
                        .foregroundColor(AppPalette.textPrimary)

                    TextField("手机号", text: $phone)
                        .keyboardType(.numberPad)
                        .focused($phoneFocused)
                        .padding(12)
                        .background(AppPalette.background)
                        .cornerRadius(10)
                        .onChange(of: phone) { phone = String(phone.filter(\.isNumber).prefix(11)) }

                    TextField("验证码", text: $smsCode)
                        .keyboardType(.numberPad)
                        .padding(12)
                        .background(AppPalette.background)
                        .cornerRadius(10)
                        .onChange(of: smsCode) { smsCode = String(smsCode.filter(\.isNumber).prefix(6)) }

                    Button {
                        guard let token = session.accessToken else { return }
                        Task {
                            await viewModel.bind(token: token, phone: phone, smsCode: smsCode)
                            phone = ""
                            smsCode = ""
                        }
                    } label: {
                        Text("绑定新手机号")
                            .font(.subheadline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(AppPalette.coral)
                            .cornerRadius(10)
                    }
                    .disabled(phone.count != 11 || smsCode.isEmpty || viewModel.loading)
                    .opacity((phone.count != 11 || smsCode.isEmpty || viewModel.loading) ? 0.5 : 1)
                }
                .padding(16)
                .background(AppPalette.surface)
                .cornerRadius(16)

                Text("当前管理的手机号")
                    .font(.subheadline.bold())
                    .foregroundColor(AppPalette.textPrimary)

                VStack(spacing: 0) {
                    ForEach(viewModel.phones) { item in
                        phoneRow(item)
                        if item.id != viewModel.phones.last?.id {
                            Divider().padding(.leading, 16)
                        }
                    }
                }
                .background(AppPalette.surface)
                .cornerRadius(16)

                if !viewModel.info.isEmpty {
                    Text(viewModel.info)
                        .font(.caption)
                        .foregroundColor(viewModel.infoIsError ? AppPalette.error : AppPalette.success)
                }
            }
            .padding(16)
        }
        .background(AppPalette.background)
        .navigationTitle("手机号")
        .task {
            if let token = session.accessToken {
                await viewModel.refresh(token: token)
            }
        }
        .confirmationDialog(
            "确定解绑 \(unbindTarget?.phone ?? "") 吗？解绑后将无法用此号码登录",
            isPresented: Binding(get: { unbindTarget != nil }, set: { if !$0 { unbindTarget = nil } }),
            titleVisibility: .visible
        ) {
            Button("解绑", role: .destructive) {
                guard let token = session.accessToken, let target = unbindTarget else { return }
                Task { await viewModel.unbind(token: token, phone: target.phone) }
            }
            Button("取消", role: .cancel) {}
        }
    }

    private func phoneRow(_ item: PhoneItem) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text(item.phone)
                        .foregroundColor(AppPalette.textPrimary)
                    if item.isPrimary {
                        Text("主号")
                            .font(.caption2)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(AppPalette.softCoral)
                            .foregroundColor(AppPalette.coral)
                            .cornerRadius(6)
                    }
                }
                Text(item.status)
                    .font(.caption)
                    .foregroundColor(AppPalette.textSecondary)
            }

            Spacer()

            if !item.isPrimary {
                Button("设为主号") {
                    guard let token = session.accessToken else { return }
                    Task { await viewModel.setPrimary(token: token, phone: item.phone) }
                }
                .font(.caption)
                .foregroundColor(AppPalette.violet)
                .disabled(viewModel.loading)
            }

            Button("解绑") {
                unbindTarget = item
            }
            .font(.caption)
            .foregroundColor(AppPalette.error)
            .disabled(viewModel.loading)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}
