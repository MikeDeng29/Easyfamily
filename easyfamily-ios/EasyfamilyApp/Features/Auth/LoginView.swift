import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = LoginViewModel()
    @FocusState private var phoneFieldFocused: Bool

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [AppPalette.coral, AppPalette.violet],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 36) {
                    brandHeader

                    VStack(alignment: .leading, spacing: 16) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("手机号登录")
                                .font(.title3.bold())
                                .foregroundColor(AppPalette.textPrimary)
                            Text("未注册的手机号将自动创建账号")
                                .font(.caption)
                                .foregroundColor(AppPalette.textSecondary)
                        }

                        phoneField

                        captchaSection
                            .animation(.easeInOut(duration: 0.25), value: viewModel.captchaRequired)

                        smsRow

                        VStack(spacing: 8) {
                            Button {
                                Task { await viewModel.login(session: session) }
                            } label: {
                                buttonLabel(title: "登录", color: AppPalette.coralDark)
                            }
                            .disabled(!viewModel.canLogin)
                            .opacity(viewModel.canLogin ? 1 : 0.5)

                            if !viewModel.canLogin && !viewModel.loading {
                                Text("请先完成手机号与验证码填写")
                                    .font(.caption2)
                                    .foregroundColor(AppPalette.textSecondary)
                            }
                        }

                        agreementText

                        if !viewModel.info.isEmpty {
                            Text(viewModel.info)
                                .font(.caption)
                                .foregroundColor(AppPalette.textSecondary)
                        }

                        quickLoginButton
                    }
                    .padding(20)
                    .background(AppPalette.surface)
                    .cornerRadius(20)
                    .padding(.horizontal, 20)
                }
                .padding(.top, 60)
                .padding(.bottom, 24)
            }
        }
    }

    private var brandHeader: some View {
        VStack(spacing: 8) {
            Image(systemName: "house.fill")
                .font(.system(size: 32, weight: .semibold))
                .foregroundStyle(
                    LinearGradient(colors: [AppPalette.coral, AppPalette.violet], startPoint: .topLeading, endPoint: .bottomTrailing)
                )
                .frame(width: 72, height: 72)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 18))
            Text("easyfamily")
                .font(.title.bold())
                .foregroundColor(.white)
            Text("AI 智能家庭守护")
                .font(.caption)
                .foregroundColor(.white.opacity(0.85))
        }
    }

    private var agreementText: some View {
        (
            Text("登录即代表你同意")
                .foregroundColor(AppPalette.textSecondary)
            + Text("《用户协议》")
                .foregroundColor(AppPalette.violet)
            + Text("和")
                .foregroundColor(AppPalette.textSecondary)
            + Text("《隐私政策》")
                .foregroundColor(AppPalette.violet)
        )
        .font(.caption2)
    }

    private var quickLoginButton: some View {
        Button {
            Task { await viewModel.quickLogin(session: session) }
        } label: {
            Text("一键登录（测试环境专用）")
                .font(.subheadline)
                .foregroundColor(AppPalette.violet)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .strokeBorder(AppPalette.violet.opacity(0.4), style: StrokeStyle(lineWidth: 1, dash: [4, 4]))
                )
        }
        .disabled(viewModel.loading)
    }

    private var phoneField: some View {
        TextField("请输入手机号", text: $viewModel.phone)
            .keyboardType(.numberPad)
            .focused($phoneFieldFocused)
            .padding(12)
            .background(AppPalette.background)
            .cornerRadius(10)
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .strokeBorder(phoneFieldFocused ? AppPalette.coral : .clear, lineWidth: 1.5)
            )
            .onAppear { phoneFieldFocused = true }
    }

    @ViewBuilder
    private var captchaSection: some View {
        if viewModel.captchaRequired {
            if viewModel.isCaptchaVerified {
                HStack {
                    Text("✓ 安全校验已通过")
                        .foregroundColor(AppPalette.success)
                        .font(.subheadline)
                    Spacer()
                    Button("重置") { viewModel.resetCaptcha() }
                        .font(.caption)
                        .foregroundColor(AppPalette.coral)
                }
                .padding(12)
                .background(AppPalette.softCoral)
                .cornerRadius(10)
                .transition(.opacity.combined(with: .move(edge: .top)))
            } else {
                SlideCaptchaView { token in
                    withAnimation(.easeInOut(duration: 0.25)) {
                        viewModel.onCaptchaSuccess(token)
                    }
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
    }

    private var smsRow: some View {
        HStack(spacing: 8) {
            TextField("验证码", text: $viewModel.smsCode)
                .keyboardType(.numberPad)
                .padding(12)
                .background(AppPalette.background)
                .cornerRadius(10)

            Button {
                viewModel.sendSms()
            } label: {
                Text(viewModel.smsCooldownSeconds > 0 ? "\(viewModel.smsCooldownSeconds)s" : "获取验证码")
                    .font(.subheadline)
                    .foregroundColor(viewModel.smsCooldownSeconds > 0 ? AppPalette.textSecondary : .white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(viewModel.smsCooldownSeconds > 0 ? AppPalette.background : AppPalette.violet)
                    .cornerRadius(10)
            }
            .disabled(!viewModel.canSendSms)
            .opacity(viewModel.canSendSms || viewModel.smsCooldownSeconds > 0 ? 1 : 0.5)
        }
    }

    private func buttonLabel(title: String, color: Color) -> some View {
        Group {
            if viewModel.loading {
                ProgressView().tint(.white)
            } else {
                Text(title)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(color)
        .foregroundColor(.white)
        .cornerRadius(10)
    }
}
