import Foundation

struct FamilyDisplayMember: Identifiable {
    let id: String
    let memberId: String   // empty for owner
    let name: String
    let maskedPhone: String
    let relation: String
    let isOwner: Bool
}

@MainActor
final class FamilyViewModel: ObservableObject {
    @Published var members: [FamilyDisplayMember] = []
    @Published var loading: Bool = false
    @Published var error: String? = nil

    func load(token: String) async {
        loading = true
        error = nil
        async let phonesResult = try? APIService.listMyPhones(token: token)
        async let familyResult = try? APIService.listFamilyMembers(token: token)

        let phones = await phonesResult ?? []
        let family = await familyResult ?? []

        var result: [FamilyDisplayMember] = []
        let ownerPhone = phones.first(where: \.isPrimary)?.phone ?? phones.first?.phone ?? ""
        result.append(FamilyDisplayMember(id: "owner", memberId: "", name: "我", maskedPhone: maskPhone(ownerPhone), relation: "户主", isOwner: true))

        for member in family {
            let name = member.name.isEmpty ? "未命名成员" : member.name
            result.append(FamilyDisplayMember(id: member.memberId, memberId: member.memberId, name: name, maskedPhone: maskPhone(member.phone), relation: member.relation, isOwner: false))
        }

        members = result
        loading = false
    }

    func addMember(token: String, name: String, phone: String, relation: String) async throws {
        let item = try await APIService.addFamilyMember(token: token, name: name, phone: phone, relation: relation)
        let display = FamilyDisplayMember(
            id: item.memberId, memberId: item.memberId,
            name: item.name.isEmpty ? "未命名成员" : item.name,
            maskedPhone: maskPhone(item.phone),
            relation: item.relation,
            isOwner: false
        )
        members.append(display)
    }

    func deleteMember(token: String, memberId: String) async throws {
        try await APIService.deleteFamilyMember(token: token, memberId: memberId)
        members.removeAll { $0.memberId == memberId }
    }

    private func maskPhone(_ phone: String) -> String {
        guard phone.count == 11 else { return phone }
        return "\(phone.prefix(3))****\(phone.suffix(4))"
    }
}
