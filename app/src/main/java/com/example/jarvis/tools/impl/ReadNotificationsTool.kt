package com.example.jarvis.tools.impl

import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.jarvis.services.JarvisNotificationListenerService
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
                    message = "Bildirişləri oxumaq üçün sistem parametrlərində JARVIS üçün 'Bildiriş Girişi' icazəsi aktivləşdirilməlidir."
                )
            } else {
                val notifs = JarvisNotificationListenerService.StateHolder.notifications
                if (notifs.isEmpty()) {
                    ToolResult.success(
                        toolId = id,
                        message = "Hazırda heç bir aktiv bildiriş yoxdur.",
                        data = mapOf("count" to 0)
                    )
                } else {
                    val preview = notifs.take(5).mapIndexed { i, n ->
                        val app = if (n.appLabel.isNotEmpty()) "[${n.appLabel}] " else ""
                        "${i + 1}. $app${n.title}${if (n.text.isNotEmpty()) ": ${n.text}" else ""}"
                    }.joinToString("\n")
                    ToolResult.success(
                        toolId = id,
                        message = "${notifs.size} aktiv bildiriş aşkar edildi:\n$preview",
                        data = mapOf("count" to notifs.size, "notifications" to notifs.map { it.title })
                    )
                }
            }
        } catch (e: Exception) {
            ToolResult.failed(id, "Bildirişlər oxunarkən xəta: ${e.message}")
        }
    }
}
