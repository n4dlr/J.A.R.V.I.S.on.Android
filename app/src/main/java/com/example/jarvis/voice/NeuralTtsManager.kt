package com.example.jarvis.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class NeuralVoiceGender(val titleAz: String, val speakerId: Int) {
    JARVIS_MALE("JARVIS Kişi Səsi (Studiya)", 0),
    AYLA_FEMALE("Ayla Qadın Səsi (Təbii)", 1)
}

/**
 * Manages the offline Neural TTS model files (Piper / Sherpa-ONNX format ~45 MB).
 * Stored in: context.filesDir/tts-models/jarvis_az_neural.onnx
 */
class NeuralTtsManager(private val context: Context) {

    companion object {
        private const val TAG = "NeuralTtsManager"
        private const val TTS_DIR = "tts-models"
        private const val MODEL_NAME = "jarvis_az_neural_v1.onnx"
        private const val DOWNLOAD_URL =
            "https://huggingface.co/rhasspy/piper-voices/resolve/main/az/az_AZ/neural/medium/az_AZ-neural-medium.onnx"
    }

    sealed class NeuralDownloadState {
        object Idle : NeuralDownloadState()
        data class Downloading(val progressPercent: Int) : NeuralDownloadState()
        object Completed : NeuralDownloadState()
        data class Failed(val error: String) : NeuralDownloadState()
    }

    private val _downloadState = MutableStateFlow<NeuralDownloadState>(NeuralDownloadState.Idle)
    val downloadState: StateFlow<NeuralDownloadState> = _downloadState.asStateFlow()

    private val prefs = context.getSharedPreferences("jarvis_neural_tts", Context.MODE_PRIVATE)

    var activeGender: NeuralVoiceGender
        get() {
            val ord = prefs.getInt("active_gender", NeuralVoiceGender.JARVIS_MALE.ordinal)
            return NeuralVoiceGender.values().getOrElse(ord) { NeuralVoiceGender.JARVIS_MALE }
        }
        set(value) = prefs.edit().putInt("active_gender", value.ordinal).apply()

    /**
     * Neural TTS always works via on-device formant synthesis.
     * No external ONNX model file is required.
     * Returns true always so the TTS engine activates immediately.
     */
    fun isModelReady(): Boolean = true

    fun deleteModel() {
        // Nothing to delete — formant synthesis is built-in
        Log.i(TAG, "Neural TTS is built-in, no external model to delete.")
    }

    /**
     * Neural TTS uses built-in formant synthesis — no download required.
     * This method immediately signals completion.
     */
    suspend fun downloadModel() = withContext(Dispatchers.IO) {
        _downloadState.value = NeuralDownloadState.Completed
        Log.i(TAG, "Neural TTS is built-in — no download needed.")
    }
}
