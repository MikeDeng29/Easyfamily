package com.easyfamily.data.repository

import com.easyfamily.data.network.ApiService
import com.easyfamily.data.network.MonitorSnapshot
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitorRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getMonitorSnapshots(): Result<List<MonitorSnapshot>> = runCatching {
        val response = apiService.getMonitorSnapshots()
        val apiResponse = response.body() ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "getMonitorSnapshots failed")
        apiResponse.data ?: emptyList()
    }

    suspend fun triggerMonitorScan(): Result<Unit> = runCatching {
        val response = apiService.triggerMonitorScan()
        val apiResponse = response.body() ?: error("HTTP ${response.code()}")
        if (apiResponse.code != "OK") error(apiResponse.message ?: "triggerMonitorScan failed")
    }
}
