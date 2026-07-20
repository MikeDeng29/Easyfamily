import Foundation

struct ChatMessage: Identifiable {
    let id = UUID()
    let role: String // "user" or "ai"
    var content: String
    var isStreaming: Bool = false
}

@MainActor
final class ChatViewModel: ObservableObject {
    @Published var messages: [ChatMessage] = []
    @Published var input: String = ""
    @Published var loading: Bool = false
    @Published var pendingBillAction: BillActionData?
    @Published var pendingFamilyAction: FamilyActionData?
    @Published var familyMembers: [FamilyMemberItem] = []
    @Published var nickname: String?
    @Published var nicknameInput: String = ""
    @Published var isProfileLoaded: Bool = false

    // MARK: - Butler identity

    @Published var butlerName: String = "青鸟管家"
    @Published var butlerAvatarId: Int = 1
    @Published var butlerPersona: String = "warm"
    @Published var butlerSetupDone: Bool = false
    @Published var showButlerSettings: Bool = false

    /// Draft values edited in the onboarding step / settings sheet before saving.
    @Published var butlerNameInput: String = ""
    @Published var butlerAvatarIdInput: Int = 1
    @Published var butlerPersonaInput: String = "warm"

    private static let billActionRegex = try! NSRegularExpression(pattern: "<!--BILL_ACTION:(\\{.*?\\})-->", options: [.dotMatchesLineSeparators])
    private static let familyActionRegex = try! NSRegularExpression(pattern: "<!--FAMILY_ACTION:(\\{.*?\\})-->", options: [.dotMatchesLineSeparators])

    private let profileStore = UserProfileStore()

    init() {
        nickname = profileStore.loadNickname()
        // If nickname is cached locally, profile is considered loaded (no server wait needed)
        isProfileLoaded = profileStore.loadNickname() != nil
        butlerSetupDone = profileStore.isButlerSetupDone()
        if let cachedName = profileStore.loadButlerName() {
            butlerName = cachedName
        }
        if let cachedAvatarId = profileStore.loadButlerAvatarId() {
            butlerAvatarId = cachedAvatarId
        }
        if let cachedPersona = profileStore.loadButlerPersona() {
            butlerPersona = cachedPersona
        }

        NotificationCenter.default.addObserver(
            forName: .butlerIdentityUpdated, object: nil, queue: .main
        ) { [weak self] notification in
            guard let self, let info = notification.userInfo else { return }
            if let name = info["name"] as? String { self.butlerName = name }
            if let avatarId = info["avatarId"] as? Int { self.butlerAvatarId = avatarId }
            if let persona = info["persona"] as? String { self.butlerPersona = persona }
        }
    }

