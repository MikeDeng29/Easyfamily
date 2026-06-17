import SwiftUI

struct FinancePermissionView: View {
    @EnvironmentObject private var session: AuthSession
    @State private var viewModel = FinancePermissionViewModel()
    @State private var isGranting = false
    @State private var grantError: String? = nil

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.role == "none" {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if viewModel.isHead {
                headContent
            } else {
                noPermissionView
            }
        }
        .background(AppPalette.background)
        .navigationTitle("财务授权")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            guard let token = session.accessToken else { return }
            await viewModel.loadRole(token: token)
            await viewModel.loadViewers(token: token)
        }
    }

    // MARK: - Head Content

    private var headContent: some View {
        List {
            // Description card
            Section {
                HStack(spacing: 12) {
                    Image(systemName: "person.badge.key.fill")
                        .font(.title2)
                        .foregroundColor(AppPalette.violet)
                    Text("已授权以下成员查看你的家庭财务数据")
                        .font(.subheadline)
                        .foregroundColor(AppPalette.textPrimary)
                }
                .padding(.vertical, 4)
            }

            // Viewers list
            Section {
                if viewModel.viewers.isEmpty {
                    Text("暂未授权任何人查看家庭财务")
                        .font(.subheadline)
                        .foregroundColor(AppPalette.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.vertical, 8)
                } else {
                    ForEach(viewModel.viewers, id: \.self) { phone in
                        HStack(spacing: 12) {
                            Image(systemName: "person.fill")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(AppPalette.violet)
                                .frame(width: 28, height: 28)
                                .background(AppPalette.softViolet)
                                .clipShape(Circle())
                            Text(phone)
                                .font(.subheadline)
                                .foregroundColor(AppPalette.textPrimary)
                        }
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            Button(role: .destructive) {
                                Task {
                                    guard let token = session.accessToken else { return }
                                    try? await viewModel.revoke(token: token, phone: phone)
                                }
                            } label: {
                                Label("撤销", systemImage: "person.badge.minus")
                            }
                        }
                    }
                }
            } header: {
                Text("已授权成员")
            }

            // Add permission section
            Section {
                HStack(spacing: 12) {
                    TextField("输入手机号", text: $viewModel.newPhone)
                        .keyboardType(.phonePad)
                        .font(.subheadline)

                    Button {
                        Task { await grantPermission() }
                    } label: {
                        if isGranting {
                            ProgressView()
                                .frame(width: 44, height: 28)
                        } else {
                            Text("授权")
                                .font(.subheadline.bold())
                                .foregroundColor(.white)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(AppPalette.violet)
                                .clipShape(Capsule())
                        }
                    }
                    .disabled(isGranting || viewModel.newPhone.trimmingCharacters(in: .whitespaces).isEmpty)
                }

                if let err = grantError {
                    Text(err)
                        .font(.caption)
                        .foregroundColor(AppPalette.error)
                }
            } header: {
                Text("添加授权")
            } footer: {
                Text("被授权成员可查看你的家庭财务数据，但无法修改")
                    .font(.caption)
                    .foregroundColor(AppPalette.textSecondary)
            }
        }
        .listStyle(.insetGrouped)
    }

    // MARK: - No Permission View

    private var noPermissionView: some View {
        VStack(spacing: 16) {
            Image(systemName: "lock.shield")
                .font(.system(size: 48))
                .foregroundColor(AppPalette.textSecondary)
            Text("无权限")
                .font(.headline)
                .foregroundColor(AppPalette.textPrimary)
            Text("只有户主才能管理财务授权")
                .font(.subheadline)
                .foregroundColor(AppPalette.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.horizontal, 32)
    }

    // MARK: - Actions

    private func grantPermission() async {
        guard let token = session.accessToken else { return }
        isGranting = true
        grantError = nil
        do {
            try await viewModel.grant(token: token)
        } catch {
            grantError = error.localizedDescription
        }
        isGranting = false
    }
}
