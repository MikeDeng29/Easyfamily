package com.easyfamily.data.network

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val code: String,
    val message: String?,
    val data: T?
)

data class CaptchaVerifyData(
    val captchaToken: String
)

data class LoginData(
    val userId: String,
    val accessToken: String,
    val refreshToken: String
)

data class PhoneItem(
    val phone: String,
    @SerializedName("isPrimary") val isPrimary: Boolean,
    val status: String
)

data class FamilyMemberItem(
    val memberId: String,
    val name: String,
    val phone: String,
    val relation: String
)

data class MonitorSnapshot(
    val snapshotId: String,
    val createdAt: Long,
    val summary: String?
)

data class RealNameVerifyResult(
    val phone: String,
    val name: String,
    val idCardMasked: String,
    val verified: Boolean,
    val source: String,
    val queryTimestamp: Long
)

data class SlideCaptchaInitResult(
    val challengeId: String,
    val backgroundImageUrl: String,
    val sliderImageUrl: String,
    val expireAtEpochSeconds: Long
)

// User profile
data class UserProfile(
    val userId: String,
    val phone: String?,
    val nickname: String?,
    val butlerName: String?,
    val butlerAvatarId: Int?,
    val butlerPersona: String?,
    val email: String?
)

// Asset
data class AssetItem(
    val id: Int,
    val name: String,
    val assetType: String,
    val value: Double,
    val note: String?,
    val createdAt: String
)

data class AssetListResponse(
    val items: List<AssetItem>,
    val totalValue: Double
)

// Liability
data class LiabilityItem(
    val id: Int,
    val name: String,
    val liabilityType: String,
    val balance: Double,
    val monthlyPayment: Double?,
    val interestRate: Double?,
    val note: String?,
    val createdAt: String
)

data class LiabilityListResponse(
    val items: List<LiabilityItem>,
    val totalBalance: Double,
    val totalMonthlyPayment: Double
)

// Finance role & permissions
data class FinanceRoleResponse(
    val role: String,       // "head" | "viewer" | "none"
    val headUserId: String?,
    val headName: String?
)

data class PermissionListResponse(
    val viewers: List<String>
)

// Financial health report
data class FinancialHealthReport(
    val monthlyIncome: Double,
    val monthlyExpense: Double,
    val netSavings: Double,
    val savingsRate: Double?,
    val totalAssets: Double,
    val liquidAssets: Double,
    val totalLiabilities: Double,
    val totalMonthlyPayment: Double,
    val debtToIncomeRatio: Double,
    val netWorth: Double,
    val emergencyFundMonths: Double,
    val healthScore: Int,
    val healthLevel: String,
    val suggestions: List<String>
)

// Family bill stats
data class MemberStats(
    val memberId: String,
    val memberName: String,
    val relation: String,
    val income: Double,
    val expense: Double,
    val netSavings: Double
)

data class FamilyBillStats(
    val members: List<MemberStats>,
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val savingsRate: Double?
)
