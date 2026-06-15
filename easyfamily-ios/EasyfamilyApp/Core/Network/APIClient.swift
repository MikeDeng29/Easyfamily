import Foundation

/// Thin URLSession-based JSON client matching the backend's `{ code, message, data }` envelope.
final class APIClient {
    static let shared = APIClient()

    private let session: URLSession
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    init(session: URLSession = .shared) {
        self.session = session
    }

    private func makeRequest(path: String, method: String, token: String?, query: [String: String]?) throws -> URLRequest {
        var components = URLComponents(string: Config.apiBaseURL + path)
        if let query, !query.isEmpty {
            components?.queryItems = query.map { URLQueryItem(name: $0.key, value: $0.value) }
        }
        guard let url = components?.url else {
            throw ApiError(message: "invalid URL: \(path)")
        }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let token {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        return request
    }

    /// Request with a JSON-encodable body.
    func request<T: Decodable, B: Encodable>(
        _ path: String,
        method: String = "POST",
        token: String? = nil,
        body: B,
        query: [String: String]? = nil
    ) async throws -> T? {
        var request = try makeRequest(path: path, method: method, token: token, query: query)
        request.httpBody = try encoder.encode(body)
        return try await send(request)
    }

    /// Request without a body (GET / DELETE / empty POST).
    func request<T: Decodable>(
        _ path: String,
        method: String = "GET",
        token: String? = nil,
        query: [String: String]? = nil
    ) async throws -> T? {
        let request = try makeRequest(path: path, method: method, token: token, query: query)
        return try await send(request)
    }

    private func send<T: Decodable>(_ request: URLRequest) async throws -> T? {
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw ApiError(message: "no HTTP response")
        }
        guard (200..<300).contains(http.statusCode) else {
            throw ApiError(message: "HTTP \(http.statusCode)")
        }
        let envelope = try decoder.decode(ApiResponse<T>.self, from: data)
        guard envelope.code == "OK" else {
            throw ApiError(code: envelope.code, message: envelope.message ?? envelope.code)
        }
        return envelope.data
    }
}
