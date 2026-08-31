package com.example.jarvis.tools.impl.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.jarvis.ai.provider.GeminiProvider
import com.example.jarvis.ai.vision.LocalVisionEngine
import com.example.jarvis.ai.vision.LocalVisionManager
import com.example.jarvis.core.JarvisResult
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class AnalyzePhotoTool(
    private val geminiProvider: GeminiProvider = GeminiProvider()
) : Tool {

    companion object {
        private const val TAG = "AnalyzePhotoTool"
    }

    override val id: String = "ANALYZE_PHOTO"
    override val name: String = "Şəkil Analizi / Vision AI"
    override val description: String = "Çəkilmiş şəkli və ya ekran görüntüsünü Gemini Vision AI ilə analiz edir və izah edir"
    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(name = "prompt", type = "string", isRequired = false, description = "Şəkil haqqında soruşmaq istədiyiniz sual", defaultValue = "Bu şəkildə nə görürsən? Ətraflı izah et."),
        ToolParameter(name = "image_path", type = "string", isRequired = false, description = "Şəkil faylının yolu", defaultValue = null)
    )
    override val requiredPermissions: List<String> = listOf("android.permission.CAMERA")
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val userPrompt = params["prompt"] ?: "Bu şəkildə nə görürsən? Ətraflı izah et."
        val imagePath = params["image_path"]

        val imageFile = if (!imagePath.isNullOrBlank()) {
            File(imagePath)
        } else {
            val photosDir = File(context.filesDir, "photos")
            photosDir.listFiles()?.maxByOrNull { it.lastModified() }
        }

        if (imageFile == null || !imageFile.exists()) {
            return@withContext ToolResult.failed(
                id,
                "Analiz ediləcək şəkil tapılmadı. Əvvəlcə 'şəkil çək' komandası ilə foto çəkin."
            )
        }

        try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            if (bitmap == null) {
                return@withContext ToolResult.failed(id, "Şəkil faylı oxuna bilmədi.")
            }

            val scaledBitmap = scaleBitmapIfNeeded(bitmap, 1024)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val imageBytes = outputStream.toByteArray()

            val isOnline = isInternetConnected(context)
            val localVisionManager = LocalVisionManager(context)
            val localVisionEngine = LocalVisionEngine(localVisionManager)

            if (!isOnline || localVisionManager.isModelReady()) {
                // Perform fast offline local vision analysis
                val offlineAnalysis = localVisionEngine.analyze(scaledBitmap, userPrompt)
                return@withContext ToolResult.success(
                    toolId = id,
                    message = offlineAnalysis,
                    data = mapOf("imagePath" to imageFile.absolutePath, "analysis" to offlineAnalysis, "source" to "LOCAL_VISION")
                )
            }

            val analysisResult = geminiProvider.analyzeImage(imageBytes, userPrompt)

            return@withContext when (analysisResult) {
                is JarvisResult.Success -> {
                    ToolResult.success(
                        toolId = id,
                        message = analysisResult.data,
                        data = mapOf("imagePath" to imageFile.absolutePath, "analysis" to analysisResult.data, "source" to "GEMINI_CLOUD")
                    )
                }
                is JarvisResult.Error -> {
                    // Cloud error -> Fallback to local vision
                    val fallbackAnalysis = localVisionEngine.analyze(scaledBitmap, userPrompt)
                    ToolResult.success(
                        toolId = id,
                        message = fallbackAnalysis,
                        data = mapOf("imagePath" to imageFile.absolutePath, "analysis" to fallbackAnalysis, "source" to "LOCAL_FALLBACK")
                    )
                }
                else -> {
                    val fallbackAnalysis = localVisionEngine.analyze(scaledBitmap, userPrompt)
                    ToolResult.success(toolId = id, message = fallbackAnalysis)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vision analysis failed: ${e.message}", e)
            return@withContext ToolResult.failed(id, "Şəkil analiz edilərkən xəta: ${e.message}")
        }
    }

    private fun isInternetConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) { false }
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
        val targetHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
