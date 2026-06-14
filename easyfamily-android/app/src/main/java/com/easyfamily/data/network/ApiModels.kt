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
