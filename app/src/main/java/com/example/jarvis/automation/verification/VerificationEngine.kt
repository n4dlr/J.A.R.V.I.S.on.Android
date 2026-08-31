package com.example.jarvis.automation.verification

import android.content.Context
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.example.jarvis.automation.observation.ObservationEngine
import com.example.jarvis.services.AccessibilityController
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

sealed class VerificationResult {
    data class Verified(val evidence: String) : VerificationResult()
    data class Partial(val reason: String) : VerificationResult()
    data class Failed(val error: String) : VerificationResult()
}

class VerificationEngine(
    private val context: Context,
    private val observationEngine: ObservationEngine,
    private val accessibilityController: AccessibilityController
) {

    /**
     * Verifies that the app package has actually reached the foreground.
     */
    suspend fun verifyAppForeground(packageName: String, timeoutMs: Long = 2500L): VerificationResult {
        val inFg = accessibilityController.waitForAppForeground(packageName, timeoutMs)
        return if (inFg) {
            VerificationResult.Verified("Tətbiq ön plana keçdi ($packageName).")
        } else {
            val curObs = observationEngine.observeCurrentState()
            if (curObs.foregroundPackage != null && curObs.foregroundPackage.contains(packageName, ignoreCase = true)) {
                VerificationResult.Verified("Tətbiq aktivdir (${curObs.foregroundPackage}).")
            } else {
                VerificationResult.Partial("Tətbiq başladıldı, lakin ön plan təsdiqi alınmadı.")
            }
        }
    }

    /**
     * Verifies media playback in YouTube, Spotify, or System media session.
     */
    suspend fun verifyPlayback(targetApp: String, query: String, timeoutMs: Long = 3500L): VerificationResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        // Step 1: Wait up to timeout for MediaSession or Audio playback
        val verified = withTimeoutOrNull(timeoutMs) {
            while (true) {
                // Check if audio is actively playing
                val isMusicActive = audioManager?.isMusicActive == true

                // Check MediaSession if available
                val obs = observationEngine.observeCurrentState()
                if (obs.isMediaPlaying || isMusicActive) {
                    return@withTimeoutOrNull VerificationResult.Verified("Audio/Video oynatması aşkar edildi.")
                }

                // Check UI text for YouTube/Spotify player indicators
                val visible = obs.visibleText.lowercase()
                if (visible.contains("pause") || visible.contains("dayandır") || visible.contains("durdur") ||
                    visible.contains("play") || visible.contains("video player") || visible.contains("oynadılır")
                ) {
                    return@withTimeoutOrNull VerificationResult.Verified("Pleyer interfeysi aktivdir.")
                }

                delay(300)
            }
            null
        }

        return verified ?: VerificationResult.Partial(
            "'$targetApp' açıldı və axtarış edildi, lakin oynatmanın avtomatik başlamasını təsdiqləyə bilmədim."
        )
    }

    /**
     * Verifies search results displayed.
     */
    suspend fun verifySearchResultsDisplayed(query: String, timeoutMs: Long = 2000L): VerificationResult {
        val verified = withTimeoutOrNull(timeoutMs) {
            while (true) {
                val obs = observationEngine.observeCurrentState()
                val queryTerms = query.lowercase().split(" ").filter { it.length > 2 }
                val visible = obs.visibleText.lowercase()
                if (queryTerms.any { visible.contains(it) }) {
                    return@withTimeoutOrNull VerificationResult.Verified("Axtarış nəticələri ekranda göstərilir.")
                }
                delay(250)
            }
            null
        }
        return verified ?: VerificationResult.Partial("Axtarış göndərildi, lakin nəticə siyahısı hələ tam yüklənməyib.")
    }
}
