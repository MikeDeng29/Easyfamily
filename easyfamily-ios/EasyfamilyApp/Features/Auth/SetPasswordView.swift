import SwiftUI

struct SetPasswordView: View {
    let hasPassword: Bool
    let token: String
    var onSuccess: (() -> Void)?

    @Environment(\.dismiss) private var dismiss
    @State private var newPassword = ""
    @State private var confirmPassword = ""
    @State private var loading = false
    @State private var errorMessage = ""
    @State private var succeeded = false
    @FocusState private var focus: Field?

    private enum Field { case newPassword, confirmPassword }

    private var title: String { hasPassword ? "修改登录密码" : "设置登录密码" }
    private var isValid: Bool { newPassword.count >= 6 && newPassword == confirmPassword }

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("新密码")
                        .font(.subheadline)
                        .foregroundColor(AppPalette.textSecondary)
                    SecureField("至少 6 位字符", text: $newPassword)
                        .focused($focus, equals: .newPassword)
                        .submitLabel(.next)
                        .onSubmit { focus = .confirmPassword }
                        .padding(12)
                        .background(AppPalette.surface)
                        .cornerRadius(10)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .strokeBorder(focus == .newPassword ? AppPalette.coral : .clear, lineWidth: 1.5)
                        )
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("确认新密码")
                        .font(.subheadline)
                        .foregroundColor(AppPalette.textSecondary)
                    SecureField("再次输入密码", text: $confirmPassword)
                        .focused($focus, equals: .confirmPassword)
                        .submitLabel(.done)
                        .onSubmit { Task { await submit() } }
                        .padding(12)
                        .background(AppPalette.surface)
                        .cornerRadius(10)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .strokeBorder(focus == .confirmPassword ? AppPalette.coral : .clear, lineWidth: 1.5)
                        )
                    if !confirmPassword.isEmpty && confirmPassword != newPassword {
                        Text("两次输入不一致")
                            .font(.caption)
                            .foregroundColor(AppPalette.error)
                    }
                }

                if !errorMessage.isEmpty {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundColor(AppPalette.error)
                }

                Button {
                    Task { await submit() }
                } label: {
                    Group {
                        if loading {
                            ProgressView().tint(.white)
                        } else {
                            Text(title)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(isValid ? AppPalette.coral : AppPalette.disabledSurface)
                    .foregroundColor(isValid ? .white : AppPalette.textSecondary)
                    .cornerRadius(10)
                }
                .disabled(!isValid || loading)

                Spacer()
            }
            .padding(24)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
            }
            .onAppear { focus = .newPassword }
            .alert("密码设置成功", isPresented: $succeeded) {
                Button("确定") {
                    onSuccess?()
                    dismiss()
                }
            }
        }
    }

    private func submit() async {
        guard isValid else { return }
        loading = true
        errorMessage = ""
        defer { loading = false }
        do {
            try await APIService.setPassword(token: token, newPassword: newPassword)
            succeeded = true
        } catch {
            errorMessage = "设置失败：\(error.localizedDescription)"
        }
    }
}
