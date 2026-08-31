package com.example.jarvis.ai.runtime

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ModelHttpDownloader {
    private const val TAG = "ModelHttpDownloader"

    /**
     * Downloads a file from [initialUrl] into [destFile] following all redirects.
     * Reports progress 0-100% via [onProgress].
     * Returns true on success, false on failure.
     */
    suspend fun downloadFileWithRedirects(
        initialUrl: String,
        destFile: File,
        minValidSizeBytes: Long = 100_000L,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(destFile.parentFile ?: return@withContext false, "${destFile.name}.tmp")
        destFile.parentFile?.mkdirs()

        try {
            var currentUrl = initialUrl
            var connection: HttpURLConnection? = null
            var redirectCount = 0
            val maxRedirects = 8

            while (redirectCount < maxRedirects) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20000
                    readTimeout = 60000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; J.A.R.V.I.S.)")
                }
                connection.connect()

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308
                ) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = location
                        redirectCount++
                        Log.i(TAG, "Redirect #$redirectCount to: $currentUrl")
                        continue
                    }
                }

                if (status !in 200..299) {
                    Log.e(TAG, "HTTP error: $status from $currentUrl")
                    connection.disconnect()
                    return@withContext false
                }

                // Valid 200 OK connection established
                break
            }

            val conn = connection ?: return@withContext false
            val totalBytes = conn.contentLengthLong
            var downloadedBytes = 0L
            var lastReported = -1

            FileOutputStream(tempFile).use { fos ->
                conn.inputStream.use { input ->
                    val buffer = ByteArray(32 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        fos.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val percent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(1, 99)
                            if (percent != lastReported) {
                                lastReported = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
            conn.disconnect()

            if (tempFile.exists() && tempFile.length() >= minValidSizeBytes) {
                if (destFile.exists()) destFile.delete()
                val renamed = tempFile.renameTo(destFile)
                if (renamed) {
                    onProgress(100)
                    Log.i(TAG, "Download completed: ${destFile.absolutePath} (${destFile.length()} bytes)")
                    return@withContext true
                }
            }
            tempFile.delete()
            false
        } catch (e: Exception) {
            Log.e(TAG, "Download error from $initialUrl: ${e.message}", e)
            tempFile.delete()
            false
        }
    }
}
