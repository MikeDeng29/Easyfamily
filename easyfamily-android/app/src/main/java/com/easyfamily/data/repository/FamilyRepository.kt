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
}
