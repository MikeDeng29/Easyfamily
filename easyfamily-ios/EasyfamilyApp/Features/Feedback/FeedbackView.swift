import SwiftUI

struct FeedbackView: View {
    @EnvironmentObject private var session: AuthSession
    @Environment(\.dismiss) private var dismiss
    @State private var vm = FeedbackViewModel()

    var body: some View {
        NavigationStack {
            ZStack {
                AppPalette.background.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Title field
                        VStack(alignment: .leading, spacing: 8) {
                            Text("标题（可选）")
                                .font(.subheadline)
                                .foregroundColor(AppPalette.textSecondary)
                            TextField("简短标题（可选）", text: $vm.title)
                                .padding(12)
                                .background(AppPalette.surface)
                                .cornerRadius(10)
                                .foregroundColor(AppPalette.textPrimary)
                        }

                        // Email field
                        VStack(alignment: .leading, spacing: 8) {
                            Text("联系邮箱（可选）")
                                .font(.subheadline)
                                .foregroundColor(AppPalette.textSecondary)
                            TextField("联系邮箱（用于接收回复）", text: $vm.email)
                                .keyboardType(.emailAddress)
                                .textContentType(.emailAddress)
                                .autocapitalization(.none)
                                .padding(12)
                                .background(AppPalette.surface)
                                .cornerRadius(10)
                                .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.gray.opacity(0.2), lineWidth: 1))
                                .foregroundColor(AppPalette.textPrimary)
                        }

                        // Description field
                        VStack(alignment: .leading, spacing: 8) {
                            Text("问题描述")
                                .font(.subheadline)
                                .foregroundColor(AppPalette.textSecondary)
                            ZStack(alignment: .topLeading) {
                                TextEditor(text: $vm.description)
                                    .frame(minHeight: 160)
                                    .padding(8)
                                    .background(AppPalette.surface)
                                    .cornerRadius(10)
                                    .foregroundColor(AppPalette.textPrimary)
                                    .scrollContentBackground(.hidden)
                                if vm.description.isEmpty {
                                    Text("请描述遇到的问题或建议...")
                                        .foregroundColor(AppPalette.textSecondary.opacity(0.6))
                                        .padding(.top, 16)
                                        .padding(.leading, 12)
                                        .allowsHitTesting(false)
                                }
                            }
                        }

                        // Error message
                        if case .failure(let msg) = vm.submitResult {
                            Text(msg)
                                .font(.footnote)
                                .foregroundColor(AppPalette.error)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }

                        // Submit button
                        Button {
                            Task {
                                guard let token = session.accessToken else { return }
                                await vm.submit(token: token)
                            }
                        } label: {
                            ZStack {
                                if vm.isSubmitting {
                                    ProgressView()
                                        .tint(.white)
                                } else {
                                    Text("提交反馈")
                                        .font(.headline)
                                        .foregroundColor(.white)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(AppPalette.coral)
                            .cornerRadius(12)
                        }
                        .disabled(vm.isSubmitting || vm.description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                        .opacity((vm.isSubmitting || vm.description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) ? 0.6 : 1.0)
                    }
                    .padding(16)
                }
            }
            .navigationTitle("问题反馈")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .foregroundColor(AppPalette.textSecondary)
                    }
                }
            }
            .task {
                if let token = session.accessToken {
                    await vm.loadDefaultEmail(token: token)
                }
            }
            .onChange(of: vm.submitResult) { _, result in
                if case .success = result {
                    // Auto-dismiss after a brief delay to allow user to see the confirmation
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                        dismiss()
                    }
                }
            }
            .overlay {
                if case .success = vm.submitResult {
                    VStack(spacing: 12) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 48))
                            .foregroundColor(AppPalette.coral)
                        Text("已收到你的反馈，感谢！")
                            .font(.headline)
                            .foregroundColor(AppPalette.textPrimary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(AppPalette.background.opacity(0.95))
                    .transition(.opacity)
                }
            }
            .animation(.easeInOut(duration: 0.3), value: vm.submitResult == nil)
        }
    }
}
