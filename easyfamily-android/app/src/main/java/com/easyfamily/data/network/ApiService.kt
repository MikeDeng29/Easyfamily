package com.easyfamily.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("/api/v1/auth/captcha/verify")
    suspend fun verifyCaptcha(@Body body: Map<String, String>): Response<ApiResponse<CaptchaVerifyData>>

    @POST("/api/v1/auth/sms/send")
    suspend fun sendSms(@Body body: Map<String, String>): Response<ApiResponse<Unit>>

    @POST("/api/v1/auth/login")
    suspend fun login(@Body body: Map<String, String>): Response<ApiResponse<LoginData>>

    @POST("/api/v1/auth/captcha/slide/init")
    suspend fun initSlideCaptcha(@Body body: Map<String, String>): Response<ApiResponse<SlideCaptchaInitResult>>

    @POST("/api/v1/auth/captcha/slide/verify")
    suspend fun verifySlideCaptcha(@Body body: Map<String, Any>): Response<ApiResponse<CaptchaVerifyData>>

    @GET("/api/v1/phones/mine")
    suspend fun listMyPhones(): Response<ApiResponse<List<PhoneItem>>>

    @POST("/api/v1/phones/bind")
    suspend fun bindPhone(@Body body: Map<String, String>): Response<ApiResponse<Unit>>

    @POST("/api/v1/phones/unbind")
    suspend fun unbindPhone(@Body body: Map<String, String>): Response<ApiResponse<Unit>>

    @POST("/api/v1/phones/set-primary")
    suspend fun setPrimaryPhone(@Body body: Map<String, String>): Response<ApiResponse<Unit>>

    @POST("/api/v1/query/real-name")
    suspend fun verifyRealName(@Body body: Map<String, String>): Response<ApiResponse<RealNameVerifyResult>>

    @GET("/api/v1/family/members")
    suspend fun listFamilyMembers(): Response<ApiResponse<List<FamilyMemberItem>>>

    @POST("/api/v1/family/members")
    suspend fun addFamilyMember(@Body body: Map<String, String>): Response<ApiResponse<FamilyMemberItem>>

    @DELETE("/api/v1/family/members/{memberId}")
    suspend fun deleteFamilyMember(@Path("memberId") memberId: String): Response<ApiResponse<Unit>>

    @GET("/api/v1/monitor/snapshots")
    suspend fun getMonitorSnapshots(): Response<ApiResponse<List<MonitorSnapshot>>>

    @POST("/api/v1/monitor/scan")
    suspend fun triggerMonitorScan(): Response<ApiResponse<Unit>>

    @POST("/api/v1/chat/stream")
    suspend fun chatStream(@Body body: Map<String, String>): Response<ApiResponse<Unit>>

    // User profile
    @GET("/api/v1/user/profile")
    suspend fun getUserProfile(): Response<ApiResponse<UserProfile>>

    @PUT("/api/v1/user/profile")
    suspend fun updateNickname(@Body body: Map<String, String>): Response<ApiResponse<UserProfile>>

    @PUT("/api/v1/user/email")
    suspend fun updateEmail(@Body body: Map<String, String>): Response<ApiResponse<UserProfile>>

    @PUT("/api/v1/user/butler")
    suspend fun updateButler(@Body body: Map<String, Any?>): Response<ApiResponse<UserProfile>>

    // Feedback
    @POST("/api/v1/feedback")
    suspend fun submitFeedback(@Body body: Map<String, String?>): Response<ApiResponse<Unit>>

    // Asset
    @GET("/api/v1/asset")
    suspend fun listAssets(): Response<ApiResponse<AssetListResponse>>

    @POST("/api/v1/asset")
    suspend fun createAsset(@Body body: Map<String, Any?>): Response<ApiResponse<AssetItem>>

    @PUT("/api/v1/asset/{id}")
    suspend fun updateAsset(
        @Path("id") id: Int,
        @Body body: Map<String, Any?>
    ): Response<ApiResponse<AssetItem>>

    @DELETE("/api/v1/asset/{id}")
    suspend fun deleteAsset(@Path("id") id: Int): Response<ApiResponse<Unit>>

    // Liability
    @GET("/api/v1/liability")
    suspend fun listLiabilities(): Response<ApiResponse<LiabilityListResponse>>

    @POST("/api/v1/liability")
    suspend fun createLiability(@Body body: Map<String, Any?>): Response<ApiResponse<LiabilityItem>>

    @PUT("/api/v1/liability/{id}")
    suspend fun updateLiability(
        @Path("id") id: Int,
        @Body body: Map<String, Any?>
    ): Response<ApiResponse<LiabilityItem>>

    @DELETE("/api/v1/liability/{id}")
    suspend fun deleteLiability(@Path("id") id: Int): Response<ApiResponse<Unit>>

    // Finance
    @GET("/api/v1/finance/my-role")
    suspend fun getFinanceRole(): Response<ApiResponse<FinanceRoleResponse>>

    @GET("/api/v1/finance/permissions")
    suspend fun listFinancePermissions(): Response<ApiResponse<PermissionListResponse>>

    @POST("/api/v1/finance/permissions")
    suspend fun grantFinancePermission(@Body body: Map<String, String>): Response<ApiResponse<Unit>>

    @DELETE("/api/v1/finance/permissions/{phone}")
    suspend fun revokeFinancePermission(@Path("phone") phone: String): Response<ApiResponse<Unit>>

    @GET("/api/v1/finance/health-report")
    suspend fun getHealthReport(@Query("month") month: String): Response<ApiResponse<FinancialHealthReport>>

    @GET("/api/v1/bill/family-stats")
    suspend fun getFamilyBillStats(@Query("month") month: String): Response<ApiResponse<FamilyBillStats>>
}
