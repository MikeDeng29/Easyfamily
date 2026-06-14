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

    // --- Bill APIs ---

    suspend fun createBill(
        token: String,
        category: String,
        amount: Double,
        note: String?,
        billedAt: String
    ): BillItemDto = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("category", category)
            .put("amount", amount)
            .put("billedAt", billedAt)
        if (note != null) payload.put("note", note)
        val json = request("POST", "/api/v1/bill", token, payload)
        parseBillItem(json.getJSONObject("data"))
    }

    suspend fun listBills(token: String, month: String? = null): List<BillItemDto> = withContext(Dispatchers.IO) {
        val path = if (month != null) "/api/v1/bill?month=$month" else "/api/v1/bill"
        val json = request("GET", path, accessToken = token)
        val data = json.optJSONArray("data") ?: JSONArray()
        buildList {
            for (i in 0 until data.length()) add(parseBillItem(data.getJSONObject(i)))
        }
    }

    suspend fun updateBill(
        token: String,
        id: Long,
        category: String,
        amount: Double,
        note: String?,
        billedAt: String
    ): BillItemDto = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("category", category)
            .put("amount", amount)
            .put("billedAt", billedAt)
        if (note != null) payload.put("note", note)
        val json = request("PUT", "/api/v1/bill/$id", token, payload)
        parseBillItem(json.getJSONObject("data"))
    }

    suspend fun deleteBill(token: String, id: Long) = withContext(Dispatchers.IO) {
        request("DELETE", "/api/v1/bill/$id", accessToken = token)
    }

    suspend fun getBillStats(token: String, month: String? = null): BillStatsDto = withContext(Dispatchers.IO) {
        val path = if (month != null) "/api/v1/bill/stats?month=$month" else "/api/v1/bill/stats"
        val json = request("GET", path, accessToken = token)
        val data = json.getJSONObject("data")
        val catsArr = data.optJSONArray("byCategory") ?: JSONArray()
        val byCategory = buildList {
            for (i in 0 until catsArr.length()) {
                val c = catsArr.getJSONObject(i)
                add(BillCategoryStatDto(c.getString("category"), c.getDouble("amount"), c.getInt("count")))
            }
        }
        BillStatsDto(data.getDouble("totalAmount"), data.getInt("count"), byCategory)
    }

    private fun parseBillItem(obj: JSONObject) = BillItemDto(
        id = obj.getLong("id"),
        category = obj.getString("category"),
        amount = obj.getDouble("amount"),
        note = obj.optString("note").takeIf { it.isNotEmpty() },
        billedAt = obj.getString("billedAt"),
        createdAt = obj.getLong("createdAt")
    )

    // --- Vehicle APIs ---

    suspend fun listVehicles(accessToken: String): List<VehicleItemDto> = withContext(Dispatchers.IO) {
        val json = request("GET", "/api/v1/vehicles", accessToken = accessToken)
        val data = json.optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until data.length()) {
                val item = data.getJSONObject(index)
                add(
                    VehicleItemDto(
                        id = item.getLong("id"),
                        plateNumber = item.getString("plateNumber"),
                        brand = item.getString("brand"),
                        model = item.getString("model"),
                        year = item.optInt("year", 0).takeIf { it > 0 }
                    )
                )
            }
        }
    }

    suspend fun createVehicle(
        accessToken: String,
        plateNumber: String,
        brand: String,
        model: String,
        year: Int?
    ): VehicleItemDto = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("plateNumber", plateNumber)
            .put("brand", brand)
            .put("model", model)
        if (year != null) payload.put("year", year)
        val json = request("POST", "/api/v1/vehicles", accessToken, payload)
        val data = json.getJSONObject("data")
        VehicleItemDto(
            id = data.getLong("id"),
            plateNumber = data.getString("plateNumber"),
            brand = data.getString("brand"),
            model = data.getString("model"),
            year = data.optInt("year", 0).takeIf { it > 0 }
        )
    }

    suspend fun updateVehicle(
        accessToken: String,
        vehicleId: Long,
        plateNumber: String,
        brand: String,
        model: String,
        year: Int?
    ): VehicleItemDto = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("plateNumber", plateNumber)
            .put("brand", brand)
            .put("model", model)
        if (year != null) payload.put("year", year)
        val json = request("PUT", "/api/v1/vehicles/$vehicleId", accessToken, payload)
        val data = json.getJSONObject("data")
        VehicleItemDto(
            id = data.getLong("id"),
            plateNumber = data.getString("plateNumber"),
            brand = data.getString("brand"),
            model = data.getString("model"),
            year = data.optInt("year", 0).takeIf { it > 0 }
        )
    }

    suspend fun deleteVehicle(accessToken: String, vehicleId: Long) = withContext(Dispatchers.IO) {
        request("DELETE", "/api/v1/vehicles/$vehicleId", accessToken = accessToken)
    }

    suspend fun listRecords(accessToken: String, vehicleId: Long): List<MaintenanceRecordDto> = withContext(Dispatchers.IO) {
        val json = request("GET", "/api/v1/vehicles/$vehicleId/records", accessToken = accessToken)
        val data = json.optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until data.length()) {
                val item = data.getJSONObject(index)
                val itemsArr = item.optJSONArray("items") ?: JSONArray()
                val items = buildList {
                    for (j in 0 until itemsArr.length()) {
                        val i = itemsArr.getJSONObject(j)
                        add(
                            MaintenanceItemDto(
                                id = i.getLong("id"),
                                category = i.getString("category"),
                                itemName = i.getString("itemName"),
                                cost = i.getDouble("cost").toBigDecimal(),
                                isDiy = i.optBoolean("isDiy", false),
                                notes = i.optString("notes", "").takeIf { it.isNotEmpty() }
                            )
                        )
                    }
                }
                add(
                    MaintenanceRecordDto(
                        id = item.getLong("id"),
                        vehicleId = item.getLong("vehicleId"),
                        serviceDate = item.getString("serviceDate"),
                        mileageKm = item.optInt("mileageKm", 0).takeIf { it > 0 },
                        shopName = item.optString("shopName", "").takeIf { it.isNotEmpty() },
                        totalCost = item.getDouble("totalCost").toBigDecimal(),
                        notes = item.optString("notes", "").takeIf { it.isNotEmpty() },
                        items = items
                    )
                )
            }
        }
    }

    suspend fun createRecord(
        accessToken: String,
        vehicleId: Long,
        serviceDate: String,
        mileageKm: Int?,
        shopName: String?,
        notes: String?,
        itemsJson: JSONArray
    ): MaintenanceRecordDto = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("serviceDate", serviceDate)
        if (mileageKm != null) payload.put("mileageKm", mileageKm)
        if (shopName != null) payload.put("shopName", shopName)
        if (notes != null) payload.put("notes", notes)
        payload.put("items", itemsJson)
        val json = request("POST", "/api/v1/vehicles/$vehicleId/records", accessToken, payload)
        val data = json.getJSONObject("data")
        val itemsArr = data.optJSONArray("items") ?: JSONArray()
        val items = buildList {
            for (j in 0 until itemsArr.length()) {
                val i = itemsArr.getJSONObject(j)
                add(
                    MaintenanceItemDto(
                        id = i.getLong("id"),
                        category = i.getString("category"),
                        itemName = i.getString("itemName"),
                        cost = i.getDouble("cost").toBigDecimal(),
                        isDiy = i.optBoolean("isDiy", false),
                        notes = i.optString("notes", "").takeIf { it.isNotEmpty() }
                    )
                )
            }
        }
        MaintenanceRecordDto(
            id = data.getLong("id"),
            vehicleId = data.getLong("vehicleId"),
            serviceDate = data.getString("serviceDate"),
            mileageKm = data.optInt("mileageKm", 0).takeIf { it > 0 },
            shopName = data.optString("shopName", "").takeIf { it.isNotEmpty() },
            totalCost = data.getDouble("totalCost").toBigDecimal(),
            notes = data.optString("notes", "").takeIf { it.isNotEmpty() },
            items = items
        )
    }

    suspend fun deleteRecord(accessToken: String, vehicleId: Long, recordId: Long) = withContext(Dispatchers.IO) {
        request("DELETE", "/api/v1/vehicles/$vehicleId/records/$recordId", accessToken = accessToken)
    }

    suspend fun getVehicleStats(accessToken: String, vehicleId: Long): VehicleStatsDto = withContext(Dispatchers.IO) {
        val json = request("GET", "/api/v1/vehicles/$vehicleId/stats", accessToken = accessToken)
        val data = json.getJSONObject("data")
        val catsArr = data.optJSONArray("byCategory") ?: JSONArray()
        val categories = buildList {
            for (j in 0 until catsArr.length()) {
                val c = catsArr.getJSONObject(j)
                add(
                    CategoryStatDto(
                        category = c.getString("category"),
                        totalCost = c.getDouble("totalCost").toBigDecimal(),
                        itemCount = c.getLong("itemCount"),
                        diyCount = c.getLong("diyCount")
                    )
                )
            }
        }
        VehicleStatsDto(
            totalCost = data.getDouble("totalCost").toBigDecimal(),
            totalRecords = data.getLong("totalRecords"),
            totalItems = data.getLong("totalItems"),
            byCategory = categories
        )
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

data class BillItemDto(
    val id: Long,
    val category: String,
    val amount: Double,
    val note: String?,
    val billedAt: String,
    val createdAt: Long
)

data class BillCategoryStatDto(
    val category: String,
    val amount: Double,
    val count: Int
)

data class BillStatsDto(
    val totalAmount: Double,
    val count: Int,
    val byCategory: List<BillCategoryStatDto>
)

data class VehicleItemDto(
    val id: Long,
    val plateNumber: String,
    val brand: String,
    val model: String,
    val year: Int?
)

data class MaintenanceItemDto(
    val id: Long,
    val category: String,
    val itemName: String,
    val cost: java.math.BigDecimal,
    val isDiy: Boolean,
    val notes: String?
)

data class MaintenanceRecordDto(
    val id: Long,
    val vehicleId: Long,
    val serviceDate: String,
    val mileageKm: Int?,
    val shopName: String?,
    val totalCost: java.math.BigDecimal,
    val notes: String?,
    val items: List<MaintenanceItemDto>
)

data class CategoryStatDto(
    val category: String,
    val totalCost: java.math.BigDecimal,
    val itemCount: Long,
    val diyCount: Long
)

data class VehicleStatsDto(
    val totalCost: java.math.BigDecimal,
    val totalRecords: Long,
    val totalItems: Long,
    val byCategory: List<CategoryStatDto>
)
