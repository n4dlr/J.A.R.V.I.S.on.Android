package com.example.jarvis.tools.impl.system

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.services.JarvisAccessibilityService
import com.example.jarvis.tools.CapabilityDetector
import com.example.jarvis.tools.Tool

/**
 * SCREEN_CONTROL — adjust screen brightness.
 * Requires WRITE_SETTINGS permission (special app access).
 */
class ScreenControlTool : Tool {
    override val id = "SCREEN_CONTROL"
    override val name = "Ekran Parlaqlığı"
    override val description = "Ekranın parlaqlığını artırır, azaldır və ya müəyyən səviyyəyə qoyur."
    override val parameters = listOf(
        ToolParameter("action", "string", true, "UP | DOWN | SET | AUTO", "UP"),
        ToolParameter("value", "int", false, "0-255 arası parlaqlıq dəyəri (yalnız SET üçün)", "128")
    )
    override val requiredPermissions = listOf(android.Manifest.permission.WRITE_SETTINGS)
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else true
    }

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            return ToolResult.specialAccessRequired(
                id,
                "WRITE_SETTINGS",
                "Ekran parlaqlığını dəyişmək üçün Parametrlər → Xüsusi tətbiq girişi → " +
                        "Sistem parametrlərini dəyişdir → JARVIS aktivləşdir."
            )
        }

        return try {
            val resolver = context.contentResolver
            val action = params["action"]?.uppercase() ?: "UP"

            when (action) {
                "AUTO" -> {
                    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                    ToolResult.success(id, "Ekran parlaqlığı avtomatik rejimə keçirildi.")
                }
                "SET" -> {
                    val value = params["value"]?.toIntOrNull()?.coerceIn(0, 255) ?: 128
                    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, value)
                    ToolResult.success(id, "Ekran parlaqlığı $value (255-dən) olaraq təyin edildi.")
                }
                "DOWN" -> {
                    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    val cur = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128)
                    val newVal = (cur - 40).coerceAtLeast(10)
                    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, newVal)
                    ToolResult.success(id, "Ekran parlaqlığı azaldıldı ($newVal/255).")
                }
                else -> { // UP
                    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    val cur = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128)
                    val newVal = (cur + 40).coerceAtMost(255)
                    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, newVal)
                    ToolResult.success(id, "Ekran parlaqlığı artırıldı ($newVal/255).")
                }
            }
        } catch (e: Exception) {
            ToolResult.failed(id, "Parlaqlıq dəyişdirilə bilmədi: ${e.message}")
        }
    }
}

/** OPEN_HOME — navigate to launcher home screen. */
class OpenHomeTool : Tool {
    override val id = "OPEN_HOME"
    override val name = "Ana Ekrana Qayıt"
    override val description = "Cihazın ana ekranına gedir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val service = JarvisAccessibilityService.get()
        return if (service != null) {
            service.goHome()
            ToolResult.success(id, "Ana ekrana keçildi.")
        } else {
            // Fallback via Intent
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                    addCategory(android.content.Intent.CATEGORY_HOME)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                ToolResult.success(id, "Ana ekrana keçildi.")
            } catch (e: Exception) {
                ToolResult.failed(id, "Ana ekrana keçid uğursuz oldu: ${e.message}")
            }
        }
    }
}

/** OPEN_RECENTS — show recent apps. */
class OpenRecentsTool : Tool {
    override val id = "OPEN_RECENTS"
    override val name = "Son Tətbiqlər"
    override val description = "Son açılmış tətbiqləri göstərir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val service = JarvisAccessibilityService.get()
        return if (service != null && service.openRecents()) {
            ToolResult.success(id, "Son tətbiqlər açıldı.")
        } else {
            ToolResult.specialAccessRequired(
                id, CapabilityDetector.ACCESSIBILITY_SERVICE,
                "Bu əməliyyat JARVIS Əlçatımlılıq Xidmətini tələb edir. Zəhmət olmasa aktivləşdirin."
            )
        }
    }
}

/** OPEN_NOTIFICATIONS — expand notification shade. */
class OpenNotificationsTool : Tool {
    override val id = "OPEN_NOTIFICATIONS"
    override val name = "Bildirişlər Paneli"
    override val description = "Bildirişlər panelini aşağı çəkir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val service = JarvisAccessibilityService.get()
        return if (service != null && service.openNotifications()) {
            ToolResult.success(id, "Bildirişlər paneli açıldı.")
        } else {
            ToolResult.specialAccessRequired(
                id, CapabilityDetector.ACCESSIBILITY_SERVICE,
                "Bu əməliyyat JARVIS Əlçatımlılıq Xidmətini tələb edir."
            )
        }
    }
}

/** OPEN_QUICK_SETTINGS — expand quick settings panel. */
class OpenQuickSettingsTool : Tool {
    override val id = "OPEN_QUICK_SETTINGS"
    override val name = "Sürətli Parametrlər"
    override val description = "Sürətli parametrlər panelini açır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val service = JarvisAccessibilityService.get()
        return if (service != null && service.openQuickSettings()) {
            ToolResult.success(id, "Sürətli parametrlər açıldı.")
        } else {
            ToolResult.specialAccessRequired(
                id, CapabilityDetector.ACCESSIBILITY_SERVICE,
                "Bu əməliyyat JARVIS Əlçatımlılıq Xidmətini tələb edir."
            )
        }
    }
}
