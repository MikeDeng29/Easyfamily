import Foundation

@MainActor
final class QueryViewModel: ObservableObject {
    @Published var phones: [PhoneItem] = []
    @Published var loading: Bool = false
    @Published var info: String = ""
    @Published var queryResult: RealNameVerifyResult?

    func loadPhones(token: String) async {
        do {
            phones = try await APIService.listMyPhones(token: token)
        } catch {
            // non-fatal: manual phone entry still works
        }
    }

    var defaultPhone: String {
        phones.first(where: \.isPrimary)?.phone ?? phones.first?.phone ?? ""
    }

    func verifyRealName(token: String, phone: String, name: String, idCardNo: String) async {
        loading = true
        info = ""
        queryResult = nil
        do {
            queryResult = try await APIService.verifyRealName(token: token, phone: phone, name: name, idCardNo: idCardNo)
        } catch {
            info = "校验失败：\(error.localizedDescription)"
        }
        loading = false
    }
}
