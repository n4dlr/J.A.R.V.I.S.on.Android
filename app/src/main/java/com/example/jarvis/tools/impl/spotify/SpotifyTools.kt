package com.example.jarvis.tools.impl.spotify

import android.content.Context
import android.util.Log
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

// ── SPOTIFY_PLAY ────────────────────────────────────────────────────────────

class SpotifyPlayTool(
    private val auth: SpotifyAuthManager,
    private val client: SpotifyApiClient
) : Tool {
    override val id = "SPOTIFY_PLAY"
    override val name = "Spotify-da Mahnı Çal"
    override val description = "Spotify-da mahnı, artist, albom və ya pleylist axtarıb çalmağa başlayır"
    override val parameters = listOf(
        ToolParameter("query", "string", isRequired = true, description = "Mahnı adı, artist adı, albom, pleylist"),
        ToolParameter("type", "string", isRequired = false, description = "track/artist/album/playlist", defaultValue = "track")
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        if (!auth.isConfigured()) {
            return ToolResult.failed(id, "Spotify client_id konfiqurasiya edilməyib. .env faylına SPOTIFY_CLIENT_ID əlavə edin.")
        }
        if (!auth.isAuthenticated()) {
            auth.openAuthInBrowser()
            return ToolResult.success(id, "Spotify giriş ekranı açıldı. Daxil olduqdan sonra əmri təkrarlayın.")
        }

        val query = params["query"] ?: return ToolResult.failed(id, "Axtarış sorğusu verilməyib.")
        val type = params["type"] ?: "track"

        return try {
            val searchResult = client.search(query, type)
            if (searchResult == null) {
                return ToolResult.failed(id, "\"$query\" üçün Spotify-da nəticə tapılmadı.")
            }

            val played = when (type) {
                "track" -> client.play(trackUri = searchResult.uri)
                else -> client.play(contextUri = searchResult.uri)
            }

            if (played) {
                val artist = if (searchResult.artist.isNotBlank()) " (${searchResult.artist})" else ""
                ToolResult.success(
                    toolId = id,
                    message = "🎵 Spotify: \"${searchResult.name}\"$artist çalınmağa başladı.",
                    data = mapOf("track" to searchResult.name, "artist" to searchResult.artist, "uri" to searchResult.uri)
                )
            } else {
                ToolResult.failed(id, "Spotify çala bilmədi. Spotify tətbiqinin aktiv olduğunu yoxlayın.")
            }
        } catch (e: Exception) {
            Log.e("SpotifyPlayTool", "Error: ${e.message}", e)
            ToolResult.failed(id, "Spotify xətası: ${e.message}")
        }
    }
}

// ── SPOTIFY_PAUSE ───────────────────────────────────────────────────────────

class SpotifyPauseTool(
    private val auth: SpotifyAuthManager,
    private val client: SpotifyApiClient
) : Tool {
    override val id = "SPOTIFY_PAUSE"
    override val name = "Spotify-nı Durdur"
    override val description = "Spotify-da oynayan mahnını dayandırır"
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = auth.isAuthenticated()

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            if (client.pause()) ToolResult.success(id, "Spotify dayandırıldı ⏸️")
            else ToolResult.failed(id, "Spotify dayandırıla bilmədi.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Xəta: ${e.message}")
        }
    }
}

// ── SPOTIFY_NEXT ─────────────────────────────────────────────────────────────

class SpotifyNextTool(
    private val auth: SpotifyAuthManager,
    private val client: SpotifyApiClient
) : Tool {
    override val id = "SPOTIFY_NEXT"
    override val name = "Spotify Növbəti Mahnı"
    override val description = "Spotify-da növbəti mahnıya keçir"
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = auth.isAuthenticated()

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            if (client.skipToNext()) ToolResult.success(id, "Spotify: Növbəti mahnıya keçildi ⏭️")
            else ToolResult.failed(id, "Növbəti mahnıya keçilə bilmədi.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Xəta: ${e.message}")
        }
    }
}

// ── SPOTIFY_PREVIOUS ─────────────────────────────────────────────────────────

class SpotifyPreviousTool(
    private val auth: SpotifyAuthManager,
    private val client: SpotifyApiClient
) : Tool {
    override val id = "SPOTIFY_PREVIOUS"
    override val name = "Spotify Əvvəlki Mahnı"
    override val description = "Spotify-da əvvəlki mahnıya qayıdır"
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = auth.isAuthenticated()

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            if (client.skipToPrevious()) ToolResult.success(id, "Spotify: Əvvəlki mahnıya qayıdıldı ⏮️")
            else ToolResult.failed(id, "Əvvəlki mahnıya qayıdıla bilmədi.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Xəta: ${e.message}")
        }
    }
}

// ── SPOTIFY_CURRENT_TRACK ────────────────────────────────────────────────────

class SpotifyCurrentTrackTool(
    private val auth: SpotifyAuthManager,
    private val client: SpotifyApiClient
) : Tool {
    override val id = "SPOTIFY_CURRENT_TRACK"
    override val name = "Hal-hazırki Spotify Mahnısı"
    override val description = "Hal-hazırda Spotify-da hansı mahnının çalındığını göstərir"
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = auth.isAuthenticated()

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val track = client.getCurrentTrack()
            if (track == null) {
                return ToolResult.success(id, "Hal-hazırda Spotify-da heç nə çalmır.")
            }
            val status = if (track.isPlaying) "çalınır 🎵" else "dayandırılıb ⏸️"
            val artist = if (track.artistName.isNotBlank()) " — ${track.artistName}" else ""
            ToolResult.success(
                toolId = id,
                message = "\"${track.trackName}\"$artist $status (albom: ${track.albumName})",
                data = mapOf(
                    "track" to track.trackName,
                    "artist" to track.artistName,
                    "album" to track.albumName,
                    "isPlaying" to track.isPlaying.toString()
                )
            )
        } catch (e: Exception) {
            ToolResult.failed(id, "Xəta: ${e.message}")
        }
    }
}
