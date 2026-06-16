import Foundation
import Combine

@MainActor
final class LoginViewModel: ObservableObject {
    @Published var phone: String = "" {
        didSet {
            let filtered = String(phone.filter(\.isNumber).prefix(11))
            if filtered != phone { phone = filtered }
        }
    }
    @Published var smsCode: String = "" {
        didSet {
            let filtered = String(smsCode.filter(\.isNumber).prefix(6))
            if filtered != smsCode { smsCode = filtered }
        }
    }
    @Published var captchaToken: String = ""
    /// Set when the backend flags this request as risky and requires a slide captcha
    /// before it will send another SMS code.
    @Published var captchaRequired: Bool = false
    @Published var smsCooldownSeconds: Int = 0
    @Published var loading: Bool = false
    @Published var info: String = ""
    @Published var infoIsError: Bool = false

    private var cooldownTimer: Timer?

    var isPhoneValid: Bool { phone.count == 11 }
    var isSmsCodeValid: Bool { smsCode.count == 6 }
    var isCaptchaVerified: Bool { !captchaToken.isEmpty }
    var canSendSms: Bool {
        isPhoneValid && smsCooldownSeconds == 0 && !loading && (!captchaRequired || isCaptchaVerified)
    }
    var canLogin: Bool { isPhoneValid && isSmsCodeValid && !loading }

    func onCaptchaSuccess(_ token: String) {
        captchaToken = token
        info = "安全校验通过，正在重新发送验证码…"
        infoIsError = false
        sendSms()
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
                try await APIService.sendSms(phone: phone, captchaToken: captchaToken.isEmpty ? nil : captchaToken)
                info = "验证码已发送，请查收短信"
                infoIsError = false
                startCooldown()
            } catch let error as ApiError where error.code == "CAPTCHA_REQUIRED" {
                captchaRequired = true
                info = "为保障账号安全，请先完成下方验证"
                infoIsError = false
            } catch {
                info = "发送失败：\(error.localizedDescription)"
                infoIsError = true
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
            infoIsError = false
            session.login(userId: result.userId, accessToken: result.accessToken, refreshToken: result.refreshToken)
        } catch {
            info = "登录失败：\(error.localizedDescription)"
            infoIsError = true
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
            infoIsError = false
            session.login(userId: result.userId, accessToken: result.accessToken, refreshToken: result.refreshToken)
        } catch {
            info = "一键登录失败：\(error.localizedDescription)"
            infoIsError = true
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
