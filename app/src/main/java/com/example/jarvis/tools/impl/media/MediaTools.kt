package com.example.jarvis.tools.impl.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.services.JarvisAccessibilityService
import com.example.jarvis.tools.Tool
import kotlinx.coroutines.delay

/**
 * MEDIA_SEARCH_PLAY — Search and play media content in a specified application.
 *
 * This tool handles commands like:
 *   "YouTube-da INNA Caliente mahnısını aç"
 *   "Spotify-da musiqi axtar"
 *   "INNA Caliente-ni YouTube-da oynat"
 *
 * Execution strategy (in order of preference):
 * 1. Deep-link URI (works without Accessibility, most reliable)
 * 2. App launch + Accessibility search automation (requires service enabled)
 * 3. Browser fallback search (always available)
 *
 * Honesty policy: This tool reports exactly what it was able to verify.
 * It never claims "playback started" unless it can confirm it.
 */
class MediaSearchPlayTool : Tool {
    override val id = "MEDIA_SEARCH_PLAY"
    override val name = "Media Axtarış və Oynatma"
    override val description = "Göstərilən tətbiqdə media axtarır və oynadır. (YouTube, Spotify, və s.)"
    override val parameters = listOf(
        ToolParameter("target_app", "string", true, "Hədəf tətbiq (youtube, spotify, və s.)"),
        ToolParameter("query", "string", true, "Axtarış sorğusu (mahnı adı, ifaçı, və s.)")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    // Deep-link URI templates for known media apps
    private val deepLinkTemplates = mapOf(
        "youtube" to { query: String -> "https://www.youtube.com/results?search_query=${Uri.encode(query)}" },
        "spotify" to { query: String -> "spotify:search:${Uri.encode(query)}" },
        "youtubemusic" to { query: String -> "https://music.youtube.com/search?q=${Uri.encode(query)}" }
    )

    // Known package names for media apps
    private val appPackages = mapOf(
        "youtube" to "com.google.android.youtube",
        "spotify" to "com.spotify.music",
        "youtubemusic" to "com.google.android.apps.youtube.music",
        "soundcloud" to "com.soundcloud.android",
        "tidal" to "com.aspiro.tidal",
        "deezer" to "deezer.android.app",
        "apple music" to "com.apple.android.music",
        "amazon music" to "com.amazon.mp3"
    )

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean {
        val app = params["target_app"]?.trim()?.lowercase() ?: return false
        val query = params["query"]?.trim() ?: return false
        return app.isNotBlank() && query.isNotBlank()
    }

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val appName = params["target_app"]?.trim()?.lowercase()
            ?: return ToolResult.failed(id, "Hədəf tətbiq göstərilməyib.")
        val query = params["query"]?.trim()
            ?: return ToolResult.failed(id, "Axtarış sorğusu göstərilməyib.")

        if (query.isBlank()) return ToolResult.failed(id, "Axtarış sorğusu boş ola bilməz.")

        // Normalize app name (handles "yutub", "youtubeda", etc. — already extracted by AppNameExtractor)
        val normalizedApp = normalizeAppName(appName)

        // Step 1: Try deep-link URI (preferred — works without Accessibility)
        val deepLinkResult = tryDeepLink(context, normalizedApp, query)
        if (deepLinkResult.isSuccess) {
            return deepLinkResult
        }

        // Step 2: Try launching the app and using Accessibility for search automation
        val accessibilityResult = tryLaunchAndSearch(context, normalizedApp, query)
        if (accessibilityResult.isSuccess) {
            return accessibilityResult
        }

        // Step 3: Browser fallback — always works, honest about what happened
        return tryBrowserFallback(context, normalizedApp, query)
    }

    private fun normalizeAppName(raw: String): String {
        return when {
            raw.contains("youtube") || raw == "yutub" || raw == "yt" -> "youtube"
            raw.contains("spotify") -> "spotify"
            raw.contains("soundcloud") -> "soundcloud"
            else -> raw
        }
    }

