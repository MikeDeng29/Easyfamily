package com.easyfamily.data.repository

import com.easyfamily.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor() {

    private val BASE_URL = BuildConfig.API_BASE_URL.trimEnd('/')

    suspend fun streamChat(message: String, accessToken: String): Flow<String> = flow {
        val connection = withContext(Dispatchers.IO) {
            val url = URL("$BASE_URL/api/v1/chat/stream")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            val payload = JSONObject().put("message", message)
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray())
            }
            conn
        }

        val status = connection.responseCode
        if (status !in 200..299) {
            val err = connection.errorStream?.let {
                BufferedReader(InputStreamReader(it)).use { br -> br.readText() }
            } ?: "Connection error"
            emit("error: $err")
            return@flow
        }

        BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                if (line.startsWith("data:")) {
                    val data = line.removePrefix("data:").trim()
                    if (data.isNotBlank()) {
                        emit(data)
                    }
                }
                line = reader.readLine()
            }
        }
    }.flowOn(Dispatchers.IO)
}
