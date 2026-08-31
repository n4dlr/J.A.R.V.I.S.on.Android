package com.example.jarvis.ai.vision

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

/**
 * Manages the download and lifecycle of the on-device Local Vision SLM
 * (SmolVLM-256M / Moondream2 GGUF Q4_K_M ~290 MB).
 *
 * Stored in: context.filesDir/vision-models/smolvlm-q4.gguf
 */
class LocalVisionManager(private val context: Context) {

    companion object {
        private const val TAG = "LocalVisionManager"
        private const val MODEL_DIR = "vision-models"
        private const val MODEL_FILENAME = "smolvlm-256m-q4_k_m.gguf"
        private const val DOWNLOAD_URL =
            "https://huggingface.co/HuggingFaceTB/SmolVLM-256M-Instruct-GGUF/resolve/main/smolvlm-256m-instruct-q4_k_m.gguf"
        private const val FALLBACK_URL =
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
        return file.exists() && file.length() > 5_000_000L // valid model file > 5MB
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
        val dir = File(context.filesDir, MODEL_DIR)
        dir.mkdirs()
        val tempFile = File(dir, "$MODEL_FILENAME.tmp")

        try {
            val success = tryDownload(DOWNLOAD_URL, tempFile) || tryDownload(FALLBACK_URL, tempFile)
            if (!success) {
                _downloadState.value = VisionDownloadState.Failed("Model yüklənməsi uğursuz oldu. İnternet bağlantısını yoxlayın.")
                tempFile.delete()
                return@withContext
            }

            tempFile.renameTo(getModelFile())
            _downloadState.value = VisionDownloadState.Completed
            Log.i(TAG, "Local Vision model successfully installed.")
        } catch (e: Exception) {
            Log.e(TAG, "Local vision download error: ${e.message}", e)
            tempFile.delete()
            _downloadState.value = VisionDownloadState.Failed("Xəta: ${e.message}")
        }
    }

    private fun tryDownload(urlStr: String, dest: File): Boolean {
        return try {
            Log.i(TAG, "Downloading vision model from $urlStr")
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.connect()

            val total = conn.contentLengthLong
            var downloaded = 0L

            FileOutputStream(dest).use { fos ->
                conn.inputStream.use { input ->
                    val buffer = ByteArray(16384)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        fos.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val progress = ((downloaded * 100) / total).toInt().coerceIn(1, 99)
                            _downloadState.value = VisionDownloadState.Downloading(progress)
                        }
                    }
                }
            }
            conn.disconnect()
            dest.exists() && dest.length() > 5_000_000L
        } catch (e: Exception) {
            Log.w(TAG, "Failed to download from $urlStr: ${e.message}")
            false
        }
    }
}
