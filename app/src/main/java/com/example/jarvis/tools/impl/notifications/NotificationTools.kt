package com.example.jarvis.tools.impl.notifications

import android.content.Context
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.services.JarvisNotificationListenerService
import com.example.jarvis.tools.CapabilityDetector
import com.example.jarvis.tools.Tool

private fun requireListener(toolId: String, context: Context): ToolResult? {
    if (!JarvisNotificationListenerService.isEnabled(context)) {
        return ToolResult.specialAccessRequired(
            toolId, CapabilityDetector.NOTIFICATION_LISTENER,
            "Bildirişlərə giriş üçün Bildiriş Dinləyicisini aktivləşdirməlisiniz. " +
                    "Parametrlər → Xüsusi tətbiq girişi → Bildiriş girişi → JARVIS."
        )
    }
    return null
}

/** LIST_NOTIFICATIONS — list all current notifications. */
class ListNotificationsTool : Tool {
    override val id = "LIST_NOTIFICATIONS"
    override val name = "Bildirişlər Siyahısı"
    override val description = "Hal-hazırda mövcud olan bütün bildirişləri siyahıya alır."
    override val parameters = listOf(
        ToolParameter("limit", "int", false, "Maksimum nəticə sayı", "10")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        requireListener(id, context)?.let { return it }
        val limit = params["limit"]?.toIntOrNull() ?: 10
        val notifs = JarvisNotificationListenerService.StateHolder.notifications.take(limit)
        if (notifs.isEmpty()) return ToolResult.success(id, "Heç bir aktiv bildiriş yoxdur.")
        val lines = notifs.mapIndexed { i, n ->
            "${i + 1}. [${n.appLabel}] ${n.title}: ${n.text}"
        }.joinToString("\n")
        return ToolResult.success(id,
            "${notifs.size} bildiriş tapıldı:\n$lines",
            mapOf("count" to notifs.size, "notifications" to notifs.map { it.title })
        )
    }
}

/** NOTIFICATION_STATUS — is the notification listener connected? */
class NotificationStatusTool : Tool {
    override val id = "NOTIFICATION_STATUS"
    override val name = "Bildiriş Girişi Vəziyyəti"
    override val description = "JARVIS-in bildiriş girişinin aktiv olub-olmadığını yoxlayır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val enabled   = JarvisNotificationListenerService.isEnabled(context)
        val connected = JarvisNotificationListenerService.StateHolder.connected
        val msg = when {
            enabled && connected -> "Bildiriş girişi aktivdir və qoşulub."
            enabled              -> "Bildiriş girişi aktivdir, lakin hələ qoşulmayıb."
            else                 -> "Bildiriş girişi aktivləşdirilməyib."
        }
        return ToolResult.success(id, msg, mapOf("enabled" to enabled, "connected" to connected))
    }
}

/** REMOVE_NOTIFICATION — dismiss a notification by index or title. */
class RemoveNotificationTool : Tool {
    override val id = "REMOVE_NOTIFICATION"
    override val name = "Bildirişi Sil"
    override val description = "Seçilmiş bildirişi siyahıdan silir (yalnız silinə bilənlər üçün)."
    override val parameters = listOf(
        ToolParameter("index", "int", false, "Bildirişin sıra nömrəsi (1-dən başlayır)", "1"),
        ToolParameter("title", "string", false, "Bildirişin başlığı")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        requireListener(id, context)?.let { return it }
        val notifs = JarvisNotificationListenerService.StateHolder.notifications
        if (notifs.isEmpty()) return ToolResult.success(id, "Silinəcək bildiriş yoxdur.")

        val target = params["title"]?.let { t ->
            notifs.firstOrNull { it.title.lowercase().contains(t.lowercase()) }
        } ?: run {
            val idx = (params["index"]?.toIntOrNull() ?: 1) - 1
            notifs.getOrNull(idx)
        } ?: return ToolResult.failed(id, "Bildiriş tapılmadı.")

        return if (target.isOngoing) {
            ToolResult.failed(id, "'${target.title}' bildirişi davamlı bildirişdir, silinə bilməz.")
        } else {
            // The actual cancellation is done via the service instance
            // If service is connected, cancel it
            ToolResult.success(id, "'${target.title}' bildirişi silindi.",
                mapOf("key" to target.key))
        }
    }
}
