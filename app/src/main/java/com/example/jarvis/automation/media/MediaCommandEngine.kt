package com.example.jarvis.automation.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.accessibility.AccessibilityNodeInfo
import com.example.jarvis.automation.apps.AppResolver
import com.example.jarvis.automation.observation.ObservationEngine
import com.example.jarvis.automation.verification.VerificationEngine
import com.example.jarvis.automation.verification.VerificationResult
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.services.AccessibilityController
import kotlinx.coroutines.delay

class MediaCommandEngine(
    private val context: Context,
    private val appResolver: AppResolver,
    private val accessibilityController: AccessibilityController,
    private val observationEngine: ObservationEngine,
    private val verificationEngine: VerificationEngine
) {

    private val youtubePackage = "com.google.android.youtube"
    private val spotifyPackage = "com.spotify.music"

    /**
     * Executes YouTube Search & Play end-to-end.
     */
    suspend fun searchAndPlayYouTube(query: String): ToolResult {
        if (query.isBlank()) {
            return ToolResult.failed("MEDIA_SEARCH_PLAY", "Axtarış üçün mahnı və ya ifaçı adı qeyd olunmayıb.")
        }

        val isInstalled = appResolver.isPackageInstalled(youtubePackage)

        // If YouTube app is not installed, fallback immediately to web YouTube
        if (!isInstalled) {
            val webUrl = "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
            return try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                ToolResult.partialSuccess(
                    toolId = "MEDIA_SEARCH_PLAY",
                    message = "YouTube tətbiqi quraşdırılmadığı üçün \"$query\" axtarışı brauzerdə açıldı.",
                    data = mapOf("query" to query, "url" to webUrl, "method" to "browser_web")
                )
            } catch (e: Exception) {
                ToolResult.failed("MEDIA_SEARCH_PLAY", "YouTube açıla bilmədi: ${e.message}")
            }
        }

        // STRATEGY 1: Intent with direct URI or launcher
        val deepLinkUri = "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUri)).apply {
            setPackage(youtubePackage)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(youtubePackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
            }
        }

        // Wait up to 3s for YouTube to enter foreground
        val inForeground = accessibilityController.waitForAppForeground(youtubePackage, timeoutMs = 3000L)

        // STRATEGY 2: If Accessibility is enabled and active, inspect and automate UI
        if (accessibilityController.isServiceEnabled()) {
            delay(1000)

            // Step A: Check if search field or search icon needs to be tapped
            val searchButton = accessibilityController.findByContentDescription("Search")
                .firstOrNull()
                ?: accessibilityController.findByContentDescription("Axtarış").firstOrNull()
                ?: accessibilityController.findByViewId("com.google.android.youtube:id/menu_item_1").firstOrNull()

            if (searchButton != null && !observationEngine.observeCurrentState().visibleText.contains(query, ignoreCase = true)) {
                accessibilityController.clickNode(searchButton)
                delay(600)
                accessibilityController.typeText(query)
                delay(400)
            }

            // Step B: Match candidate search result node or click the first video item
            val bestCandidateNode = findBestMatchingVideoNode(query) ?: findFirstVideoItem()
            if (bestCandidateNode != null) {
                accessibilityController.clickNode(bestCandidateNode)
                delay(1200)
            }
        }

        // Step C: Verify Playback
        return when (val verify = verificationEngine.verifyPlayback("YouTube", query, timeoutMs = 3500L)) {
            is VerificationResult.Verified -> {
                ToolResult.success(
                    toolId = "MEDIA_SEARCH_PLAY",
                    message = "YouTube-da \"$query\" mahnısı açıldı və oynadılır.",
                    data = mapOf("app" to "YouTube", "query" to query, "verified" to true),
                    verification = verify.evidence
                )
            }
            is VerificationResult.Partial -> {
                ToolResult.partialSuccess(
                    toolId = "MEDIA_SEARCH_PLAY",
                    message = "YouTube-da \"$query\" açıldı və oynadılır.",
                    data = mapOf("app" to "YouTube", "query" to query, "verified" to false),
                    reason = verify.reason
                )
            }
            is VerificationResult.Failed -> {
                ToolResult.failed(
                    toolId = "MEDIA_SEARCH_PLAY",
                    error = "YouTube əməliyyatı uğursuz oldu: ${verify.error}"
                )
            }
        }
    }

    /**
     * Executes Spotify Search & Play.
     */
    suspend fun searchAndPlaySpotify(query: String): ToolResult {
        if (query.isBlank()) {
            return ToolResult.failed("MEDIA_SEARCH_PLAY", "Axtarış üçün sorğu qeyd olunmayıb.")
        }

        val spotifyUri = "spotify:search:${Uri.encode(query)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUri)).apply {
            setPackage(spotifyPackage)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val isInstalled = appResolver.isPackageInstalled(spotifyPackage)
        if (!isInstalled) {
            val webUrl = "https://open.spotify.com/search/${Uri.encode(query)}"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
            return ToolResult.partialSuccess(
                toolId = "MEDIA_SEARCH_PLAY",
                message = "Spotify quraşdırılmadığı üçün \"$query\" brauzerdə açıldı.",
                data = mapOf("app" to "Spotify", "query" to query, "url" to webUrl)
            )
        }

        return try {
            context.startActivity(intent)
            delay(1000)
            ToolResult.success(
                toolId = "MEDIA_SEARCH_PLAY",
                message = "Spotify-da \"$query\" axtarışı açıldı.",
                data = mapOf("app" to "Spotify", "query" to query)
            )
        } catch (e: Exception) {
            ToolResult.failed("MEDIA_SEARCH_PLAY", "Spotify açıla bilmədi: ${e.message}")
        }
    }

    /**
     * Inspects current Accessibility tree to find the video node best matching query terms (artist + title).
     */
    private fun findBestMatchingVideoNode(query: String): AccessibilityNodeInfo? {
        val root = accessibilityController.getRootNode() ?: return null
        val queryTokens = query.lowercase().split("\\s+".toRegex()).filter { it.length >= 2 }
        var bestNode: AccessibilityNodeInfo? = null
        var maxMatches = 0

        fun scoreNode(node: AccessibilityNodeInfo) {
            val text = (node.text?.toString().orEmpty() + " " + node.contentDescription?.toString().orEmpty()).lowercase()
            if (text.isNotBlank() && (node.isClickable || node.parent?.isClickable == true)) {
                var matches = 0
                for (token in queryTokens) {
                    if (text.contains(token)) matches++
                }
                if (matches > maxMatches) {
                    maxMatches = matches
                    bestNode = if (node.isClickable) node else node.parent
                }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { scoreNode(it) }
            }
        }

        scoreNode(root)
        return bestNode
    }

    /**
     * Fallback: finds the first clickable search result item in the YouTube feed.
     */
    private fun findFirstVideoItem(): AccessibilityNodeInfo? {
        val root = accessibilityController.getRootNode() ?: return null
        var firstClickable: AccessibilityNodeInfo? = null

        fun find(node: AccessibilityNodeInfo) {
            if (firstClickable != null) return
            val desc = node.contentDescription?.toString().orEmpty().lowercase()
            // YouTube video titles in accessibility tree typically contain duration or views
            val looksLikeVideo = desc.contains("minute") || desc.contains("second") ||
                    desc.contains("view") || desc.contains("baxış") || desc.contains("dəqiqə")

            if (looksLikeVideo && (node.isClickable || node.parent?.isClickable == true)) {
                firstClickable = if (node.isClickable) node else node.parent
                return
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { find(it) }
            }
        }

        find(root)
        return firstClickable
    }
}
