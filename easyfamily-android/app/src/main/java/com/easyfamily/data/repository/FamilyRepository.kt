package com.easyfamily.data.repository

import com.easyfamily.data.network.ApiService
import com.easyfamily.data.network.FamilyMemberItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun listFamilyMembers(): Result<List<FamilyMemberItem>> = runCatching {
        val response = apiService.listFamilyMembers()
        val apiResponse = response.body() ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "listFamilyMembers failed")
        apiResponse.data ?: emptyList()
    }

    suspend fun addFamilyMember(
        name: String,
        phone: String,
        relation: String
    ): Result<FamilyMemberItem> = runCatching {
        val body = mapOf("name" to name, "phone" to phone, "relation" to relation)
        val response = apiService.addFamilyMember(body)
        val apiResponse = response.body() ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "addFamilyMember failed")
        apiResponse.data ?: error("no data")
    }

    suspend fun deleteFamilyMember(memberId: String): Result<Unit> = runCatching {
        val response = apiService.deleteFamilyMember(memberId)
        val apiResponse = response.body() ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "deleteFamilyMember failed")
    }
}
