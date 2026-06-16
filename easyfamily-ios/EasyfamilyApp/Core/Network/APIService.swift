import Foundation

struct EmptyBody: Encodable {}

/// Endpoint-level wrappers mirroring the Android repositories (AuthRepository,
/// CaptchaRepository, PhoneRepository, FamilyRepository, BillViewModel/ApiClient,
/// VehicleViewModel/ApiClient).
enum APIService {
    private static var client: APIClient { .shared }

    // MARK: - Auth / Captcha

    static func slideCaptchaInit() async throws -> SlideCaptchaInitResponse {
        guard let result: SlideCaptchaInitResponse = try await client.request(
            "/api/v1/auth/captcha/slide/init", method: "POST", body: EmptyBody()
        ) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    /// Simple mock captcha verify used by the "quick login (test)" flow.
    static func captchaVerify(ticket: String = "mock") async throws -> String {
        guard let result: CaptchaTokenResponse = try await client.request(
            "/api/v1/auth/captcha/verify", method: "POST", body: CaptchaVerifyRequest(captchaProvider: "mock", ticket: ticket)
        ) else {
            throw ApiError(message: "empty response")
        }
        return result.captchaToken
    }

    static func slideCaptchaVerify(challengeId: String, offsetX: Int, totalTimeMs: Int, tracks: [SlideTrackPoint]) async throws -> String {
        let body = SlideCaptchaVerifyRequest(challengeId: challengeId, offsetX: offsetX, totalTimeMs: totalTimeMs, tracks: tracks)
        guard let result: CaptchaTokenResponse = try await client.request(
            "/api/v1/auth/captcha/slide/verify", method: "POST", body: body
        ) else {
            throw ApiError(message: "empty response")
        }
        return result.captchaToken
    }

    static func sendSms(phone: String, captchaToken: String? = nil) async throws {
        let _: EmptyValue? = try await client.request(
            "/api/v1/auth/sms/send", method: "POST", body: SmsSendRequest(phone: phone, captchaToken: captchaToken)
        )
    }

    static func login(phone: String, smsCode: String) async throws -> LoginResponse {
        guard let result: LoginResponse = try await client.request(
            "/api/v1/auth/login", method: "POST", body: LoginRequest(phone: phone, smsCode: smsCode)
        ) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    // MARK: - User Profile

    static func getUserProfile(token: String) async throws -> UserProfile {
        guard let result: UserProfile = try await client.request("/api/v1/user/profile", method: "GET", token: token) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func updateNickname(token: String, nickname: String) async throws -> UserProfile {
        guard let result: UserProfile = try await client.request(
            "/api/v1/user/profile", method: "PUT", token: token, body: UpdateNicknameRequest(nickname: nickname)
        ) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func updateButler(token: String, request: UpdateButlerRequest) async throws -> UserProfile {
        guard let result: UserProfile = try await client.request(
            "/api/v1/user/butler", method: "PUT", token: token, body: request
        ) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    // MARK: - Phones

    static func listMyPhones(token: String) async throws -> [PhoneItem] {
        let result: [PhoneItem]? = try await client.request("/api/v1/phones/mine", method: "GET", token: token)
        return result ?? []
    }

    static func bindPhone(token: String, phone: String, smsCode: String) async throws {
        let _: EmptyValue? = try await client.request(
            "/api/v1/phones/bind", method: "POST", token: token, body: BindPhoneRequest(phone: phone, smsCode: smsCode)
        )
    }

    static func unbindPhone(token: String, phone: String) async throws {
        let _: EmptyValue? = try await client.request(
            "/api/v1/phones/unbind", method: "POST", token: token, body: PhoneOnlyRequest(phone: phone)
        )
    }

    static func setPrimaryPhone(token: String, phone: String) async throws {
        let _: EmptyValue? = try await client.request(
            "/api/v1/phones/set-primary", method: "POST", token: token, body: PhoneOnlyRequest(phone: phone)
        )
    }

    static func verifyRealName(token: String, phone: String, name: String, idCardNo: String?) async throws -> RealNameVerifyResult {
        let body = RealNameVerifyRequest(phone: phone, name: name, idCardNo: (idCardNo?.isEmpty ?? true) ? nil : idCardNo)
        guard let result: RealNameVerifyResult = try await client.request(
            "/api/v1/query/real-name", method: "POST", token: token, body: body
        ) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    // MARK: - Family

    static func listFamilyMembers(token: String) async throws -> [FamilyMemberItem] {
        let result: [FamilyMemberItem]? = try await client.request("/api/v1/family/members", method: "GET", token: token)
        return result ?? []
    }

    // MARK: - Bill

    static func listBills(token: String, month: String? = nil) async throws -> [BillItemDto] {
        var query: [String: String]? = nil
        if let month { query = ["month": month] }
        let result: [BillItemDto]? = try await client.request("/api/v1/bill", method: "GET", token: token, query: query)
        return result ?? []
    }

    static func getBillStats(token: String, month: String? = nil) async throws -> BillStatsDto {
        var query: [String: String]? = nil
        if let month { query = ["month": month] }
        guard let result: BillStatsDto = try await client.request("/api/v1/bill/stats", method: "GET", token: token, query: query) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func createBill(token: String, category: String, amount: Double, note: String?, billedAt: String) async throws -> BillItemDto {
        let body = BillCreateRequest(category: category, amount: amount, note: note, billedAt: billedAt)
        guard let result: BillItemDto = try await client.request("/api/v1/bill", method: "POST", token: token, body: body) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func deleteBill(token: String, id: Int64) async throws {
        let _: EmptyValue? = try await client.request("/api/v1/bill/\(id)", method: "DELETE", token: token)
    }

    // MARK: - Vehicle

    static func listVehicles(token: String) async throws -> [VehicleItemDto] {
        let result: [VehicleItemDto]? = try await client.request("/api/v1/vehicles", method: "GET", token: token)
        return result ?? []
    }

    static func createVehicle(token: String, plateNumber: String, brand: String, model: String, year: Int?) async throws -> VehicleItemDto {
        let body = VehicleCreateRequest(plateNumber: plateNumber, brand: brand, model: model, year: year)
        guard let result: VehicleItemDto = try await client.request("/api/v1/vehicles", method: "POST", token: token, body: body) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func deleteVehicle(token: String, vehicleId: Int64) async throws {
        let _: EmptyValue? = try await client.request("/api/v1/vehicles/\(vehicleId)", method: "DELETE", token: token)
    }

    static func listRecords(token: String, vehicleId: Int64) async throws -> [MaintenanceRecordDto] {
        let result: [MaintenanceRecordDto]? = try await client.request("/api/v1/vehicles/\(vehicleId)/records", method: "GET", token: token)
        return result ?? []
    }

    static func createRecord(
        token: String,
        vehicleId: Int64,
        serviceDate: String,
        mileageKm: Int?,
        shopName: String?,
        notes: String?,
        items: [MaintenanceItemInput]
    ) async throws -> MaintenanceRecordDto {
        let body = RecordCreateRequest(serviceDate: serviceDate, mileageKm: mileageKm, shopName: shopName, notes: notes, items: items)
        guard let result: MaintenanceRecordDto = try await client.request(
            "/api/v1/vehicles/\(vehicleId)/records", method: "POST", token: token, body: body
        ) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func deleteRecord(token: String, vehicleId: Int64, recordId: Int64) async throws {
        let _: EmptyValue? = try await client.request("/api/v1/vehicles/\(vehicleId)/records/\(recordId)", method: "DELETE", token: token)
    }

    static func getVehicleStats(token: String, vehicleId: Int64) async throws -> VehicleStatsDto {
        guard let result: VehicleStatsDto = try await client.request("/api/v1/vehicles/\(vehicleId)/stats", method: "GET", token: token) else {
            throw ApiError(message: "empty response")
        }
        return result
    }
}
