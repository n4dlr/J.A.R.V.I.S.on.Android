package com.example.jarvis.voice

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Continuous hands-free voice conversation session.
 *
 * Unlike single-shot voice recognition, this session:
 *  1. Starts listening via SpeechRecognizer
 *  2. Passes recognized text to [onCommand] callback
 *  3. Waits for TTS to finish (echo-cancellation)
 *  4. Automatically resumes listening after a brief pause
 *  5. Repeats until the user says a stop keyword or [stop] is called
 *
 * Stop keywords: "saxla", "bitir", "dur", "çıx", "stop", "exit", "dayandır"
 */
class ContinuousVoiceSession(
    private val context: Context,
    private val voiceHelper: VoiceRecognizerHelper,
    private val ttsHelper: TextToSpeechHelper,
    private val normalizer: AzerbaijaniTextNormalizer,
    private val onCommand: suspend (String) -> Unit,
    private val onSessionStarted: () -> Unit = {},
    private val onSessionStopped: () -> Unit = {}
) {
    companion object {
        private const val TAG = "ContinuousVoiceSession"
        private const val RESUME_DELAY_MS = 400L
        private const val TTS_POLL_INTERVAL_MS = 150L

        private val STOP_KEYWORDS = setOf(
            "saxla", "bitir", "dur", "çıx", "cix", "stop", "exit",
            "dayandır", "dayandir", "sessiyanı bitir", "danışmağı bitir"
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sessionJob: Job? = null

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    // WakeLock keeps CPU alive while session runs (screen may be off)
    private var wakeLock: PowerManager.WakeLock? = null

    fun start(locale: String = "az-AZ") {
        if (_isActive.value) return
        _isActive.value = true
        Log.i(TAG, "Continuous voice session started")

        // Acquire partial wake lock so session survives screen-off
        acquireWakeLock()
        onSessionStarted()

        sessionJob = scope.launch {
            try {
                runSessionLoop(locale)
            } catch (e: CancellationException) {
                Log.i(TAG, "Session cancelled.")
            } catch (e: Exception) {
                Log.e(TAG, "Session error: ${e.message}", e)
            } finally {
                cleanup()
            }
        }
    }

    fun stop() {
        Log.i(TAG, "Stopping continuous session...")
        sessionJob?.cancel()
        voiceHelper.stopListening()
        cleanup()
    }

    private suspend fun runSessionLoop(locale: String) {
        while (_isActive.value) {
            // Wait for any active TTS to finish before listening (echo protection)
            waitForTtsToFinish()

            if (!_isActive.value) break

            // Brief pause so the microphone doesn't pick up TTS tail audio
            delay(RESUME_DELAY_MS)

            if (!_isActive.value) break

            // Listen for one utterance
            val recognizedText = awaitSpeechResult(locale) ?: continue

            // Check for stop keywords
            val normalized = normalizer.normalize(recognizedText)
            if (STOP_KEYWORDS.any { normalized.contains(it) }) {
                Log.i(TAG, "Stop keyword detected: $recognizedText")
                ttsHelper.speak("Davamlı danışıq rejimi bağlandı.")
                break
            }

            // Process command
            try {
                onCommand(recognizedText)
            } catch (e: Exception) {
                Log.e(TAG, "Command processing error: ${e.message}", e)
            }

            // Small breathing room before next listen cycle
            delay(200)
        }
        cleanup()
    }

    private suspend fun awaitSpeechResult(locale: String): String? {
        var result: String? = null
        var done = false

        voiceHelper.startListening(locale) { text ->
            result = text.trim()
            done = true
        }

        // Poll until we get a result or session is stopped
        while (!done && _isActive.value) {
            delay(100)
        }

        return result
    }

    private suspend fun waitForTtsToFinish() {
        // Poll isSpeaking state
        var waited = 0
        while (ttsHelper.isSpeaking.value && waited < 15_000) {
            delay(TTS_POLL_INTERVAL_MS)
            waited += TTS_POLL_INTERVAL_MS.toInt()
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "JARVIS:ContinuousVoiceSession"
            ).also { it.acquire(30 * 60 * 1000L) } // max 30 minutes
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire wake lock: ${e.message}")
        }
    }

    private fun cleanup() {
        _isActive.value = false
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
        } catch (_: Exception) {}
        wakeLock = null
        scope.cancel()
        onSessionStopped()
        Log.i(TAG, "Continuous voice session cleaned up.")
    }
}
