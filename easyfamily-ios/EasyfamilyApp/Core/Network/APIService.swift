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

    static func loginWithPassword(phone: String, password: String) async throws -> LoginResponse {
        guard let result: LoginResponse = try await client.request(
            "/api/v1/auth/login/password", method: "POST", body: PasswordLoginRequest(phone: phone, password: password)
        ) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func refreshToken(refreshToken: String) async throws -> RefreshTokenResponse {
        guard let result: RefreshTokenResponse = try await client.request(
            "/api/v1/auth/refresh", method: "POST", body: RefreshTokenRequest(refreshToken: refreshToken)
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

    static func updateEmail(token: String, email: String) async throws -> UserProfile {
        guard let result: UserProfile = try await client.request(
            "/api/v1/user/email", method: "PUT", token: token, body: UpdateEmailRequest(email: email)
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

    static func setPassword(token: String, newPassword: String) async throws {
        let _: EmptyValue? = try await client.request(
            "/api/v1/user/password", method: "PUT", token: token, body: SetPasswordRequest(newPassword: newPassword)
        )
    }

    static func updateCity(token: String, city: String) async throws {
        let _: EmptyValue? = try await client.request(
            "/api/v1/user/city", method: "PATCH", token: token, body: UpdateCityRequest(city: city)
        )
    }

    // MARK: - Weekly Menu

    static func weeklyMenu(token: String) async throws -> WeeklyMenuResponse {
        guard let result: WeeklyMenuResponse = try await client.request(
            "/api/v1/menu/weekly", method: "GET", token: token
        ) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func markDishPreference(token: String, dishName: String) async throws {
        let _: EmptyBody? = try await client.request(
            "/api/v1/menu/preference", method: "POST", token: token,
            body: DishPreferenceRequest(dishName: dishName)
        )
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

    static func addFamilyMember(token: String, name: String, phone: String, relation: String) async throws -> FamilyMemberItem {
        guard let result: FamilyMemberItem = try await client.request(
            "/api/v1/family/members", method: "POST", token: token,
            body: FamilyMemberCreateRequest(name: name, phone: phone, relation: relation)
        ) else { throw ApiError(message: "empty response") }
        return result
    }

    static func deleteFamilyMember(token: String, memberId: String) async throws {
        let _: EmptyValue? = try await client.request("/api/v1/family/members/\(memberId)", method: "DELETE", token: token)
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

    static func createBill(token: String, direction: String = "expense", category: String, amount: Double, note: String?, billedAt: String) async throws -> BillItemDto {
        let body = BillCreateRequest(direction: direction, category: category, amount: amount, note: note, billedAt: billedAt)
        guard let result: BillItemDto = try await client.request("/api/v1/bill", method: "POST", token: token, body: body) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func deleteBill(token: String, id: Int64) async throws {
        let _: EmptyValue? = try await client.request("/api/v1/bill/\(id)", method: "DELETE", token: token)
    }

    static func getMonthlyTrend(token: String, months: Int = 6) async throws -> [MonthlyTrendItemDto] {
        let result: [MonthlyTrendItemDto]? = try await client.request(
            "/api/v1/bill/monthly-trend", method: "GET", token: token, query: ["months": "\(months)"]
        )
        return result ?? []
    }

    static func getSecurityReport(token: String) async throws -> SecurityReportDto {
        guard let result: SecurityReportDto = try await client.request(
            "/api/v1/bill/security-report", method: "GET", token: token
        ) else { throw ApiError(message: "empty response") }
        return result
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

    static func importMaintenanceRecord(token: String, imageData: Data, mimeType: String) async throws -> MaintenanceImportResult {
        let boundary = "Boundary-\(UUID().uuidString)"
        var body = Data()
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"image\"; filename=\"maintenance.jpg\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: \(mimeType)\r\n\r\n".data(using: .utf8)!)
        body.append(imageData)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)

        guard let url = URL(string: Config.apiBaseURL + "/api/v1/vehicles/import-record") else {
            throw ApiError(message: "invalid URL")
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.httpBody = body

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw ApiError(message: "no HTTP response")
        }
        guard (200..<300).contains(http.statusCode) else {
            throw ApiError(message: "HTTP \(http.statusCode)")
        }
        let envelope = try JSONDecoder().decode(ApiResponse<MaintenanceImportResult>.self, from: data)
        guard envelope.code == "OK" else {
            throw ApiError(code: envelope.code, message: envelope.message ?? envelope.code)
        }
        guard let result = envelope.data else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    // MARK: - Feedback

    static func submitFeedback(token: String, title: String?, description: String, email: String? = nil) async throws {
        let _: EmptyValue? = try await client.request(
            "/api/v1/feedback", method: "POST", token: token, body: FeedbackRequest(title: title, description: description, email: email)
        )
    }

    // MARK: - Asset

    static func listAssets(token: String) async throws -> AssetListResponse {
        guard let result: AssetListResponse = try await client.request("/api/v1/asset", method: "GET", token: token) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func createAsset(token: String, req: AssetCreateRequest) async throws -> AssetItem {
        guard let result: AssetItem = try await client.request("/api/v1/asset", method: "POST", token: token, body: req) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func updateAsset(token: String, id: Int, req: AssetCreateRequest) async throws -> AssetItem {
        guard let result: AssetItem = try await client.request("/api/v1/asset/\(id)", method: "PUT", token: token, body: req) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func deleteAsset(token: String, id: Int) async throws {
        let _: EmptyValue? = try await client.request("/api/v1/asset/\(id)", method: "DELETE", token: token)
    }

    // MARK: - Liability

    static func listLiabilities(token: String) async throws -> LiabilityListResponse {
        guard let result: LiabilityListResponse = try await client.request("/api/v1/liability", method: "GET", token: token) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func createLiability(token: String, req: LiabilityCreateRequest) async throws -> LiabilityItem {
        guard let result: LiabilityItem = try await client.request("/api/v1/liability", method: "POST", token: token, body: req) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func updateLiability(token: String, id: Int, req: LiabilityCreateRequest) async throws -> LiabilityItem {
        guard let result: LiabilityItem = try await client.request("/api/v1/liability/\(id)", method: "PUT", token: token, body: req) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func deleteLiability(token: String, id: Int) async throws {
        let _: EmptyValue? = try await client.request("/api/v1/liability/\(id)", method: "DELETE", token: token)
    }

    // MARK: - Finance

    static func getFinanceRole(token: String) async throws -> FinanceRoleResponse {
        guard let result: FinanceRoleResponse = try await client.request(
            "/api/v1/finance/my-role", method: "GET", token: token
        ) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func listFinancePermissions(token: String) async throws -> PermissionListResponse {
        guard let result: PermissionListResponse = try await client.request(
            "/api/v1/finance/permissions", method: "GET", token: token
        ) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func grantFinancePermission(token: String, phone: String) async throws {
        let _: EmptyValue? = try await client.request(
            "/api/v1/finance/permissions", method: "POST", token: token, body: GrantPermissionRequest(phone: phone)
        )
    }

    static func revokeFinancePermission(token: String, phone: String) async throws {
        let _: EmptyValue? = try await client.request(
            "/api/v1/finance/permissions/\(phone)", method: "DELETE", token: token
        )
    }

    static func getFinancialHealthReport(token: String, month: String?) async throws -> FinancialHealthReport {
        var query: [String: String]? = nil
        if let month { query = ["month": month] }
        guard let result: FinancialHealthReport = try await client.request(
            "/api/v1/finance/health-report", method: "GET", token: token, query: query
        ) else {
            throw ApiError(message: "empty response")
        }
        return result
    }

    static func getFamilyBillStats(token: String, month: String?) async throws -> FamilyBillStats {
        var query: [String: String]? = nil
        if let month { query = ["month": month] }
        guard let result: FamilyBillStats = try await client.request(
            "/api/v1/bill/family-stats", method: "GET", token: token, query: query
        ) else {
            throw ApiError(message: "empty response")
        }
        return result
    }
}
