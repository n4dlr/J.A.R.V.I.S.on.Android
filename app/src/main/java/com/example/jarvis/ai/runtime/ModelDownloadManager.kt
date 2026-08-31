package com.example.jarvis.ai.runtime

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

sealed class ModelDownloadState {
    object Idle : ModelDownloadState()
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : ModelDownloadState()
    object Completed : ModelDownloadState()
    data class Failed(val error: String) : ModelDownloadState()
}

class ModelDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelDownloadManager"
        const val MODEL_FILENAME = "jarvis-az-qwen2.5-0.5b-q4_k_m.gguf"
        // HuggingFace direct download URL for Qwen2.5-0.5B-Instruct GGUF (Q4_K_M)
        const val DEFAULT_DOWNLOAD_URL = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
    }

    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    fun getModelFile(): File = File(context.filesDir, MODEL_FILENAME)

    fun isModelDownloaded(): Boolean {
        val file = getModelFile()
        // GGUF Q4_K_M for 0.5B is roughly ~350-400 MB. We check > 10MB to ensure not truncated
        return file.exists() && file.length() > 10 * 1024 * 1024
    }

    fun getModelSizeBytes(): Long {
        val file = getModelFile()
        return if (file.exists()) file.length() else 0L
    }

    suspend fun downloadModel(
        downloadUrl: String = DEFAULT_DOWNLOAD_URL,
        onProgress: ((Int) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val targetFile = getModelFile()
        val tempFile = File(context.filesDir, "$MODEL_FILENAME.tmp")

        try {
            _downloadState.value = ModelDownloadState.Downloading(0, 0, 0)
            Log.i(TAG, "Starting model download from $downloadUrl to ${tempFile.absolutePath}")

            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = true
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val err = "Server xətası: HTTP $responseCode"
                _downloadState.value = ModelDownloadState.Failed(err)
                return@withContext false
            }

            val totalBytes = connection.contentLengthLong
            var bytesDownloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var lastReportedPercent = -1

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesDownloaded += read

                        if (totalBytes > 0) {
                            val percent = ((bytesDownloaded * 100) / totalBytes).toInt()
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                _downloadState.value = ModelDownloadState.Downloading(percent, bytesDownloaded, totalBytes)
                                onProgress?.invoke(percent)
                            }
                        }
                    }
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                _downloadState.value = ModelDownloadState.Completed
                Log.i(TAG, "Model download complete! Size: ${targetFile.length()} bytes")
                return@withContext true
            } else {
                _downloadState.value = ModelDownloadState.Failed("Fayl tam yüklənmədi.")
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            if (tempFile.exists()) tempFile.delete()
            _downloadState.value = ModelDownloadState.Failed(e.message ?: "Yükləmə xətası")
            return@withContext false
        }
    }

    fun deleteModel(): Boolean {
        val file = getModelFile()
        val deleted = if (file.exists()) file.delete() else true
        if (deleted) {
            _downloadState.value = ModelDownloadState.Idle
        }
        return deleted
    }
}