    var canSubmitNickname: Bool {
        !nicknameInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    func loadProfile(token: String) async {
        async let profileResult = APIService.getUserProfile(token: token)
        async let membersResult = APIService.listFamilyMembers(token: token)

        if let profile = try? await profileResult {
            if let serverNickname = profile.nickname {
                nickname = serverNickname
                profileStore.saveNickname(serverNickname)
            }
            if let name = profile.butlerName { butlerName = name }
            if let avatarId = profile.butlerAvatarId { butlerAvatarId = avatarId }
            if let persona = profile.butlerPersona { butlerPersona = persona }
            profileStore.saveButlerIdentity(name: butlerName, avatarId: butlerAvatarId, persona: butlerPersona)
        }
        familyMembers = (try? await membersResult) ?? []
        isProfileLoaded = true
    }

    func saveNickname(token: String) {
        let trimmed = nicknameInput.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        nickname = trimmed
        nicknameInput = ""
        profileStore.saveNickname(trimmed)
        Task {
            do {
                _ = try await APIService.updateNickname(token: token, nickname: trimmed)
            } catch {
                // Local state already updated; will retry sync on next loadProfile.
            }
        }
    }

    func resetNickname() {
        nickname = nil
        nicknameInput = ""
        profileStore.clearNickname()
    }

    /// Pre-fills the draft fields with the current identity. Used both for the
    /// first-conversation onboarding step (inline form) and before opening the
    /// settings sheet for later edits.
    func primeButlerSetupDraft() {
        butlerNameInput = butlerName
        butlerAvatarIdInput = butlerAvatarId
        butlerPersonaInput = butlerPersona
    }

    /// Opens the butler settings sheet, pre-filled with the current identity.
    func beginButlerSetup() {
        primeButlerSetupDraft()
        showButlerSettings = true
    }

    func cancelButlerSetup() {
        showButlerSettings = false
        if !butlerSetupDone {
            // First-time onboarding can't be dismissed without a choice;
            // treat cancel as "keep defaults".
            butlerSetupDone = true
            profileStore.markButlerSetupDone()
        }
    }

    func saveButlerSetup(token: String) {
        let trimmed = butlerNameInput.trimmingCharacters(in: .whitespacesAndNewlines)
        let name = trimmed.isEmpty ? "青鸟管家" : String(trimmed.prefix(10))

        butlerName = name
        butlerAvatarId = butlerAvatarIdInput
        butlerPersona = butlerPersonaInput
        butlerSetupDone = true
        showButlerSettings = false

        profileStore.saveButlerIdentity(name: butlerName, avatarId: butlerAvatarId, persona: butlerPersona)
        profileStore.markButlerSetupDone()

        Task {
            do {
                _ = try await APIService.updateButler(
                    token: token,
                    request: UpdateButlerRequest(butlerName: butlerName, butlerAvatarId: butlerAvatarId, butlerPersona: butlerPersona)
                )
            } catch {
                // Local state already updated; will retry sync on next loadProfile.
            }
        }
    }

    func sendMessage(token: String, session: AuthSession? = nil) {
        let text = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, !loading else { return }

        messages.append(ChatMessage(role: "user", content: text))
        messages.append(ChatMessage(role: "ai", content: "", isStreaming: true))
        input = ""
        loading = true

        Task {
            var activeToken = token
            do {
                for try await chunk in ChatStreamClient.stream(message: text, token: activeToken) {
                    appendToLastMessage(chunk)
                }
            } catch let err as ApiError {
                if err.message.contains("401"), let s = session,
                   let newToken = await s.refreshAccessToken() {
                    activeToken = newToken
                    if messages.last?.role == "ai" { messages.removeLast() }
                    messages.append(ChatMessage(role: "ai", content: "", isStreaming: true))
                    do {
                        for try await chunk in ChatStreamClient.stream(message: text, token: activeToken) {
                            appendToLastMessage(chunk)
                        }
                    } catch {
                        appendToLastMessage("\n[出错了：\(error.localizedDescription)]")
                    }
                } else if err.message.contains("401") {
                    appendToLastMessage("\n[登录已过期，请重新登录]")
                } else {
                    appendToLastMessage("\n[出错了：\(err.localizedDescription)]")
                }
            } catch {
                appendToLastMessage("\n[出错了：\(error.localizedDescription)]")
            }
            finishStreaming()
            loading = false
        }
    }

    func confirmBillAction(token: String) {
        guard let action = pendingBillAction else { return }
        pendingBillAction = nil
        Task {
            do {
                let direction = action.direction ?? "expense"
                _ = try await APIService.createBill(token: token, direction: direction, category: action.category, amount: action.amount, note: action.note, billedAt: action.date)
                let label = direction == "income" ? "收入" : "支出"
                messages.append(ChatMessage(role: "ai", content: "已记录 \(label) \(action.category) ¥\(String(format: "%.2f", action.amount))"))
            } catch {
                messages.append(ChatMessage(role: "ai", content: "记录失败：\(error.localizedDescription)"))
            }
        }
    }

    func dismissBillAction() {
        pendingBillAction = nil
        messages.append(ChatMessage(role: "ai", content: "好的，已取消本次记账"))
    }

    func confirmFamilyAction(token: String) {
        guard let action = pendingFamilyAction else { return }
        pendingFamilyAction = nil
        Task {
            do {
                if action.action == "add", let phone = action.phone {
                    let member = try await APIService.addFamilyMember(token: token, name: action.name, phone: phone, relation: action.relation)
                    familyMembers.append(member)
                    messages.append(ChatMessage(role: "ai", content: "已添加家庭成员：\(action.name)（\(action.relation)）"))
                } else if action.action == "delete" {
                    guard let member = familyMembers.first(where: { $0.name == action.name }) else {
                        messages.append(ChatMessage(role: "ai", content: "未找到成员「\(action.name)」，无法删除"))
                        return
                    }
                    try await APIService.deleteFamilyMember(token: token, memberId: member.memberId)
                    familyMembers.removeAll { $0.memberId == member.memberId }
                    messages.append(ChatMessage(role: "ai", content: "已移除家庭成员：\(action.name)（\(action.relation)）"))
                }
            } catch {
                messages.append(ChatMessage(role: "ai", content: "操作失败：\(error.localizedDescription)"))
            }
        }
    }

    func dismissFamilyAction() {
        pendingFamilyAction = nil
        messages.append(ChatMessage(role: "ai", content: "好的，已取消操作"))
    }

    private func appendToLastMessage(_ chunk: String) {
        guard var last = messages.popLast() else { return }
        last.content += chunk
        messages.append(last)
    }

    private func finishStreaming() {
        guard var last = messages.popLast() else { return }
        var content = last.content
        let range = NSRange(content.startIndex..<content.endIndex, in: content)

        if let match = Self.billActionRegex.firstMatch(in: content, range: range),
           let jsonRange = Range(match.range(at: 1), in: content),
           let fullRange = Range(match.range(at: 0), in: content) {
            let json = String(content[jsonRange])
            content = content.replacingCharacters(in: fullRange, with: "").trimmingCharacters(in: .whitespacesAndNewlines)
            if let data = json.data(using: .utf8), let action = try? JSONDecoder().decode(BillActionData.self, from: data) {
                pendingBillAction = action
            }
        }

        let range2 = NSRange(content.startIndex..<content.endIndex, in: content)
        if let match = Self.familyActionRegex.firstMatch(in: content, range: range2),
           let jsonRange = Range(match.range(at: 1), in: content),
           let fullRange = Range(match.range(at: 0), in: content) {
            let json = String(content[jsonRange])
            content = content.replacingCharacters(in: fullRange, with: "").trimmingCharacters(in: .whitespacesAndNewlines)
            if let data = json.data(using: .utf8), let action = try? JSONDecoder().decode(FamilyActionData.self, from: data) {
                pendingFamilyAction = action
            }
        }

        last.content = content
        last.isStreaming = false
        messages.append(last)
    }
}

extension Notification.Name {
    static let butlerIdentityUpdated = Notification.Name("com.easyfamily.butlerIdentityUpdated")
}
