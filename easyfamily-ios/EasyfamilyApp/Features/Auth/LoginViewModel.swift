import Foundation
import Combine

@MainActor
final class LoginViewModel: ObservableObject {
    @Published var phone: String = "" {
        didSet { phone = String(phone.filter(\.isNumber).prefix(11)) }
    }
    @Published var smsCode: String = "" {
        didSet { smsCode = String(smsCode.filter(\.isNumber).prefix(6)) }
    }
    @Published var captchaToken: String = ""
    @Published var smsCooldownSeconds: Int = 0
    @Published var loading: Bool = false
    @Published var info: String = ""

    private var cooldownTimer: Timer?

    var isPhoneValid: Bool { phone.count == 11 }
    var isSmsCodeValid: Bool { smsCode.count == 6 }
    var isCaptchaVerified: Bool { !captchaToken.isEmpty }
    var canSendSms: Bool { isCaptchaVerified && isPhoneValid && smsCooldownSeconds == 0 && !loading }
    var canLogin: Bool { isPhoneValid && isSmsCodeValid && !loading }

    func onCaptchaSuccess(_ token: String) {
        captchaToken = token
        info = ""
    }

    func resetCaptcha() {
        captchaToken = ""
        info = ""
    }

    func sendSms() {
        guard canSendSms else { return }
        loading = true
        Task {
            do {
                try await APIService.sendSms(phone: phone, captchaToken: captchaToken)
                info = "验证码已发送（测试环境默认 123456）"
                startCooldown()
            } catch {
                info = "发送失败：\(error.localizedDescription)"
            }
            loading = false
        }
    }

    func login(session: AuthSession) async {
        guard canLogin else { return }
        loading = true
        defer { loading = false }
        do {
            let result = try await APIService.login(phone: phone, smsCode: smsCode)
            info = "登录成功"
            session.login(userId: result.userId, accessToken: result.accessToken)
        } catch {
            info = "登录失败：\(error.localizedDescription)"
        }
    }

    /// Mirrors Android's "一键登录（测试）": runs captcha → SMS send → login with a
    /// fixed test phone/code in sequence.
    func quickLogin(session: AuthSession) async {
        loading = true
        defer { loading = false }
        let testPhone = "13800000000"
        let testCode = "123456"
        do {
            let token = try await APIService.captchaVerify(ticket: "mock")
            try await APIService.sendSms(phone: testPhone, captchaToken: token)
            let result = try await APIService.login(phone: testPhone, smsCode: testCode)
            phone = testPhone
            smsCode = testCode
            captchaToken = token
            info = "登录成功"
            session.login(userId: result.userId, accessToken: result.accessToken)
        } catch {
            info = "一键登录失败：\(error.localizedDescription)"
        }
    }

    private func startCooldown() {
        smsCooldownSeconds = 30
        cooldownTimer?.invalidate()
        cooldownTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] timer in
            Task { @MainActor in
                guard let self else { timer.invalidate(); return }
                if self.smsCooldownSeconds > 0 {
                    self.smsCooldownSeconds -= 1
                } else {
                    timer.invalidate()
                }
            }
        }
    }
}
