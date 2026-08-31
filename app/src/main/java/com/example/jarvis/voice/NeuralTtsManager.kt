package com.example.jarvis.voice

import android.content.Context
import android.util.Log
import com.example.jarvis.ai.runtime.ModelHttpDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

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
        private const val DOWNLOAD_URL_FALLBACK =
            "https://github.com/rhasspy/piper/releases/download/v0.0.2/voice-az_AZ-neural-medium.onnx"
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

    fun getModelFile(): File = File(File(context.filesDir, TTS_DIR), MODEL_NAME)

    fun isModelReady(): Boolean {
        val file = getModelFile()
        return file.exists() && file.length() > 500_000L
    }

    fun deleteModel() {
        getModelFile().delete()
        _downloadState.value = NeuralDownloadState.Idle
        Log.i(TAG, "Neural TTS model deleted.")
    }

    suspend fun downloadModel() = withContext(Dispatchers.IO) {
        if (isModelReady()) {
            _downloadState.value = NeuralDownloadState.Completed
            return@withContext
        }

        _downloadState.value = NeuralDownloadState.Downloading(0)
        val targetFile = getModelFile()

        val success = ModelHttpDownloader.downloadFileWithRedirects(
            initialUrl = DOWNLOAD_URL,
            destFile = targetFile,
            minValidSizeBytes = 500_000L
        ) { progress ->
            _downloadState.value = NeuralDownloadState.Downloading(progress)
        } || ModelHttpDownloader.downloadFileWithRedirects(
            initialUrl = DOWNLOAD_URL_FALLBACK,
            destFile = targetFile,
            minValidSizeBytes = 500_000L
        ) { progress ->
            _downloadState.value = NeuralDownloadState.Downloading(progress)
        }

        if (success && isModelReady()) {
            _downloadState.value = NeuralDownloadState.Completed
            Log.i(TAG, "Neural TTS model installed successfully.")
        } else {
            _downloadState.value = NeuralDownloadState.Failed("Neyron səs modeli yüklənə bilmədi.")
        }
    }
}
