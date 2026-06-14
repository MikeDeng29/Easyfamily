import Foundation

struct FamilyDisplayMember: Identifiable {
    let id: String
    let name: String
    let maskedPhone: String
    let relation: String
    let isOwner: Bool
}

@MainActor
final class FamilyViewModel: ObservableObject {
    @Published var members: [FamilyDisplayMember] = []
    @Published var loading: Bool = false
    @Published var info: String = ""

    func load(token: String) async {
        loading = true
        info = ""
        async let phonesResult = try? APIService.listMyPhones(token: token)
        async let familyResult = try? APIService.listFamilyMembers(token: token)

        let phones = await phonesResult ?? []
        let family = await familyResult ?? []

        var result: [FamilyDisplayMember] = []
        let ownerPhone = phones.first(where: \.isPrimary)?.phone ?? phones.first?.phone ?? ""
        result.append(FamilyDisplayMember(id: "owner", name: "我", maskedPhone: maskPhone(ownerPhone), relation: "户主", isOwner: true))

        for member in family {
            let name = member.name.isEmpty ? "未命名成员" : member.name
            result.append(FamilyDisplayMember(id: member.phone, name: name, maskedPhone: maskPhone(member.phone), relation: "关心对象", isOwner: false))
        }

        members = result
        if phones.isEmpty && family.isEmpty {
            info = "暂无可展示成员"
        }
        loading = false
    }

    private func maskPhone(_ phone: String) -> String {
        guard phone.count == 11 else { return phone }
        return "\(phone.prefix(3))****\(phone.suffix(4))"
    }
}
