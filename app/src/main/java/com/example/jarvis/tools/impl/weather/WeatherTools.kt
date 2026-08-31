package com.example.jarvis.tools.impl.weather

import android.content.Context
import android.util.Log
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GetWeatherTool : Tool {

    companion object {
        private const val TAG = "GetWeatherTool"
    }

    override val id: String = "GET_WEATHER"
    override val name: String = "Hava Haqqında / Hava Durumu"
    override val description: String = "Hava haqqında məlumat alır (temperatur, külək, yağıntı, proqnoz)"
    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "city",
            type = "string",
            isRequired = false,
            description = "Şəhər adı (məsələn: 'Baku', 'Ganja', 'Sumqayit')",
            defaultValue = "Baku"
        )
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val cityParam = params["city"]?.trim()?.ifBlank { "Baku" } ?: "Baku"
        val normalizedCity = when (cityParam.lowercase()) {
            "baki", "baku", "bakı" -> "Baku"
            "gence", "gəncə", "ganja" -> "Ganja"
            "sumqayit", "sumqayıt" -> "Sumqayit"
            "seki", "şəki" -> "Shaki"
            "lenkeran", "lənkəran" -> "Lankaran"
            "mingecevir", "mingəçevir" -> "Mingachevir"
            "naxcivan", "naxçıvan" -> "Nakhchivan"
            "istanbul" -> "Istanbul"
            "ankara" -> "Ankara"
            "moskva", "moscow" -> "Moscow"
            "london" -> "London"
            else -> cityParam
        }

        try {
            // Step 1: Geocoding via Open-Meteo
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(normalizedCity, "UTF-8")}&count=1&language=az&format=json"
            val geoResponse = fetchHttp(geoUrl)
            val geoJson = JSONObject(geoResponse)
            val results = geoJson.optJSONArray("results")

            if (results == null || results.length() == 0) {
                return@withContext ToolResult.failed(id, "'$cityParam' şəhəri üçün koordinatlar tapılmadı.")
            }

            val locationObj = results.getJSONObject(0)
            val lat = locationObj.getDouble("latitude")
            val lon = locationObj.getDouble("longitude")
            val foundName = locationObj.optString("name", normalizedCity)
            val country = locationObj.optString("country", "Azərbaycan")

            // Step 2: Fetch Current Weather
            val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
            val weatherResponse = fetchHttp(weatherUrl)
            val weatherJson = JSONObject(weatherResponse)
            val current = weatherJson.getJSONObject("current")

            val temp = current.getDouble("temperature_2m")
            val feelsLike = current.getDouble("apparent_temperature")
            val humidity = current.getInt("relative_humidity_2m")
            val windSpeed = current.getDouble("wind_speed_10m")
            val weatherCode = current.getInt("weather_code")
            val precipitation = current.optDouble("precipitation", 0.0)

            val conditionDesc = getWeatherConditionAz(weatherCode)

            val outputMessage = buildString {
                append("$foundName ($country) üçün hava durumu:\n")
                append("🌡️ Temperatur: ${temp}°C (Hiss edilən: ${feelsLike}°C)\n")
                append("🌤️ Vəziyyət: $conditionDesc\n")
                append("💧 Rütubət: $humidity%\n")
                append("💨 Külək: ${windSpeed} km/s\n")
                if (precipitation > 0) {
                    append("🌧️ Yağıntı: ${precipitation} mm")
                }
            }

            return@withContext ToolResult.success(
                toolId = id,
                message = outputMessage,
                data = mapOf(
                    "city" to foundName,
                    "temperature" to temp.toString(),
                    "condition" to conditionDesc
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get weather: ${e.message}", e)
            return@withContext ToolResult.failed(id, "Hava məlumatı alına bilmədi: ${e.message}")
        }
    }

    private fun fetchHttp(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        conn.requestMethod = "GET"
        conn.connect()
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun getWeatherConditionAz(code: Int): String = when (code) {
        0 -> "Aydın və açıq səma ☀️"
        1, 2, 3 -> "Əsasən aydın və ya az buludlu 🌤️"
        45, 48 -> "Dumanlı 🌫️"
        51, 53, 55 -> "Çiskinli yağış 🌦️"
        61, 63, 65 -> "Yağışlı 🌧️"
        71, 73, 75 -> "Qarlı ❄️"
        77 -> "Dənəvər qar 🌨️"
        80, 81, 82 -> "Leysan yağış 🌧️⚡"
        85, 86 -> "Qar leysanı 🌨️"
        95, 96, 99 -> "Şimşəkli tufan və ildırım ⛈️"
        else -> "Mülayim hava"
    }
}
