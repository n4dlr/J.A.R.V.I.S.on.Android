package com.example.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

class OpenSettingsTool : Tool {
    override val id: String = "OPEN_SETTINGS"
    override val name: String = "Tənzimləmələri Aç"
    override val description: String = "Sistem və ya spesifik parametr səhifəsini (Wi-Fi, Bluetooth, Səs, Ekran, Batareya və s.) açır."
    override val parameters: List<ToolParameter> = listOf(
        ToolParameter("target", "String", false, "wifi, bluetooth, display, sound, battery, app, main", "main")
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val target = params["target"]?.lowercase() ?: "main"
            val action = when {
                target.contains("wifi") || target.contains("vayfay") -> Settings.ACTION_WIFI_SETTINGS
                target.contains("bluetooth") || target.contains("blutuz") -> Settings.ACTION_BLUETOOTH_SETTINGS
                target.contains("display") || target.contains("ekran") -> Settings.ACTION_DISPLAY_SETTINGS
                target.contains("sound") || target.contains("ses") -> Settings.ACTION_SOUND_SETTINGS
                target.contains("battery") || target.contains("batareya") -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                target.contains("app") || target.contains("tetbiq") -> Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }

            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            ToolResult.success(id, "Tənzimləmələr bölməsi ($target) açıldı.", mapOf("action" to action))
        } catch (e: Exception) {
            ToolResult.failed(id, "Tənzimləmələr açıla bilmədi: ${e.message}")
        }
    }
}
