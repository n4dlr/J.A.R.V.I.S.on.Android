package com.example.jarvis.tools.impl.spotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Manages Spotify OAuth 2.0 PKCE authentication flow.
 *
 * SETUP (one-time, free):
 *   1. Go to https://developer.spotify.com/dashboard
 *   2. Create an app → copy "Client ID"
 *   3. Add Redirect URI: "jarvis://spotify-callback"
 *   4. Paste the Client ID in your .env file as: SPOTIFY_CLIENT_ID=your_client_id_here
 *
 * The token is stored in SharedPreferences (sufficiently secure for this use-case;
 * for production, use EncryptedSharedPreferences).
 */
class SpotifyAuthManager(private val context: Context) {

    companion object {
        private const val TAG = "SpotifyAuthManager"
        private const val PREF_FILE = "jarvis_spotify_tokens"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_CODE_VERIFIER = "code_verifier"

        const val REDIRECT_URI = "jarvis://spotify-callback"
        const val SCOPES = "user-read-playback-state user-modify-playback-state user-read-currently-playing playlist-read-private"
        const val AUTH_URL = "https://accounts.spotify.com/authorize"
        const val TOKEN_URL = "https://accounts.spotify.com/api/token"

        // Read client ID from BuildConfig (set via .env → SPOTIFY_CLIENT_ID)
        // If not configured, returns empty string — auth will gracefully fail.
        private fun getClientId(context: Context): String {
            return try {
                val clazz = Class.forName("${context.packageName}.BuildConfig")
                clazz.getField("SPOTIFY_CLIENT_ID").get(null) as? String ?: ""
            } catch (_: Exception) {
                ""
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient()

    // ── Token Management ──────────────────────────────────────────────────────

    fun hasValidToken(): Boolean {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return token != null && System.currentTimeMillis() < expiresAt - 60_000L
    }

    fun getAccessToken(): String? {
        return if (hasValidToken()) prefs.getString(KEY_ACCESS_TOKEN, null) else null
    }

    suspend fun ensureValidToken(): String? {
        if (hasValidToken()) return getAccessToken()
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        return refreshAccessToken(refreshToken)
    }

    fun isAuthenticated(): Boolean {
        return prefs.getString(KEY_REFRESH_TOKEN, null) != null
    }

    fun getClientId(): String = getClientId(context)

    fun isConfigured(): Boolean = getClientId(context).isNotBlank()

    // ── PKCE Flow ─────────────────────────────────────────────────────────────

    fun buildAuthorizationUrl(): String {
        val clientId = getClientId(context)
        if (clientId.isBlank()) {
            Log.w(TAG, "Spotify client_id not configured. Set SPOTIFY_CLIENT_ID in .env")
            return ""
        }

        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        prefs.edit().putString(KEY_CODE_VERIFIER, codeVerifier).apply()

        return Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", codeChallenge)
            .build()
            .toString()
    }

    fun openAuthInBrowser() {
        val url = buildAuthorizationUrl()
        if (url.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun handleCallback(code: String): Boolean = withContext(Dispatchers.IO) {
        val codeVerifier = prefs.getString(KEY_CODE_VERIFIER, null) ?: return@withContext false
        val clientId = getClientId(context)

        try {
            val body = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .add("client_id", clientId)
                .add("code_verifier", codeVerifier)
                .build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext false

            if (!response.isSuccessful) {
                Log.e(TAG, "Token exchange failed: $responseBody")
                return@withContext false
            }

            saveTokens(JSONObject(responseBody))
            Log.i(TAG, "Spotify auth successful!")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Callback handling failed: ${e.message}", e)
            return@withContext false
        }
    }

    private suspend fun refreshAccessToken(refreshToken: String): String? = withContext(Dispatchers.IO) {
        val clientId = getClientId(context)
        try {
            val body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", clientId)
                .build()

            val request = Request.Builder().url(TOKEN_URL).post(body).build()
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null

            if (!response.isSuccessful) {
                Log.w(TAG, "Token refresh failed: $responseBody")
                return@withContext null
            }

            val json = JSONObject(responseBody)
            saveTokens(json)
            return@withContext json.optString("access_token")
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh error: ${e.message}", e)
            null
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
        Log.i(TAG, "Spotify tokens cleared.")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun saveTokens(json: JSONObject) {
        val accessToken = json.optString("access_token")
        val refreshToken = json.optString("refresh_token")
        val expiresIn = json.optLong("expires_in", 3600L)

        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            if (refreshToken.isNotBlank()) putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + expiresIn * 1000L)
            apply()
        }
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
