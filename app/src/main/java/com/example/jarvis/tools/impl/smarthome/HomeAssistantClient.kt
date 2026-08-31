package com.example.jarvis.tools.impl.smarthome

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Home Assistant REST API client.
 *
 * SETUP:
 *   1. Open Home Assistant → Profile → Security → Long-lived access tokens → Create token
 *   2. In JARVIS Settings → Smart Home: enter your HA Server URL and the token
 *   3. URL format: http://192.168.1.x:8123  OR  http://homeassistant.local:8123
 */
class HomeAssistantClient(
    private val serverUrl: String,
    private val token: String
) {
    companion object {
        private const val TAG = "HomeAssistantClient"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = serverUrl.isNotBlank() && token.isNotBlank()

    /** Test connection to HA — returns true if server responds */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${serverUrl.trimEnd('/')}/api/")
                .header("Authorization", "Bearer $token")
                .get().build()
            val response = httpClient.newCall(request).execute()
            Log.i(TAG, "HA connection test: ${response.code}")
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "HA connection failed: ${e.message}", e)
            false
        }
    }

    /** Call a HA service (e.g. light.turn_on, climate.set_temperature) */
    suspend fun callService(domain: String, service: String, entityId: String, serviceData: Map<String, Any> = emptyMap()): ServiceCallResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext ServiceCallResult(false, "Home Assistant konfiqurasiya edilməyib. JARVIS Tənzimləmələrindən server URL və token daxil edin.")
        }

        try {
            val payload = JSONObject().apply {
                put("entity_id", entityId)
                serviceData.forEach { (k, v) -> put(k, v) }
            }

            val url = "${serverUrl.trimEnd('/')}/api/services/$domain/$service"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                ServiceCallResult(true, "Əmr icra olundu: $domain.$service ($entityId)")
            } else {
                ServiceCallResult(false, "HA xətası: ${response.code} ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Service call failed: ${e.message}", e)
            ServiceCallResult(false, "Şəbəkə xətası: ${e.message}")
        }
    }

    /** Get state of an entity */
    suspend fun getState(entityId: String): EntityState? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val url = "${serverUrl.trimEnd('/')}/api/states/$entityId"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get().build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) return@withContext null

            val json = JSONObject(body)
            EntityState(
                entityId = json.optString("entity_id"),
                state = json.optString("state"),
                attributes = json.optJSONObject("attributes")?.let { attrs ->
                    val map = mutableMapOf<String, String>()
                    attrs.keys().forEach { key -> map[key] = attrs.optString(key) }
                    map
                } ?: emptyMap()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Get state failed: ${e.message}", e)
            null
        }
    }
}

data class ServiceCallResult(val success: Boolean, val message: String)
data class EntityState(val entityId: String, val state: String, val attributes: Map<String, String>)
