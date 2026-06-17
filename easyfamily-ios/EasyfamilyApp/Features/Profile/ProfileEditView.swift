import SwiftUI

struct ProfileEditView: View {
    @EnvironmentObject private var session: AuthSession
    @Environment(\.dismiss) private var dismiss

    let profile: UserProfile?
    let onSave: (UserProfile) -> Void

    @State private var nickname: String = ""
    @State private var email: String = ""
    @State private var butlerName: String = ""
    @State private var butlerAvatarId: Int = 1
    @State private var butlerPersona: String = "warm"
    @State private var isSaving = false
    @State private var error: String? = nil

    private let profileStore = UserProfileStore()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    // Butler avatar preview
                    Image(systemName: ButlerAvatar.icon(for: butlerAvatarId))
                        .font(.system(size: 40, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 88, height: 88)
                        .background(ButlerAvatar.color(for: butlerAvatarId))
                        .clipShape(Circle())
                        .animation(.spring(response: 0.3), value: butlerAvatarId)
                        .padding(.top, 28)
                        .padding(.bottom, 24)

                    // Personal info section
                    sectionLabel("个人信息")

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
                            TextField("用于接收通知", text: $email)
                                .multilineTextAlignment(.trailing)
                                .keyboardType(.emailAddress)
                                .textContentType(.emailAddress)
                                .autocapitalization(.none)
                        }
                    }
                    .background(AppPalette.surface)
                    .cornerRadius(16)
                    .padding(.horizontal, 16)

                    // Butler section
                    sectionLabel("我的管家")
                        .padding(.top, 20)

                    VStack(spacing: 0) {
                        // Avatar picker
                        VStack(alignment: .leading, spacing: 10) {
                            Text("形象")
                                .font(.caption)
                                .foregroundColor(AppPalette.textSecondary)
                                .padding(.leading, 16)
                                .padding(.top, 12)

                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 12) {
                                    ForEach(ButlerAvatar.allIds, id: \.self) { avatarId in
                                        Button {
                                            butlerAvatarId = avatarId
                                        } label: {
                                            Image(systemName: ButlerAvatar.icon(for: avatarId))
                                                .font(.system(size: 18, weight: .semibold))
                                                .foregroundColor(.white)
                                                .frame(width: 44, height: 44)
                                                .background(ButlerAvatar.color(for: avatarId))
                                                .clipShape(Circle())
                                                .overlay(
                                                    Circle()
                                                        .strokeBorder(
                                                            butlerAvatarId == avatarId
                                                                ? AppPalette.textPrimary
                                                                : Color.clear,
                                                            lineWidth: 2.5
                                                        )
                                                        .padding(-3)
                                                )
                                        }
                                    }
                                }
                                .padding(.horizontal, 16)
                                .padding(.bottom, 12)
                            }
                        }

                        Divider().padding(.leading, 16)

                        formRow(label: "名字", icon: "sparkles") {
                            TextField("青鸟管家", text: $butlerName)
                                .multilineTextAlignment(.trailing)
                        }

                        Divider().padding(.leading, 60)

                        // Persona picker
                        VStack(alignment: .leading, spacing: 0) {
                            Text("性格")
                                .font(.caption)
                                .foregroundColor(AppPalette.textSecondary)
                                .padding(.leading, 16)
                                .padding(.top, 12)
                                .padding(.bottom, 4)

                            ForEach(Array(ButlerPersona.all.enumerated()), id: \.element) { index, persona in
                                if index > 0 {
                                    Divider().padding(.leading, 16)
                                }
                                Button {
                                    butlerPersona = persona
                                } label: {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(ButlerPersona.label(for: persona))
                                                .font(.subheadline.bold())
                                                .foregroundColor(AppPalette.textPrimary)
                                            Text(ButlerPersona.description(for: persona))
                                                .font(.caption)
                                                .foregroundColor(AppPalette.textSecondary)
                                        }
                                        Spacer()
                                        if butlerPersona == persona {
                                            Image(systemName: "checkmark.circle.fill")
                                                .foregroundColor(AppPalette.coral)
                                        }
                                    }
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 10)
                                    .background(
                                        butlerPersona == persona
                                            ? AppPalette.softCoral
                                            : Color.clear
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.bottom, 8)
                    }
                    .background(AppPalette.surface)
                    .cornerRadius(16)
                    .padding(.horizontal, 16)

                    if let error {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(AppPalette.error)
                            .padding(.horizontal, 16)
                            .padding(.top, 12)
                    }

                    Spacer(minLength: 40)
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
            butlerName = profile?.butlerName ?? ""
            butlerAvatarId = profile?.butlerAvatarId ?? 1
            butlerPersona = profile?.butlerPersona ?? "warm"
        }
    }

    @ViewBuilder
    private func sectionLabel(_ text: String) -> some View {
        Text(text)
            .font(.footnote.bold())
            .foregroundColor(AppPalette.textSecondary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 32)
            .padding(.bottom, 8)
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

            let effectiveName = {
                let t = butlerName.trimmingCharacters(in: .whitespacesAndNewlines)
                return t.isEmpty ? "青鸟管家" : String(t.prefix(10))
            }()
            let butlerChanged = effectiveName != (profile?.butlerName ?? "青鸟管家")
                || butlerAvatarId != (profile?.butlerAvatarId ?? 1)
                || butlerPersona != (profile?.butlerPersona ?? "warm")

            if butlerChanged {
                let req = UpdateButlerRequest(butlerName: effectiveName, butlerAvatarId: butlerAvatarId, butlerPersona: butlerPersona)
                updated = try await APIService.updateButler(token: token, request: req)
                profileStore.saveButlerIdentity(name: effectiveName, avatarId: butlerAvatarId, persona: butlerPersona)
                NotificationCenter.default.post(
                    name: .butlerIdentityUpdated,
                    object: nil,
                    userInfo: ["name": effectiveName, "avatarId": butlerAvatarId, "persona": butlerPersona]
                )
            }

            if let updated { onSave(updated) }
            dismiss()
        } catch {
            self.error = "保存失败：\(error.localizedDescription)"
        }
    }
}
