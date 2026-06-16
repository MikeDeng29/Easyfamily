import Foundation

/// Thin URLSession-based JSON client matching the backend's `{ code, message, data }` envelope.
/// Inject `authSession` at app startup so 401 responses are transparently retried after a
/// token refresh — callers never need to handle 401 themselves.
final class APIClient {
    static let shared = APIClient()

    weak var authSession: AuthSession?

    private let urlSession: URLSession
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    init(urlSession: URLSession = .shared) {
        self.urlSession = urlSession
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
        let (data, response) = try await urlSession.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw ApiError(message: "no HTTP response")
        }
        // On 401: refresh the token once and retry transparently.
        if http.statusCode == 401, let auth = authSession {
            guard let newToken = await auth.refreshAccessToken() else {
                throw ApiError(message: "HTTP 401")
            }
            var retry = request
            retry.setValue("Bearer \(newToken)", forHTTPHeaderField: "Authorization")
            return try await execute(retry)
        }
        return try decode(data, statusCode: http.statusCode)
    }

    // Executes a request with no retry logic (used for the post-refresh attempt).
    private func execute<T: Decodable>(_ request: URLRequest) async throws -> T? {
        let (data, response) = try await urlSession.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw ApiError(message: "no HTTP response")
        }
        return try decode(data, statusCode: http.statusCode)
    }

    private func decode<T: Decodable>(_ data: Data, statusCode: Int) throws -> T? {
        guard (200..<300).contains(statusCode) else {
            throw ApiError(message: "HTTP \(statusCode)")
        }
        let envelope = try decoder.decode(ApiResponse<T>.self, from: data)
        guard envelope.code == "OK" else {
            throw ApiError(code: envelope.code, message: envelope.message ?? envelope.code)
        }
        return envelope.data
    }
}
