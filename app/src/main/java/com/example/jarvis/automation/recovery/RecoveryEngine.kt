package com.example.jarvis.automation.recovery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.jarvis.domain.model.CommandIntent
import com.example.jarvis.domain.model.ToolResult

sealed class RecoveryAction {
    data class FallbackToBrowser(val searchUrl: String, val message: String) : RecoveryAction()
    data class RequestAccessibility(val message: String) : RecoveryAction()
    data class RequestPermission(val permissions: List<String>, val message: String) : RecoveryAction()
    data class PartialReport(val message: String) : RecoveryAction()
    data class None(val reason: String) : RecoveryAction()
}

class RecoveryEngine(
    private val context: Context
) {

    /**
     * Determine best recovery action when an execution or verification encounters an issue.
     */
    fun recoverFromFailure(intent: CommandIntent, failedToolId: String, errorMessage: String): RecoveryAction {
        // Case 1: App not found or uninstalled for media/search command -> Fallback to Web Browser
        if (intent.intentId == "MEDIA_SEARCH_PLAY" || intent.intentId == "WEB_SEARCH") {
            val query = intent.query ?: intent.rawQuery
            val url = if (intent.targetApp == "spotify") {
                "https://open.spotify.com/search/${Uri.encode(query)}"
            } else {
                "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
            }
            return RecoveryAction.FallbackToBrowser(
                searchUrl = url,
                message = "'${intent.targetApp ?: "Tətbiq"}' tapılmadığı üçün \"$query\" axtarışı brauzerdə açılır."
            )
        }

        // Case 2: Accessibility required
        if (errorMessage.contains("ACCESSIBILITY", ignoreCase = true) || errorMessage.contains("Əlçatımlılıq", ignoreCase = true)) {
            return RecoveryAction.RequestAccessibility(
                "Bu əməliyyatın avtomatik icrası üçün JARVIS Əlçatımlılıq (Accessibility) icazəsi tələb olunur."
            )
        }

        // Case 3: Missing Permission
        if (errorMessage.contains("PERMISSION", ignoreCase = true) || errorMessage.contains("icazə", ignoreCase = true)) {
            return RecoveryAction.RequestPermission(
                permissions = intent.requiresPermission,
                message = "Tələb olunan icazə verilməyib."
            )
        }

        // Case 4: Default partial success / explanation
        return RecoveryAction.PartialReport(
            "Əməliyyat başladıldı, lakin yekun icra zamanı xəta baş verdi: $errorMessage"
        )
    }

    /**
     * Executes the recovery action.
     */
    fun executeRecovery(action: RecoveryAction): ToolResult {
        return when (action) {
            is RecoveryAction.FallbackToBrowser -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.searchUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ToolResult.partialSuccess(
                        toolId = "BROWSER_FALLBACK",
                        message = action.message,
                        data = mapOf("url" to action.searchUrl)
                    )
                } catch (e: Exception) {
                    ToolResult.failed("BROWSER_FALLBACK", "Brauzer açıla bilmədi: ${e.message}")
                }
            }
            is RecoveryAction.RequestAccessibility -> {
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ToolResult.accessibilityRequired("ACCESSIBILITY_SETTINGS", action.message)
                } catch (e: Exception) {
                    ToolResult.failed("ACCESSIBILITY_SETTINGS", action.message)
                }
            }
            is RecoveryAction.RequestPermission -> {
                ToolResult.permissionRequired("PERMISSION_RECOVERY", action.permissions, action.message)
            }
            is RecoveryAction.PartialReport -> {
                ToolResult.partialSuccess("RECOVERY_PARTIAL", action.message)
            }
            is RecoveryAction.None -> {
                ToolResult.failed("NO_RECOVERY", action.reason)
            }
        }
    }
}
