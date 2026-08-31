package com.example.jarvis.voice

import android.content.Context
import android.util.Log
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class WakeWordEvent {
    object WakeWordOnly : WakeWordEvent()
    data class WakeWordWithCommand(val command: String) : WakeWordEvent()
}

class WakeWordDetector(
    private val context: Context,
    private val voiceRecognizer: VoiceRecognizerHelper,
    private val normalizer: AzerbaijaniTextNormalizer = AzerbaijaniTextNormalizer()
) {
    companion object {
        private const val TAG = "WakeWordDetector"
        private val WAKE_REGEX = Regex("""(?i)\b(?:hey\s+jarvis|jarvis|hey\s+carvis|carvis)\b""")
    }

    private val _isHotwordActive = MutableStateFlow(false)
    val isHotwordActive: StateFlow<Boolean> = _isHotwordActive.asStateFlow()

    private var hotwordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Checks if a transcribed phrase contains the wake word as a distinct word boundary.
     */
    fun extractWakeWordCommand(rawText: String): WakeWordEvent? {
        val normalized = normalizer.normalize(rawText).trim()
        if (normalized.isBlank()) return null

        val match = WAKE_REGEX.find(normalized) ?: return null

        val wakeStart = match.range.first
        val wakeEnd = match.range.last + 1

        // Extract any command before or after the wake word
        val before = normalized.substring(0, wakeStart).trim()
        val after = normalized.substring(wakeEnd).trim()
        val command = if (after.isNotBlank()) after else before

        if (command.isNotBlank()) {
            val rawLower = rawText.lowercase()
            val idx = rawLower.indexOf(command.take(6).trim())
            val cleanCommand = if (idx >= 0) rawText.substring(idx).trim() else command
            return WakeWordEvent.WakeWordWithCommand(cleanCommand)
        }

        return WakeWordEvent.WakeWordOnly
    }

    /**
     * Starts background continuous hotword listening loop when hands-free mode is enabled.
     */
    fun startContinuousHotwordListening(
        locale: String = "az-AZ",
        onWakeWordDetected: (WakeWordEvent) -> Unit
    ) {
        if (_isHotwordActive.value) return
        _isHotwordActive.value = true

        hotwordJob = scope.launch {
            while (isActive && _isHotwordActive.value) {
                if (!voiceRecognizer.isListening.value) {
                    voiceRecognizer.startListening(locale) { transcript ->
                        val wakeEvent = extractWakeWordCommand(transcript)
                        if (wakeEvent != null) {
                            Log.i(TAG, "Wake word detected: $wakeEvent")
                            onWakeWordDetected(wakeEvent)
                        }
                    }
                }
                delay(3000)
            }
        }
    }

    fun stopContinuousHotwordListening() {
        _isHotwordActive.value = false
        hotwordJob?.cancel()
        hotwordJob = null
        voiceRecognizer.stopListening()
    }
}
