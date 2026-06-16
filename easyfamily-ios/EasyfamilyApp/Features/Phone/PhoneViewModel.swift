import Foundation

@MainActor
final class PhoneViewModel: ObservableObject {
    @Published var phones: [PhoneItem] = []
    @Published var loading: Bool = false
    @Published var info: String = ""
    @Published var infoIsError: Bool = false

    func refresh(token: String) async {
        loading = true
        do {
            phones = try await APIService.listMyPhones(token: token)
        } catch {
            info = "加载失败：\(error.localizedDescription)"
            infoIsError = true
        }
        loading = false
    }

    func bind(token: String, phone: String, smsCode: String) async {
        guard phone.count == 11, !smsCode.isEmpty else { return }
        loading = true
        do {
            try await APIService.bindPhone(token: token, phone: phone, smsCode: smsCode)
            info = "绑定成功"
            infoIsError = false
            await refresh(token: token)
        } catch {
            info = "绑定失败：\(error.localizedDescription)"
            infoIsError = true
        }
        loading = false
    }

    func unbind(token: String, phone: String) async {
        guard phone.count == 11 else { return }
        loading = true
        do {
            try await APIService.unbindPhone(token: token, phone: phone)
            info = "解绑成功"
            infoIsError = false
            await refresh(token: token)
        } catch {
            info = "解绑失败：\(error.localizedDescription)"
            infoIsError = true
        }
        loading = false
    }

    func setPrimary(token: String, phone: String) async {
        guard phone.count == 11 else { return }
        loading = true
        do {
            try await APIService.setPrimaryPhone(token: token, phone: phone)
            info = "已切换主号"
            infoIsError = false
            await refresh(token: token)
        } catch {
            info = "操作失败：\(error.localizedDescription)"
            infoIsError = true
        }
        loading = false
    }
}
