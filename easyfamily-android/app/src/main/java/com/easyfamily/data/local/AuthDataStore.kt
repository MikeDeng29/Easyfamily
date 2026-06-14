package com.easyfamily.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
    }

    val accessToken: Flow<String> = dataStore.data.map { prefs ->
        prefs[ACCESS_TOKEN] ?: ""
    }

    suspend fun saveToken(token: String) {
        dataStore.edit { it[ACCESS_TOKEN] = token }
    }

    suspend fun clearToken() {
        dataStore.edit { it.remove(ACCESS_TOKEN) }
    }
}
