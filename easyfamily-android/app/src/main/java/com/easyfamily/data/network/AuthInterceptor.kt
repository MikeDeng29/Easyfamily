package com.easyfamily.data.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.easyfamily.data.local.AuthDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val dataStore: DataStore<Preferences>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            dataStore.data.map { it[AuthDataStore.ACCESS_TOKEN] ?: "" }.first()
        }
        val request = if (token.isNotBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
