package com.easyfamily.data.repository

import com.easyfamily.data.network.ApiService
import com.easyfamily.data.network.SlideCaptchaInitResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptchaRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun initSlideCaptcha(): Result<SlideCaptchaInitResult> = runCatching {
        val response = apiService.initSlideCaptcha(emptyMap())
        val apiResponse = response.body() ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "initSlideCaptcha failed")
        apiResponse.data ?: error("missing initSlideCaptcha data")
    }

    suspend fun verifySlideCaptcha(
        challengeId: String,
        offsetX: Int,
        totalTimeMs: Int,
        tracks: List<Map<String, Int>>
    ): Result<String> = runCatching {
        val body: Map<String, Any> = mapOf(
            "challengeId" to challengeId,
            "offsetX" to offsetX,
            "totalTimeMs" to totalTimeMs,
            "tracks" to tracks
        )
        val response = apiService.verifySlideCaptcha(body)
        val apiResponse = response.body() ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "verifySlideCaptcha failed")
        apiResponse.data?.captchaToken ?: error("missing captchaToken")
    }
}
