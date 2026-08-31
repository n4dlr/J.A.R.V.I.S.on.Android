package com.example.jarvis.tools.impl.spotify

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Spotify Web API REST client.
 * Provides typed methods for the endpoints JARVIS needs.
 */
class SpotifyApiClient(
    private val authManager: SpotifyAuthManager
) {

    companion object {
        private const val TAG = "SpotifyApiClient"
        private const val BASE_URL = "https://api.spotify.com/v1"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * Search for a track/album/artist/playlist.
     * Returns the URI of the top result, or null if not found.
     */
    suspend fun search(query: String, type: String = "track"): SpotifySearchResult? = withContext(Dispatchers.IO) {
        val token = authManager.ensureValidToken() ?: return@withContext null
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$BASE_URL/search?q=$encodedQuery&type=$type&limit=1&market=AZ"

        try {
            val response = get(url, token) ?: return@withContext null
            val results = response.optJSONObject("${type}s") ?: return@withContext null
            val items = results.optJSONArray("items") ?: return@withContext null
            if (items.length() == 0) return@withContext null

            val item = items.getJSONObject(0)
            SpotifySearchResult(
                uri = item.optString("uri"),
                name = item.optString("name"),
                artist = extractArtist(item, type),
                type = type
            )
        } catch (e: Exception) {
            Log.e(TAG, "Search failed: ${e.message}", e)
            null
        }
    }

    // ── Playback Control ──────────────────────────────────────────────────────

    /** Play a specific URI (track, album, playlist, or artist). */
    suspend fun play(contextUri: String? = null, trackUri: String? = null): Boolean = withContext(Dispatchers.IO) {
        val token = authManager.ensureValidToken() ?: return@withContext false
        val bodyJson = JSONObject().apply {
            if (contextUri != null) put("context_uri", contextUri)
            if (trackUri != null) put("uris", org.json.JSONArray().put(trackUri))
        }
        put("$BASE_URL/me/player/play", bodyJson.toString(), token)
    }

    /** Pause current playback. */
    suspend fun pause(): Boolean = withContext(Dispatchers.IO) {
        val token = authManager.ensureValidToken() ?: return@withContext false
        put("$BASE_URL/me/player/pause", "{}", token)
    }

    /** Skip to next track. */
    suspend fun skipToNext(): Boolean = withContext(Dispatchers.IO) {
        val token = authManager.ensureValidToken() ?: return@withContext false
        post("$BASE_URL/me/player/next", token)
    }

    /** Skip to previous track. */
    suspend fun skipToPrevious(): Boolean = withContext(Dispatchers.IO) {
        val token = authManager.ensureValidToken() ?: return@withContext false
        post("$BASE_URL/me/player/previous", token)
    }

    /** Get the currently playing track info. */
    suspend fun getCurrentTrack(): CurrentTrackInfo? = withContext(Dispatchers.IO) {
        val token = authManager.ensureValidToken() ?: return@withContext null
        try {
            val response = get("$BASE_URL/me/player/currently-playing", token) ?: return@withContext null
            val item = response.optJSONObject("item") ?: return@withContext null
            val artists = item.optJSONArray("artists")
            val artistName = if (artists != null && artists.length() > 0) {
                artists.getJSONObject(0).optString("name", "")
            } else ""

            CurrentTrackInfo(
                trackName = item.optString("name"),
                artistName = artistName,
                albumName = item.optJSONObject("album")?.optString("name") ?: "",
                isPlaying = response.optBoolean("is_playing", false),
                progressMs = response.optInt("progress_ms", 0),
                durationMs = item.optInt("duration_ms", 0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Get current track failed: ${e.message}", e)
            null
        }
    }

    /** Set playback volume (0–100). */
    suspend fun setVolume(volumePercent: Int): Boolean = withContext(Dispatchers.IO) {
        val token = authManager.ensureValidToken() ?: return@withContext false
        val clampedVol = volumePercent.coerceIn(0, 100)
        put("$BASE_URL/me/player/volume?volume_percent=$clampedVol", "{}", token)
    }

    // ── HTTP Helpers ──────────────────────────────────────────────────────────

    private fun get(url: String, token: String): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        if (response.code == 204) return JSONObject() // no content = success
        val body = response.body?.string() ?: return null
        return if (response.isSuccessful) JSONObject(body) else {
            Log.w(TAG, "GET $url failed ${response.code}: $body")
            null
        }
    }

    private fun put(url: String, body: String, token: String): Boolean {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .put(body.toRequestBody("application/json".toMediaType()))
            .build()
        val response = httpClient.newCall(request).execute()
        return response.isSuccessful || response.code == 204
    }

    private fun post(url: String, token: String): Boolean {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post("".toRequestBody())
            .build()
        val response = httpClient.newCall(request).execute()
        return response.isSuccessful || response.code == 204
    }

    private fun extractArtist(item: JSONObject, type: String): String {
        return when (type) {
            "track" -> item.optJSONArray("artists")?.getJSONObject(0)?.optString("name") ?: ""
            "album" -> item.optJSONArray("artists")?.getJSONObject(0)?.optString("name") ?: ""
            "artist" -> item.optString("name")
            else -> ""
        }
    }
}

data class SpotifySearchResult(
    val uri: String,
    val name: String,
    val artist: String,
    val type: String
)

data class CurrentTrackInfo(
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val isPlaying: Boolean,
    val progressMs: Int,
    val durationMs: Int
)
