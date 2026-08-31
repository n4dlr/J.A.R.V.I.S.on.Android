package com.example.jarvis.tools.impl.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.jarvis.automation.apps.AppResolver
import com.example.jarvis.automation.media.MediaCommandEngine
import com.example.jarvis.automation.observation.ObservationEngine
import com.example.jarvis.automation.verification.VerificationEngine
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.services.AccessibilityController
import com.example.jarvis.tools.Tool

/**
 * MEDIA_SEARCH_PLAY — Search and play media content in a specified application.
 *
 * This tool handles commands like:
 *   "YouTube-da INNA Caliente mahnısını aç"
 *   "Spotify-da musiqi axtar"
 *   "INNA Caliente-ni YouTube-da oynat"
 */
class MediaSearchPlayTool(
    private val mediaCommandEngine: MediaCommandEngine? = null
) : Tool {
    override val id = "MEDIA_SEARCH_PLAY"
    override val name = "Media Axtarış və Oynatma"
    override val description = "Göstərilən tətbiqdə media axtarır və oynadır. (YouTube, Spotify, və s.)"
    override val parameters = listOf(
        ToolParameter("target_app", "string", true, "Hədəf tətbiq (youtube, spotify, və s.)"),
        ToolParameter("query", "string", true, "Axtarış sorğusu (mahnı adı, ifaçı, və s.)")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean {
        val app = params["target_app"]?.trim()?.lowercase() ?: return false
        val query = params["query"]?.trim() ?: return false
        return app.isNotBlank() && query.isNotBlank()
    }

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val appName = params["target_app"]?.trim()?.lowercase() ?: "youtube"
        val query = params["query"]?.trim() ?: return ToolResult.failed(id, "Axtarış sorğusu qeyd olunmayıb.")

        val engine = mediaCommandEngine ?: run {
            val acc = AccessibilityController(context)
            val obs = ObservationEngine(context, acc)
            val ver = VerificationEngine(context, obs, acc)
            val resolver = AppResolver(context)
            MediaCommandEngine(context, resolver, acc, obs, ver)
        }

        return if (appName == "spotify") {
            engine.searchAndPlaySpotify(query)
        } else {
            engine.searchAndPlayYouTube(query)
        }
    }
}

/**
 * APP_SEARCH — search within a specific application (e.g. browser search).
 *
 * Handles: "Chrome-da X axtar", "Google-da Y haqqında axtar"
 */
class AppSearchTool : Tool {
    override val id = "APP_SEARCH"
    override val name = "Tətbiqdə Axtar"
    override val description = "Göstərilən tətbiqdə (Chrome, Google, və s.) axtarış aparır."
    override val parameters = listOf(
        ToolParameter("query", "string", true, "Axtarış sorğusu"),
        ToolParameter("target_app", "string", false, "Hədəf tətbiq", "chrome")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val query = params["query"]?.trim()
            ?: return ToolResult.failed(id, "Axtarış sorğusu göstərilməyib.")
        val targetApp = params["target_app"]?.trim()?.lowercase() ?: "chrome"

        val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                val chromePackages = listOf("com.android.chrome", "org.mozilla.firefox", "com.microsoft.emmx")
                val browserPkg = chromePackages.firstOrNull { pkg ->
                    context.packageManager.getLaunchIntentForPackage(pkg) != null
                }
                if (browserPkg != null) setPackage(browserPkg)
            }
            context.startActivity(intent)
            ToolResult.success(
                id,
                "\"$query\" üzrə $targetApp-da axtarış açıldı.",
                mapOf("query" to query, "app" to targetApp, "url" to searchUrl)
            )
        } catch (e: Exception) {
            ToolResult.failed(id, "Axtarış açıla bilmədi: ${e.message}")
        }
    }
}
