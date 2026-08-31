package com.example.jarvis.ai.vision

import android.content.Context
import android.util.Log
import com.example.jarvis.ai.runtime.ModelHttpDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages the offline Local Vision SLM model (SmolVLM / Moondream GGUF ~290 MB).
 * Stored in: context.filesDir/vision-models/smolvlm-256m-instruct-q4_k_m.gguf
 */
class LocalVisionManager(private val context: Context) {

    companion object {
        private const val TAG = "LocalVisionManager"
        private const val MODEL_DIR = "vision-models"
        private const val MODEL_FILENAME = "smolvlm-256m-instruct-q4_k_m.gguf"
        // HuggingFace direct download URL for quantized SmolVLM GGUF
        private const val DOWNLOAD_URL =
            "https://huggingface.co/HuggingFaceTB/SmolVLM-256M-Instruct-GGUF/resolve/main/smolvlm-256m-instruct-q4_k_m.gguf"
        private const val DOWNLOAD_URL_FALLBACK =
            "https://huggingface.co/vikhyatk/moondream2/resolve/main/moondream2-text-model-q4_k.gguf"
    }

    sealed class VisionDownloadState {
        object Idle : VisionDownloadState()
        data class Downloading(val progressPercent: Int) : VisionDownloadState()
        object Completed : VisionDownloadState()
        data class Failed(val error: String) : VisionDownloadState()
    }

    private val _downloadState = MutableStateFlow<VisionDownloadState>(VisionDownloadState.Idle)
    val downloadState: StateFlow<VisionDownloadState> = _downloadState.asStateFlow()

    fun getModelFile(): File = File(File(context.filesDir, MODEL_DIR), MODEL_FILENAME)

    fun isModelReady(): Boolean {
        val file = getModelFile()
        return file.exists() && file.length() > 5_000_000L // valid GGUF model > 5MB
    }

    fun deleteModel() {
        getModelFile().delete()
        _downloadState.value = VisionDownloadState.Idle
        Log.i(TAG, "Local vision model deleted.")
    }

    suspend fun downloadModel() = withContext(Dispatchers.IO) {
        if (isModelReady()) {
            _downloadState.value = VisionDownloadState.Completed
            return@withContext
        }

        _downloadState.value = VisionDownloadState.Downloading(0)
        val targetFile = getModelFile()

        val success = ModelHttpDownloader.downloadFileWithRedirects(
            initialUrl = DOWNLOAD_URL,
            destFile = targetFile,
            minValidSizeBytes = 5_000_000L
        ) { progress ->
            _downloadState.value = VisionDownloadState.Downloading(progress)
        } || ModelHttpDownloader.downloadFileWithRedirects(
            initialUrl = DOWNLOAD_URL_FALLBACK,
            destFile = targetFile,
            minValidSizeBytes = 5_000_000L
        ) { progress ->
            _downloadState.value = VisionDownloadState.Downloading(progress)
        }

        if (success && isModelReady()) {
            _downloadState.value = VisionDownloadState.Completed
            Log.i(TAG, "Local vision model installed successfully.")
        } else {
            _downloadState.value = VisionDownloadState.Failed("Vision model faylı yüklənə bilmədi.")
        }
    }
}
