import Foundation

/// Streams `/api/v1/chat/stream`, yielding each `data:` line's payload as it arrives
/// (mirrors Android's ChatRepository line-based SSE parsing).
enum ChatStreamClient {
    static func stream(message: String, token: String) -> AsyncThrowingStream<String, Error> {
        AsyncThrowingStream { continuation in
            let task = Task {
                do {
                    guard let url = URL(string: Config.apiBaseURL + "/api/v1/chat/stream") else {
                        throw ApiError(message: "invalid URL")
                    }
                    var request = URLRequest(url: url)
                    request.httpMethod = "POST"
                    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                    request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                    request.httpBody = try JSONEncoder().encode(ChatRequest(message: message))
                    request.timeoutInterval = 60

                    let (bytes, response) = try await URLSession.shared.bytes(for: request)
                    if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                        throw ApiError(message: "HTTP \(http.statusCode)")
                    }

                    for try await line in bytes.lines {
                        guard line.hasPrefix("data:") else { continue }
                        let payload = line.dropFirst("data:".count).trimmingCharacters(in: .whitespaces)
                        if !payload.isEmpty {
                            continuation.yield(payload)
                        }
                    }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }
}
