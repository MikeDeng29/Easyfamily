package com.easyfamily.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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

    @GET("/api/v1/monitor/snapshots")
    suspend fun getMonitorSnapshots(): Response<ApiResponse<List<MonitorSnapshot>>>

    @POST("/api/v1/monitor/scan")
    suspend fun triggerMonitorScan(): Response<ApiResponse<Unit>>

    @POST("/api/v1/chat/stream")
    suspend fun chatStream(@Body body: Map<String, String>): Response<ApiResponse<Unit>>
}
