import SwiftUI

struct ProfileEditView: View {
    @EnvironmentObject private var session: AuthSession
    @Environment(\.dismiss) private var dismiss

    let profile: UserProfile?
    let onSave: (UserProfile) -> Void

    @State private var nickname: String = ""
    @State private var email: String = ""
    @State private var isSaving = false
    @State private var error: String? = nil

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Avatar
                    Image(systemName: "person.fill")
                        .font(.system(size: 40, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 88, height: 88)
                        .background(
                            LinearGradient(colors: [AppPalette.coral, AppPalette.violet],
                                           startPoint: .topLeading, endPoint: .bottomTrailing)
                        )
                        .clipShape(Circle())
                        .padding(.top, 24)

                    // Form fields
                    VStack(spacing: 0) {
                        formRow(label: "昵称", icon: "person.fill") {
                            TextField("请输入昵称", text: $nickname)
                                .multilineTextAlignment(.trailing)
                        }
                        Divider().padding(.leading, 60)
                        formRow(label: "手机号", icon: "phone.fill") {
                            Text(profile?.phone ?? "—")
                                .foregroundColor(AppPalette.textSecondary)
                        }
                        Divider().padding(.leading, 60)
                        formRow(label: "邮箱", icon: "envelope.fill") {
                            TextField("请输入邮箱（用于接收通知）", text: $email)
                                .multilineTextAlignment(.trailing)
                                .keyboardType(.emailAddress)
                                .textContentType(.emailAddress)
                                .autocapitalization(.none)
                        }
                    }
                    .background(AppPalette.surface)
                    .cornerRadius(16)
                    .padding(.horizontal, 16)

                    if let error {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(AppPalette.error)
                            .padding(.horizontal, 16)
                    }

                    Spacer(minLength: 32)
                }
            }
            .background(AppPalette.background)
            .navigationTitle("个人信息")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") { Task { await save() } }
                        .disabled(isSaving)
                        .overlay { if isSaving { ProgressView().scaleEffect(0.8) } }
                }
            }
        }
        .onAppear {
            nickname = profile?.nickname ?? ""
            email = profile?.email ?? ""
        }
    }

    private func formRow<Content: View>(label: String, icon: String, @ViewBuilder content: () -> Content) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(AppPalette.coral)
                .frame(width: 32, height: 32)
                .background(AppPalette.softCoral)
                .clipShape(RoundedRectangle(cornerRadius: 8))

            Text(label)
                .foregroundColor(AppPalette.textPrimary)

            Spacer()

            content()
                .foregroundColor(AppPalette.textPrimary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }

    private func save() async {
        guard let token = session.accessToken else { return }
        isSaving = true
        error = nil
        defer { isSaving = false }

        do {
            var updated: UserProfile? = nil
            let trimmedNickname = nickname.trimmingCharacters(in: .whitespacesAndNewlines)
            let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)

            if !trimmedNickname.isEmpty, trimmedNickname != profile?.nickname {
                updated = try await APIService.updateNickname(token: token, nickname: trimmedNickname)
            }
            if trimmedEmail != (profile?.email ?? "") {
                updated = try await APIService.updateEmail(token: token, email: trimmedEmail)
            }

            if let updated {
                onSave(updated)
            }
            dismiss()
        } catch {
            self.error = "保存失败：\(error.localizedDescription)"
        }
    }
}
