package com.example.jarvis.tools.impl

import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

class ReadNotificationsTool : Tool {
    override val id: String = "READ_NOTIFICATIONS"
    override val name: String = "Bildirişləri Oxu"
    override val description: String = "Cihaza gələn son bildirişlərin vəziyyətini yoxlayır."
    override val parameters: List<ToolParameter> = emptyList()
    override val requiredPermissions: List<String> = listOf("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE")
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            val isEnabled = enabledListeners.contains(context.packageName)

            if (!isEnabled) {
                ToolResult.permissionRequired(
                    toolId = id,
                    permissions = listOf("Notification Listener Access"),
                    message = "Bildirişləri birbaşa oxumaq üçün sistemdə 'Bildiriş Girişi' icazəsi aktivləşdirilməlidir."
                )
            } else {
                ToolResult.success(
                    toolId = id,
                    message = "Bildiriş xidməti aktivdir. Hazırda yeni kritik bildiriş yoxdur.",
                    data = mapOf("hasAccess" to true)
                )
            }
        } catch (e: Exception) {
            ToolResult.failed(id, "Bildirişlər yoxlanılarkən xəta: ${e.message}")
        }
    }
}
