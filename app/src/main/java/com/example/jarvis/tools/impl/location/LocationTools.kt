package com.example.jarvis.tools.impl.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine

/** GET_LOCATION — get the last known location. */
class GetLocationTool : Tool {
    override val id = "GET_LOCATION"
    override val name = "Məkanı Al"
    override val description = "Cihazın son məlum GPS/şəbəkə məkanını alır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    @Suppress("MissingPermission")
    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isEnabled) {
                return@withContext ToolResult.failed(id,
                    "Məkan xidmətləri söndürülüb. Zəhmət olmasa Parametrlərdən aktivləşdirin.")
            }

            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            val loc = providers
                .mapNotNull { provider ->
                    try { lm.getLastKnownLocation(provider) } catch (_: Exception) { null }
                }
                .maxByOrNull { it.time }

            if (loc == null) {
                ToolResult.success(id,
                    "Son məlum məkan tapılmadı. GPS-i açıq yerə çıxın və yenidən cəhd edin.")
            } else {
                val lat = "%.5f".format(loc.latitude)
                val lon = "%.5f".format(loc.longitude)
                val acc = loc.accuracy.toInt()
                ToolResult.success(id,
                    "Məkanınız: $lat°N, $lon°E (dəqiqlik: ±${acc}m).",
                    mapOf("latitude" to loc.latitude, "longitude" to loc.longitude, "accuracy" to acc))
            }
        } catch (e: SecurityException) {
            ToolResult.permissionRequired(id,
                listOf(Manifest.permission.ACCESS_FINE_LOCATION),
                "Məkan alınması üçün Məkan icazəsi tələb olunur.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Məkan alına bilmədi: ${e.message}")
        }
    }
}

/** OPEN_LOCATION_SETTINGS — open location settings. */
class OpenLocationSettingsTool : Tool {
    override val id = "OPEN_LOCATION_SETTINGS"
    override val name = "Məkan Parametrləri"
    override val description = "Sistem məkan parametrləri səhifəsini açır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            ToolResult.success(id, "Məkan parametrləri açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Məkan parametrləri açıla bilmədi: ${e.message}")
        }
    }
}

/** OPEN_MAP — open a map at specific coords or a search query. */
class OpenMapTool : Tool {
    override val id = "OPEN_MAP"
    override val name = "Xəritəni Aç"
    override val description = "Google Maps-i açır. Ünvan və ya koordinatla axtarış edə bilər."
    override val parameters = listOf(
        ToolParameter("query", "string", false, "Ünvan, yer adı və ya koordinatlar", "")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val query = params["query"]?.trim() ?: ""
        return try {
            val uri = if (query.isNotEmpty())
                Uri.parse("geo:0,0?q=${Uri.encode(query)}")
            else
                Uri.parse("geo:0,0")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            val msg = if (query.isNotEmpty()) "'$query' xəritədə açıldı." else "Xəritə açıldı."
            ToolResult.success(id, msg)
        } catch (e: Exception) {
            ToolResult.failed(id, "Xəritə açıla bilmədi: ${e.message}")
        }
    }
}
