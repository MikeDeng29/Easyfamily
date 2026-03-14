package com.easyfamily.data

import com.easyfamily.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private val BASE_URL = BuildConfig.API_BASE_URL.trimEnd('/')

    suspend fun verifyCaptcha(ticket: String): String = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("captchaProvider", "mock")
            .put("ticket", ticket)
        val json = request("POST", "/api/v1/auth/captcha/verify", payload = payload)
        json.getJSONObject("data").getString("captchaToken")
    }

    suspend fun sendSms(phone: String, captchaToken: String) = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("phone", phone)
            .put("captchaToken", captchaToken)
        request("POST", "/api/v1/auth/sms/send", payload = payload)
    }

    suspend fun login(phone: String, smsCode: String): LoginResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("phone", phone)
            .put("smsCode", smsCode)
        val json = request("POST", "/api/v1/auth/login", payload = payload)
        val data = json.getJSONObject("data")
        LoginResult(
            userId = data.getString("userId"),
            accessToken = data.getString("accessToken"),
            refreshToken = data.getString("refreshToken")
        )
    }

    suspend fun verifyRealName(
        accessToken: String,
        phone: String,
        name: String,
        idCardNo: String
    ): RealNameVerifyResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("phone", phone)
            .put("name", name)
        if (idCardNo.isNotBlank()) {
            payload.put("idCardNo", idCardNo)
        }
        val json = request("POST", "/api/v1/query/real-name", accessToken, payload)
        val data = json.getJSONObject("data")
        RealNameVerifyResult(
            phone = data.getString("phone"),
            name = data.getString("name"),
            idCardMasked = data.getString("idCardMasked"),
            verified = data.getBoolean("verified"),
            source = data.getString("source"),
            queryTimestamp = data.getLong("queryTimestamp")
        )
    }

    suspend fun listMyPhones(accessToken: String): List<PhoneItem> = withContext(Dispatchers.IO) {
        val json = request("GET", "/api/v1/phones/mine", accessToken = accessToken)
        val data = json.optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until data.length()) {
                val item = data.getJSONObject(index)
                add(
                    PhoneItem(
                        phone = item.getString("phone"),
                        isPrimary = item.optBoolean("isPrimary", false),
                        status = item.optString("status", "UNKNOWN")
                    )
                )
            }
        }
    }

    suspend fun bindPhone(accessToken: String, phone: String, smsCode: String) =
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("phone", phone)
                .put("smsCode", smsCode)
            request("POST", "/api/v1/phones/bind", accessToken, payload)
        }

    suspend fun unbindPhone(accessToken: String, phone: String) = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("phone", phone)
        request("POST", "/api/v1/phones/unbind", accessToken, payload)
    }

    suspend fun setPrimaryPhone(accessToken: String, phone: String) = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("phone", phone)
        request("POST", "/api/v1/phones/primary", accessToken, payload)
    }

    suspend fun initSlideCaptcha(): SlideCaptchaInitResult = withContext(Dispatchers.IO) {
        val json = request("POST", "/api/v1/auth/captcha/slide/init", payload = JSONObject())
        val data = json.getJSONObject("data")
        SlideCaptchaInitResult(
            challengeId = data.getString("challengeId"),
            backgroundImageUrl = data.getString("backgroundImageUrl"),
            sliderImageUrl = data.getString("sliderImageUrl"),
            expireAtEpochSeconds = data.getLong("expireAtEpochSeconds")
        )
    }

    suspend fun verifySlideCaptcha(
        challengeId: String,
        offsetX: Int,
        totalTimeMs: Int,
        tracks: List<SlideTrackPoint>
    ): String = withContext(Dispatchers.IO) {
        val tracksArray = JSONArray()
        tracks.forEach { p ->
            tracksArray.put(
                JSONObject()
                    .put("x", p.x)
                    .put("y", p.y)
                    .put("t", p.t)
            )
        }
        val payload = JSONObject()
            .put("challengeId", challengeId)
            .put("offsetX", offsetX)
            .put("totalTimeMs", totalTimeMs)
            .put("tracks", tracksArray)
        val json = request("POST", "/api/v1/auth/captcha/slide/verify", payload = payload)
        json.getJSONObject("data").getString("captchaToken")
    }

    suspend fun listFamilyMembers(accessToken: String): List<FamilyMemberItem> = withContext(Dispatchers.IO) {
        val json = request("GET", "/api/v1/family/members", accessToken = accessToken)
        val data = json.optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until data.length()) {
                val item = data.getJSONObject(index)
                add(
                    FamilyMemberItem(
                        memberId = item.optString("memberId"),
                        name = item.optString("name", "未命名成员"),
                        phone = item.optString("phone"),
                        relation = item.optString("relation", "关心对象")
                    )
                )
            }
        }
    }

    private fun request(
        method: String,
        path: String,
        accessToken: String? = null,
        payload: JSONObject? = null
    ): JSONObject {
        val connection = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5000
            readTimeout = 10000
            setRequestProperty("Content-Type", "application/json")
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            if (payload != null) {
                doOutput = true
            }
        }

        if (payload != null) {
            connection.outputStream.use { os ->
                os.write(payload.toString().toByteArray())
            }
        }

        val status = connection.responseCode
        val body = if (status in 200..299) {
            BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        } else {
            val err = connection.errorStream?.let { BufferedReader(InputStreamReader(it)).use { br -> br.readText() } }
                ?: ""
            throw IllegalStateException("HTTP $status ${extractMessage(err)}")
        }
        val json = JSONObject(body)
        val code = json.optString("code")
        if (code != "OK") {
            throw IllegalStateException(json.optString("message", "request failed"))
        }
        return json
    }

    private fun extractMessage(raw: String): String {
        return try {
            if (raw.isBlank()) {
                ""
            } else {
                JSONObject(raw).optString("message", raw)
            }
        } catch (_: Exception) {
            raw
        }
    }
}

data class LoginResult(
    val userId: String,
    val accessToken: String,
    val refreshToken: String
)

data class RealNameVerifyResult(
    val phone: String,
    val name: String,
    val idCardMasked: String,
    val verified: Boolean,
    val source: String,
    val queryTimestamp: Long
)

data class PhoneItem(
    val phone: String,
    val isPrimary: Boolean,
    val status: String
)

data class SlideCaptchaInitResult(
    val challengeId: String,
    val backgroundImageUrl: String,
    val sliderImageUrl: String,
    val expireAtEpochSeconds: Long
)

data class SlideTrackPoint(
    val x: Int,
    val y: Int,
    val t: Int
)

data class FamilyMemberItem(
    val memberId: String,
    val name: String,
    val phone: String,
    val relation: String
)
