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

/**
 * Manages the Vosk offline Azerbaijani STT model lifecycle.
 *
 * The model (~50 MB) is downloaded from HuggingFace on demand
 * and stored in context.filesDir/vosk-model-az/
 *
 * Model source: https://alphacephei.com/vosk/models
 *   → vosk-model-small-az-0.3.zip (~50 MB, Azerbaijani, CPU inference)
 */
class VoskModelManager(private val context: Context) {

    companion object {
        private const val TAG = "VoskModelManager"
        private const val MODEL_DIR_NAME = "vosk-model-az"
        private const val MODEL_SENTINEL = "am/final.mdl" // File that confirms model is complete
        // Primary download URL: alphacephei official mirror
        private const val DOWNLOAD_URL =
            "https://alphacephei.com/vosk/models/vosk-model-small-az-0.3.zip"
        // Fallback: HuggingFace mirror
        private const val DOWNLOAD_URL_FALLBACK =
            "https://huggingface.co/alphacephei/vosk-model-small-az/resolve/main/vosk-model-small-az-0.3.zip"
    }

    sealed class VoskDownloadState {
        object Idle : VoskDownloadState()
        data class Downloading(val progressPercent: Int) : VoskDownloadState()
        object Completed : VoskDownloadState()
        data class Failed(val error: String) : VoskDownloadState()
    }

    private val _downloadState = MutableStateFlow<VoskDownloadState>(VoskDownloadState.Idle)
    val downloadState: StateFlow<VoskDownloadState> = _downloadState.asStateFlow()

    fun getModelDir(): File = File(context.filesDir, MODEL_DIR_NAME)

    fun getModelPath(): String = getModelDir().absolutePath

    fun isModelReady(): Boolean {
        val sentinelFile = File(getModelDir(), MODEL_SENTINEL)
        return sentinelFile.exists()
    }

    fun deleteModel() {
        getModelDir().deleteRecursively()
        _downloadState.value = VoskDownloadState.Idle
        Log.i(TAG, "Vosk model deleted.")
    }

    suspend fun downloadModel() = withContext(Dispatchers.IO) {
        if (isModelReady()) {
            _downloadState.value = VoskDownloadState.Completed
            return@withContext
        }

        _downloadState.value = VoskDownloadState.Downloading(0)

        val zipFile = File(context.cacheDir, "vosk-model-az.zip")
        val modelDir = getModelDir()
        modelDir.mkdirs()

        try {
            // Try primary URL, then fallback
            val downloaded = tryDownload(DOWNLOAD_URL, zipFile) ||
                    tryDownload(DOWNLOAD_URL_FALLBACK, zipFile)

            if (!downloaded) {
                _downloadState.value = VoskDownloadState.Failed("Yükləmə uğursuz oldu. İnternet bağlantısını yoxlayın.")
                return@withContext
            }

            _downloadState.value = VoskDownloadState.Downloading(90)

            // Unzip
            Log.i(TAG, "Unzipping Vosk model...")
            unzip(zipFile, modelDir)

            // The zip extracts to a subdirectory — move contents up
            flattenModelDir(modelDir)

            zipFile.delete()

            if (isModelReady()) {
                _downloadState.value = VoskDownloadState.Completed
                Log.i(TAG, "Vosk model installed successfully at ${modelDir.absolutePath}")
            } else {
                _downloadState.value = VoskDownloadState.Failed("Model faylları natamam yükləndi.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            zipFile.delete()
            _downloadState.value = VoskDownloadState.Failed("Xəta: ${e.message}")
        }
    }

    private fun tryDownload(url: String, dest: File): Boolean {
        return try {
            Log.i(TAG, "Downloading Vosk model from: $url")
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.connect()

            val totalBytes = conn.contentLengthLong
            var downloadedBytes = 0L

            FileOutputStream(dest).use { fos ->
                conn.inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        fos.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 85) / totalBytes).toInt()
                            _downloadState.value = VoskDownloadState.Downloading(progress.coerceIn(1, 85))
                        }
                    }
                }
            }
            conn.disconnect()
            dest.exists() && dest.length() > 1_000_000L // at least 1MB
        } catch (e: Exception) {
            Log.w(TAG, "Download from $url failed: ${e.message}")
            false
        }
    }

    private fun unzip(zipFile: File, destDir: File) {
        val zipInputStream = java.util.zip.ZipInputStream(zipFile.inputStream())
        var entry = zipInputStream.nextEntry
        while (entry != null) {
            val entryFile = File(destDir, entry.name)
            if (entry.isDirectory) {
                entryFile.mkdirs()
            } else {
                entryFile.parentFile?.mkdirs()
                FileOutputStream(entryFile).use { fos ->
                    zipInputStream.copyTo(fos)
                }
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
        zipInputStream.close()
    }

    /** If model was unzipped into a subdirectory, move contents up to modelDir directly. */
    private fun flattenModelDir(modelDir: File) {
        val children = modelDir.listFiles() ?: return
        // If there's exactly one child directory, it's the vosk-model subfolder
        if (children.size == 1 && children[0].isDirectory) {
            val subDir = children[0]
            subDir.listFiles()?.forEach { file ->
                file.renameTo(File(modelDir, file.name))
            }
            subDir.delete()
        }
    }
}
