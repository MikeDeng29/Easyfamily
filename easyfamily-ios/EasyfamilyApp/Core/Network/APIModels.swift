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

struct RefreshTokenRequest: Encodable {
    let refreshToken: String
}

struct RefreshTokenResponse: Decodable {
    let accessToken: String
    let refreshToken: String?
}

// MARK: - User Profile

struct UserProfile: Decodable {
    let userId: String
    let phone: String?
    let nickname: String?
    let butlerName: String?
    let butlerAvatarId: Int?
    let butlerPersona: String?
    let email: String?
}

struct UpdateNicknameRequest: Encodable {
    let nickname: String
}

struct UpdateButlerRequest: Encodable {
    let butlerName: String?
    let butlerAvatarId: Int?
    let butlerPersona: String?
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
    let direction: String   // "income" | "expense"
    let category: String
    let amount: Double
    let note: String?
    let billedAt: String
    let createdAt: Int64?
}

struct BillCategoryStatDto: Decodable, Identifiable {
    let direction: String
    let category: String
    let amount: Double
    let count: Int

    var id: String { "\(direction)_\(category)" }
}

struct BillStatsDto: Decodable {
    let totalIncome: Double
    let totalExpense: Double
    let netSavings: Double
    let savingsRate: Double?
    let count: Int
    let byCategory: [BillCategoryStatDto]
}

struct BillCreateRequest: Encodable {
    let direction: String
    let category: String
    let amount: Double
    let note: String?
    let billedAt: String
}

struct MonthlyTrendItemDto: Decodable, Identifiable {
    let month: String
    let totalIncome: Double
    let totalExpense: Double
    let netSavings: Double
    var id: String { month }
}

struct SecurityReportDto: Decodable {
    let hasEnoughData: Bool
    let period: String
    let avgMonthlyIncome: Double
    let avgMonthlyExpense: Double
    let avgMonthlySavings: Double
    let savingsRate: Double?
    let savingsRateStatus: Int   // 0=红 1=黄 2=绿
    let emergencyFundMonths: Double
    let emergencyFundStatus: Int
    let healthScore: Int
    let healthLevel: String
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

// MARK: - Feedback

struct FeedbackRequest: Encodable {
    let title: String?
    let description: String
    let email: String?
}

// MARK: - Chat

struct ChatRequest: Encodable {
    let message: String
}

struct BillActionData: Decodable {
    let direction: String?   // optional，旧 AI 回复可能没有，默认 "expense"
    let category: String
    let amount: Double
    let note: String?
    let date: String
}

// MARK: - Asset

struct AssetItem: Codable, Identifiable {
    let id: Int
    let name: String
    let assetType: String
    let value: Decimal
    let note: String?
    let createdAt: String
}

struct AssetListResponse: Codable {
    let items: [AssetItem]
    let totalValue: Decimal
}

struct AssetCreateRequest: Encodable {
    let name: String
    let assetType: String
    let value: Decimal
    let note: String?
}

// MARK: - Liability

struct LiabilityItem: Codable, Identifiable {
    let id: Int
    let name: String
    let liabilityType: String
    let balance: Decimal
    let monthlyPayment: Decimal?
    let interestRate: Decimal?
    let note: String?
    let createdAt: String
}

struct LiabilityListResponse: Codable {
    let items: [LiabilityItem]
    let totalBalance: Decimal
    let totalMonthlyPayment: Decimal
}

struct LiabilityCreateRequest: Encodable {
    let name: String
    let liabilityType: String
    let balance: Decimal
    let monthlyPayment: Decimal?
    let interestRate: Decimal?
    let note: String?
}

// MARK: - Finance Permissions

struct FinanceRoleResponse: Codable {
    let role: String        // "head" | "viewer" | "none"
    let headUserId: String?
    let headName: String?
}

struct PermissionListResponse: Codable {
    let viewers: [String]   // 脱敏手机号
}

struct GrantPermissionRequest: Encodable {
    let phone: String
}

// MARK: - Family Finance

struct FinancialHealthReport: Codable {
    let monthlyIncome: Decimal
    let monthlyExpense: Decimal
    let netSavings: Decimal
    let savingsRate: Decimal?
    let totalAssets: Decimal
    let liquidAssets: Decimal
    let assetBreakdown: [AssetItem]
    let totalLiabilities: Decimal
    let totalMonthlyPayment: Decimal
    let debtToIncomeRatio: Double
    let liabilityBreakdown: [LiabilityItem]
    let netWorth: Decimal
    let emergencyFundMonths: Double
    let healthScore: Int
    let healthLevel: String
    let suggestions: [String]
}

struct FamilyBillStats: Codable {
    let members: [MemberStats]
    let totalIncome: Decimal
    let totalExpense: Decimal
    let netSavings: Decimal
    let savingsRate: Decimal?
}

struct MemberStats: Codable {
    let memberId: String
    let memberName: String
    let relation: String
    let income: Decimal
    let expense: Decimal
    let netSavings: Decimal
}
