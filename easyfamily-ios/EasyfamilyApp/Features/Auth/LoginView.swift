import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = LoginViewModel()

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [AppPalette.coral, AppPalette.violet, AppPalette.violetDark],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 24) {
                    brandHeader

                    VStack(alignment: .leading, spacing: 16) {
                        Text("手机号登录")
                            .font(.title3.bold())
                            .foregroundColor(AppPalette.textPrimary)

                        phoneField

                        captchaSection

                        smsRow

                        Button {
                            Task { await viewModel.login(session: session) }
                        } label: {
                            buttonLabel(title: "登录", color: AppPalette.coralDark)
                        }
                        .disabled(!viewModel.canLogin)
                        .opacity(viewModel.canLogin ? 1 : 0.5)

                        Button {
                            Task { await viewModel.quickLogin(session: session) }
                        } label: {
                            Text("一键登录（测试）")
                                .font(.subheadline)
                                .foregroundColor(AppPalette.violet)
                        }
                        .disabled(viewModel.loading)

                        if !viewModel.info.isEmpty {
                            Text(viewModel.info)
                                .font(.caption)
                                .foregroundColor(AppPalette.textSecondary)
                        }
                    }
                    .padding(20)
                    .background(AppPalette.surface)
                    .cornerRadius(20)
                    .padding(.horizontal, 20)

                    Text("登录即代表你同意《用户协议》和《隐私政策》")
                        .font(.caption2)
                        .foregroundColor(.white.opacity(0.8))
                        .padding(.bottom, 24)
                }
                .padding(.top, 60)
            }
        }
    }

    private var brandHeader: some View {
        VStack(spacing: 8) {
            Text("🏠")
                .font(.system(size: 40))
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

    private var phoneField: some View {
        TextField("请输入手机号", text: $viewModel.phone)
            .keyboardType(.numberPad)
            .padding(12)
            .background(AppPalette.background)
            .cornerRadius(10)
    }

    @ViewBuilder
    private var captchaSection: some View {
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
        } else {
            SlideCaptchaView { token in
                viewModel.onCaptchaSuccess(token)
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
                    .foregroundColor(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(AppPalette.violet)
                    .cornerRadius(10)
            }
            .disabled(!viewModel.canSendSms)
            .opacity(viewModel.canSendSms ? 1 : 0.5)
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
