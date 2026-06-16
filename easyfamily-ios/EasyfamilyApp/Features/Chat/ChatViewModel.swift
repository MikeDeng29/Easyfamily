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
    @Published var nickname: String?
    @Published var nicknameInput: String = ""

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

    /// Matches `<!--BILL_ACTION:{...}-->` appended by the backend's chat system prompt
    /// when it detects a bill-recording intent.
    private static let billActionRegex = try! NSRegularExpression(pattern: "<!--BILL_ACTION:(\\{.*?\\})-->", options: [.dotMatchesLineSeparators])

    private let profileStore = UserProfileStore()

    init() {
        nickname = profileStore.loadNickname()
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
    }

    var canSubmitNickname: Bool {
        !nicknameInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    /// Refreshes the profile (nickname + butler identity) from the server (source of
    /// truth), falling back to the locally cached values if the request fails.
    func loadProfile(token: String) async {
        do {
            let profile = try await APIService.getUserProfile(token: token)
            nickname = profile.nickname
            if let nickname {
                profileStore.saveNickname(nickname)
            } else {
                profileStore.clearNickname()
            }

            if let name = profile.butlerName { butlerName = name }
            if let avatarId = profile.butlerAvatarId { butlerAvatarId = avatarId }
            if let persona = profile.butlerPersona { butlerPersona = persona }
            profileStore.saveButlerIdentity(name: butlerName, avatarId: butlerAvatarId, persona: butlerPersona)
        } catch {
            // Keep showing the locally cached values (already set in init) when offline.
        }
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

    func sendMessage(token: String) {
        let text = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, !loading else { return }

        messages.append(ChatMessage(role: "user", content: text))
        messages.append(ChatMessage(role: "ai", content: "", isStreaming: true))
        input = ""
        loading = true

        Task {
            do {
                for try await chunk in ChatStreamClient.stream(message: text, token: token) {
                    appendToLastMessage(chunk)
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
                _ = try await APIService.createBill(token: token, category: action.category, amount: action.amount, note: action.note, billedAt: action.date)
                messages.append(ChatMessage(role: "ai", content: "✅ 已记录 \(action.category) ¥\(String(format: "%.2f", action.amount))"))
            } catch {
                messages.append(ChatMessage(role: "ai", content: "记录失败：\(error.localizedDescription)"))
            }
        }
    }

    func dismissBillAction() {
        pendingBillAction = nil
        messages.append(ChatMessage(role: "ai", content: "好的，已取消本次记账"))
    }

    private func appendToLastMessage(_ chunk: String) {
        guard var last = messages.popLast() else { return }
        last.content += chunk
        messages.append(last)
    }

    private func finishStreaming() {
        guard var last = messages.popLast() else { return }
        let content = last.content
        let range = NSRange(content.startIndex..<content.endIndex, in: content)
        if let match = Self.billActionRegex.firstMatch(in: content, range: range),
           let jsonRange = Range(match.range(at: 1), in: content),
           let fullRange = Range(match.range(at: 0), in: content) {
            let json = String(content[jsonRange])
            last.content = content.replacingCharacters(in: fullRange, with: "").trimmingCharacters(in: .whitespacesAndNewlines)
            if let data = json.data(using: .utf8), let action = try? JSONDecoder().decode(BillActionData.self, from: data) {
                pendingBillAction = action
            }
        }
        last.isStreaming = false
        messages.append(last)
    }
}
