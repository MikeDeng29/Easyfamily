package com.easyfamily.data.repository

import com.easyfamily.data.local.AuthDataStore
import com.easyfamily.data.network.ApiService
import com.easyfamily.data.network.LoginData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val authDataStore: AuthDataStore
) {

    suspend fun verifyCaptcha(ticket: String): Result<String> = runCatching {
        val body = mapOf("captchaProvider" to "mock", "ticket" to ticket)
        val response = apiService.verifyCaptcha(body)
        val apiResponse = response.body()
            ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "verifyCaptcha failed")
        apiResponse.data?.captchaToken ?: error("missing captchaToken")
    }

    suspend fun sendSms(phone: String, captchaToken: String): Result<Unit> = runCatching {
        val body = mapOf("phone" to phone, "captchaToken" to captchaToken)
        val response = apiService.sendSms(body)
        val apiResponse = response.body()
            ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "sendSms failed")
    }

    suspend fun login(phone: String, smsCode: String): Result<LoginData> = runCatching {
        val body = mapOf("phone" to phone, "smsCode" to smsCode)
        val response = apiService.login(body)
        val apiResponse = response.body()
            ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "login failed")
        val data = apiResponse.data ?: error("missing login data")
        authDataStore.saveToken(data.accessToken)
        data
    }

    suspend fun logout() {
        authDataStore.clearToken()
    }

    fun accessTokenFlow() = authDataStore.accessToken
}