    private suspend fun tryDeepLink(context: Context, appName: String, query: String): ToolResult {
        val pkg = appPackages[appName]
        val deepLinkFn = deepLinkTemplates[appName]

        if (pkg == null || deepLinkFn == null) {
            return ToolResult.failed(id, "Deep-link şablonu tapılmadı.")
        }

        // Check if app is installed
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(pkg)
        if (launchIntent == null) {
            return ToolResult.failed(id, "'$appName' quraşdırılmayıb.")
        }

        return try {
            val uri = deepLinkFn(query)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                // Prefer the specific app if it can handle the URI
                setPackage(pkg)
            }
            context.startActivity(intent)
            ToolResult.success(
                id,
                "'$appName' tətbiqində \"$query\" axtarışı açıldı.",
                mapOf(
                    "app" to appName,
                    "query" to query,
                    "uri" to uri,
                    "method" to "deep_link"
                )
            )
        } catch (e: Exception) {
            // Deep-link failed, app might not support it
            ToolResult.failed(id, "Deep-link uğursuz oldu: ${e.message}")
        }
    }

    private suspend fun tryLaunchAndSearch(context: Context, appName: String, query: String): ToolResult {
        if (!JarvisAccessibilityService.isEnabled(context)) {
            return ToolResult.failed(id, "Əlçatımlılıq xidməti aktiv deyil.")
        }

        val pkg = appPackages[appName]
            ?: return ToolResult.failed(id, "Tanınmayan tətbiq: $appName")

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(pkg)
            ?: return ToolResult.failed(id, "'$appName' quraşdırılmayıb.")

        return try {
            // Launch the app
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(launchIntent)

            // Wait for the app to come to foreground (up to 3 seconds)
            var waited = 0
            while (waited < 3000) {
                delay(300)
                waited += 300
                val service = JarvisAccessibilityService.get()
                if (service != null) break
            }

            val service = JarvisAccessibilityService.get()
                ?: return ToolResult.success(
                    id,
                    "'$appName' açıldı. Axtarış avtomatik başladıla bilmədi (Əlçatımlılıq xidməti hazır deyil). \"$query\" üçün əl ilə axtarış edin.",
                    mapOf("app" to appName, "query" to query, "method" to "app_launched_only")
                )

            // Try to find and click the search button/field
            val searchTexts = listOf("Axtar", "Search", "Axtarış", "ara")
            var searchClicked = false
            for (text in searchTexts) {
                if (service.clickByText(text)) {
                    searchClicked = true
                    break
                }
            }

            if (!searchClicked) {
                // Try by view ID for YouTube
                val searchViewIds = listOf(
                    "com.google.android.youtube:id/menu_item_1",
                    "com.google.android.youtube:id/search_edit_text",
                    "com.spotify.music:id/search_tab"
                )
                for (viewId in searchViewIds) {
                    if (service.clickById(viewId)) {
                        searchClicked = true
                        break
                    }
                }
            }

            if (searchClicked) {
                delay(500)
                // Type the search query
                service.typeText(query)
                delay(300)
                ToolResult.success(
                    id,
                    "'$appName' açıldı və \"$query\" axtarışı başladıldı.",
                    mapOf("app" to appName, "query" to query, "method" to "accessibility")
                )
            } else {
                ToolResult.success(
                    id,
                    "'$appName' açıldı. Axtarış sahəsi avtomatik tapılmadı. \"$query\" üçün axtarış düyməsinə basın.",
                    mapOf("app" to appName, "query" to query, "method" to "app_launched_only")
                )
            }
        } catch (e: Exception) {
            ToolResult.failed(id, "Tətbiq açılarkən xəta: ${e.message}")
        }
    }

    private fun tryBrowserFallback(context: Context, appName: String, query: String): ToolResult {
        return try {
            val searchUrl = when (appName) {
                "youtube" -> "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
                "spotify" -> "https://open.spotify.com/search/${Uri.encode(query)}"
                else -> "https://www.google.com/search?q=${Uri.encode("$appName $query")}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(
                id,
                "\"$query\" axtarışı brauzer vasitəsilə açıldı (${appName} quraşdırılmayıb və ya dəstəklənmir).",
                mapOf("app" to appName, "query" to query, "method" to "browser_fallback", "url" to searchUrl)
            )
        } catch (e: Exception) {
            ToolResult.failed(id, "Heç bir üsulla '$appName' üçün \"$query\" axtarışı açıla bilmədi: ${e.message}")
        }
    }
}

/**
 * APP_SEARCH — search within a specific application (non-media, e.g. browser search).
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

        // For browser apps, just open a Google search URL
        val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                // Try to open in specified browser
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
