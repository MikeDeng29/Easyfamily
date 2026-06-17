import SwiftUI

struct FamilyView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = FamilyViewModel()
    @State private var showAddMember = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("统一管理家庭成员，便于查询和财务汇总。")
                    .font(.subheadline)
                    .foregroundColor(AppPalette.textSecondary)
                    .padding(.bottom, 4)

                if viewModel.loading {
                    ProgressView().padding(.top, 8)
                } else {
                    ForEach(viewModel.members) { member in
                        memberRow(member)
                    }

                    if let error = viewModel.error {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(AppPalette.error)
                    }
                }
            }
            .padding(16)
        }
        .background(AppPalette.background)
        .navigationTitle("大家庭")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showAddMember = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddMember) {
            AddMemberView(viewModel: viewModel)
                .environmentObject(session)
        }
        .task {
            if let token = session.accessToken {
                await viewModel.load(token: token)
            }
        }
    }

    private func memberRow(_ member: FamilyDisplayMember) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(member.name)
                    .font(.headline)
                    .foregroundColor(AppPalette.textPrimary)
                Text(member.maskedPhone)
                    .font(.subheadline)
                    .foregroundColor(AppPalette.textSecondary)
            }
            Spacer()
            Text(member.relation)
                .font(.caption)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(member.isOwner ? AppPalette.softCoral : AppPalette.background)
                .foregroundColor(member.isOwner ? AppPalette.coral : AppPalette.textPrimary)
                .cornerRadius(8)
        }
        .padding(14)
        .background(AppPalette.surface)
        .cornerRadius(14)
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            if !member.isOwner {
                Button(role: .destructive) {
                    guard let token = session.accessToken else { return }
                    Task { try? await viewModel.deleteMember(token: token, memberId: member.memberId) }
                } label: {
                    Label("删除", systemImage: "trash")
                }
            }
        }
    }
}

// MARK: - Add Member Sheet

private struct AddMemberView: View {
    @EnvironmentObject private var session: AuthSession
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var viewModel: FamilyViewModel

    @State private var name: String = ""
    @State private var phone: String = ""
    @State private var relation: String = "配偶"
    @State private var isSaving = false
    @State private var error: String? = nil

    private let relations = ["配偶", "子女", "父母", "兄弟姐妹", "其他"]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    VStack(spacing: 0) {
                        formRow(label: "姓名", icon: "person.fill") {
                            TextField("请输入姓名", text: $name)
                                .multilineTextAlignment(.trailing)
                        }
                        Divider().padding(.leading, 60)
                        formRow(label: "手机号", icon: "phone.fill") {
                            TextField("11位手机号", text: $phone)
                                .multilineTextAlignment(.trailing)
                                .keyboardType(.phonePad)
                        }
                        Divider().padding(.leading, 60)
                        HStack(spacing: 12) {
                            Image(systemName: "heart.fill")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(AppPalette.coral)
                                .frame(width: 32, height: 32)
                                .background(AppPalette.softCoral)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                            Text("关系")
                                .foregroundColor(AppPalette.textPrimary)
                            Spacer()
                            Picker("", selection: $relation) {
                                ForEach(relations, id: \.self) { Text($0) }
                            }
                            .pickerStyle(.menu)
                            .tint(AppPalette.textPrimary)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
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
                .padding(.top, 16)
            }
            .background(AppPalette.background)
            .navigationTitle("添加成员")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") { Task { await save() } }
                        .disabled(isSaving || !canSave)
                        .overlay { if isSaving { ProgressView().scaleEffect(0.8) } }
                }
            }
        }
    }

    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        phone.count == 11
    }

    private func save() async {
        guard let token = session.accessToken else { return }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            try await viewModel.addMember(
                token: token,
                name: name.trimmingCharacters(in: .whitespacesAndNewlines),
                phone: phone,
                relation: relation
            )
            dismiss()
        } catch {
            self.error = "添加失败：\(error.localizedDescription)"
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
}
