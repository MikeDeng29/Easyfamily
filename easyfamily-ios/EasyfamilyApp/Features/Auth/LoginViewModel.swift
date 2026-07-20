import Foundation
import Combine

enum LoginMode: String, CaseIterable {
    case sms = "验证码"
    case password = "密码"
}

@MainActor
final class LoginViewModel: ObservableObject {
    @Published var loginMode: LoginMode = .sms
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
    @Published var password: String = ""
    @Published var captchaToken: String = ""
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
    var canLogin: Bool {
        switch loginMode {
        case .sms: return isPhoneValid && isSmsCodeValid && !loading
        case .password: return isPhoneValid && password.count >= 6 && !loading
        }
    }

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
        switch loginMode {
        case .sms: await loginWithSms(session: session)
        case .password: await loginWithPassword(session: session)
        }
    }

    private func loginWithSms(session: AuthSession) async {
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

    private func loginWithPassword(session: AuthSession) async {
        guard canLogin else { return }
        loading = true
        defer { loading = false }
        do {
            let result = try await APIService.loginWithPassword(phone: phone, password: password)
            info = "登录成功"
            infoIsError = false
            session.login(userId: result.userId, accessToken: result.accessToken, refreshToken: result.refreshToken)
        } catch let error as ApiError where error.code == "PASSWORD_NOT_SET" {
            info = "该账号尚未设置密码，请使用验证码登录"
            infoIsError = true
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
