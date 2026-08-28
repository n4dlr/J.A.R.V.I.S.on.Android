package com.example.jarvis.tools.impl.camera

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

/** OPEN_CAMERA — open the default camera app. */
class OpenCameraTool : Tool {
    override val id = "OPEN_CAMERA"
    override val name = "Kameranı Aç"
    override val description = "Cihazın kamera tətbiqini açır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ToolResult.success(id, "Kamera açıldı.")
            } else {
                ToolResult.unsupported(id, "Bu cihazda kamera tətbiqi tapılmadı.")
            }
        } catch (e: Exception) {
            ToolResult.failed(id, "Kamera açıla bilmədi: ${e.message}")
        }
    }
}

/** RECORD_VIDEO — launch camera app in video recording mode. */
class RecordVideoTool : Tool {
    override val id = "RECORD_VIDEO"
    override val name = "Video Yaz"
    override val description = "Kameranı video yazma rejiminə keçirir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = listOf(android.Manifest.permission.CAMERA)
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ToolResult.success(id, "Kamera video yazma rejiminə keçirildi.")
            } else {
                ToolResult.unsupported(id, "Video kamera tətbiqi tapılmadı.")
            }
        } catch (e: Exception) {
            ToolResult.failed(id, "Video kamera açıla bilmədi: ${e.message}")
        }
    }
}
