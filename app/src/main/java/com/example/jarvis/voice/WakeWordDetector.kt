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
        private val WAKE_PATTERNS = listOf(
            "hey jarvis", "ey jarvis", "ay jarvis", "jarvis", "hey carvis", "carvis", "ceyvis", "hey ceyvis"
        )
    }

    private val _isHotwordActive = MutableStateFlow(false)
    val isHotwordActive: StateFlow<Boolean> = _isHotwordActive.asStateFlow()

    private var hotwordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Checks if a transcribed phrase starts with or contains the wake word.
     */
    fun extractWakeWordCommand(rawText: String): WakeWordEvent? {
        val normalized = normalizer.normalize(rawText).trim()

        for (wake in WAKE_PATTERNS) {
            if (normalized == wake) {
                return WakeWordEvent.WakeWordOnly
            }
            if (normalized.startsWith("$wake ")) {
                val command = normalized.substringAfter("$wake ").trim()
                if (command.isNotBlank()) {
                    // Try to preserve raw casing for the command
                    val rawLower = rawText.lowercase()
                    val idx = rawLower.indexOf(command.take(6).trim())
                    val cleanCommand = if (idx >= 0) rawText.substring(idx).trim() else command
                    return WakeWordEvent.WakeWordWithCommand(cleanCommand)
                }
                return WakeWordEvent.WakeWordOnly
            }
        }

        // Substring wake detection for natural conversational wake-up
        for (wake in WAKE_PATTERNS) {
            if (normalized.contains(wake)) {
                val command = normalized.substringAfter(wake).trim()
                return if (command.isNotBlank()) {
                    WakeWordEvent.WakeWordWithCommand(command)
                } else {
                    WakeWordEvent.WakeWordOnly
                }
            }
        }

        return null
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
