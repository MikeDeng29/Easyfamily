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

    /// Matches `<!--BILL_ACTION:{...}-->` appended by the backend's chat system prompt
    /// when it detects a bill-recording intent.
    private static let billActionRegex = try! NSRegularExpression(pattern: "<!--BILL_ACTION:(\\{.*?\\})-->", options: [.dotMatchesLineSeparators])

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
