import Foundation

// MARK: - Envelope

struct ApiResponse<T: Decodable>: Decodable {
    let code: String
    let message: String?
    let data: T?
}

/// Used for endpoints whose `data` is null/absent.
struct EmptyValue: Decodable {
    init() {}
    init(from decoder: Decoder) throws {}
}

struct ApiError: Error, LocalizedError {
    let code: String?
    let message: String
    var errorDescription: String? { message }

    init(code: String? = nil, message: String) {
        self.code = code
        self.message = message
    }
}

// MARK: - Auth

struct SlideCaptchaInitResponse: Decodable {
    let challengeId: String
    let backgroundImageUrl: String
    let sliderImageUrl: String?
    let expireAtEpochSeconds: Int64?
}

struct SlideTrackPoint: Codable {
    let x: Int
    let y: Int
    let t: Int
}

struct SlideCaptchaVerifyRequest: Encodable {
    let challengeId: String
    let offsetX: Int
    let totalTimeMs: Int
    let tracks: [SlideTrackPoint]
}

struct CaptchaTokenResponse: Decodable {
    let captchaToken: String
}

struct CaptchaVerifyRequest: Encodable {
    let captchaProvider: String
    let ticket: String
}

struct SmsSendRequest: Encodable {
    let phone: String
    let captchaToken: String?
}

struct LoginRequest: Encodable {
    let phone: String
    let smsCode: String
}

struct LoginResponse: Decodable {
    let userId: String
    let accessToken: String
    let refreshToken: String?
}

// MARK: - Phones

struct PhoneItem: Decodable, Identifiable {
    let phone: String
    let status: String
    let isPrimary: Bool

    var id: String { phone }
}

struct BindPhoneRequest: Encodable {
    let phone: String
    let smsCode: String
}

struct PhoneOnlyRequest: Encodable {
    let phone: String
}

// MARK: - Query (real name)

struct RealNameVerifyRequest: Encodable {
    let phone: String
    let name: String
    let idCardNo: String?
}

struct RealNameVerifyResult: Decodable {
    let phone: String
    let name: String
    let idCardMasked: String?
    let verified: Bool
    let source: String?
    let queryTimestamp: Int64?
}

// MARK: - Family

struct FamilyMemberItem: Decodable, Identifiable {
    let name: String
    let phone: String

    var id: String { phone }
}

// MARK: - Bill

struct BillItemDto: Decodable, Identifiable {
    let id: Int64
    let category: String
    let amount: Double
    let note: String?
    let billedAt: String
    let createdAt: Int64?
}

struct BillCategoryStatDto: Decodable, Identifiable {
    let category: String
    let amount: Double
    let count: Int

    var id: String { category }
}

struct BillStatsDto: Decodable {
    let totalAmount: Double
    let count: Int
    let byCategory: [BillCategoryStatDto]
}

struct BillCreateRequest: Encodable {
    let category: String
    let amount: Double
    let note: String?
    let billedAt: String
}

// MARK: - Vehicle

struct VehicleItemDto: Decodable, Identifiable {
    let id: Int64
    let plateNumber: String
    let brand: String
    let model: String
    let year: Int?
}

struct VehicleCreateRequest: Encodable {
    let plateNumber: String
    let brand: String
    let model: String
    let year: Int?
}

struct MaintenanceItemDto: Decodable, Identifiable {
    let id: Int64
    let category: String
    let itemName: String
    let cost: Double
    let isDiy: Bool
    let notes: String?
}

struct MaintenanceRecordDto: Decodable, Identifiable {
    let id: Int64
    let vehicleId: Int64
    let serviceDate: String
    let mileageKm: Int?
    let shopName: String?
    let totalCost: Double
    let notes: String?
    let items: [MaintenanceItemDto]
}

struct MaintenanceItemInput: Encodable {
    let category: String
    let itemName: String
    let cost: Double
    let isDiy: Bool
}

struct RecordCreateRequest: Encodable {
    let serviceDate: String
    let mileageKm: Int?
    let shopName: String?
    let notes: String?
    let items: [MaintenanceItemInput]
}

struct CategoryStatDto: Decodable, Identifiable {
    let category: String
    let totalCost: Double
    let itemCount: Int64
    let diyCount: Int64

    var id: String { category }
}

struct VehicleStatsDto: Decodable {
    let totalCost: Double
    let totalRecords: Int64
    let totalItems: Int64
    let byCategory: [CategoryStatDto]
}

// MARK: - Chat

struct ChatRequest: Encodable {
    let message: String
}

struct BillActionData: Decodable {
    let category: String
    let amount: Double
    let note: String?
    let date: String
}
