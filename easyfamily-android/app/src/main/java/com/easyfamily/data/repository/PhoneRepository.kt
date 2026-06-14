package com.easyfamily.data.repository

import com.easyfamily.data.network.ApiService
import com.easyfamily.data.network.PhoneItem
import com.easyfamily.data.network.RealNameVerifyResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun listMyPhones(): Result<List<PhoneItem>> = runCatching {
        val response = apiService.listMyPhones()
        val apiResponse = response.body() ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "listMyPhones failed")
        apiResponse.data ?: emptyList()
    }

    suspend fun bindPhone(phone: String, smsCode: String): Result<Unit> = runCatching {
        val body = mapOf("phone" to phone, "smsCode" to smsCode)
        val response = apiService.bindPhone(body)
        val apiResponse = response.body() ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "bindPhone failed")
    }

    suspend fun unbindPhone(phone: String): Result<Unit> = runCatching {
        val body = mapOf("phone" to phone)
        val response = apiService.unbindPhone(body)
        val apiResponse = response.body() ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "unbindPhone failed")
    }

    suspend fun setPrimaryPhone(phone: String): Result<Unit> = runCatching {
        val body = mapOf("phone" to phone)
        val response = apiService.setPrimaryPhone(body)
        val apiResponse = response.body() ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "setPrimaryPhone failed")
    }

    suspend fun verifyRealName(
        phone: String,
        name: String,
        idCardNo: String
    ): Result<RealNameVerifyResult> = runCatching {
        val body = buildMap<String, String> {
            put("phone", phone)
            put("name", name)
            if (idCardNo.isNotBlank()) put("idCardNo", idCardNo)
        }
        val response = apiService.verifyRealName(body)
        val apiResponse = response.body() ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "verifyRealName failed")
        apiResponse.data ?: error("missing verifyRealName data")
    }
}
