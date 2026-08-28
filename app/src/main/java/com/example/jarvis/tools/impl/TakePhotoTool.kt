package com.example.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

class TakePhotoTool : Tool {
    override val id: String = "TAKE_PHOTO"
    override val name: String = "Kamera / Şəkil Çək"
    override val description: String = "Kameranı aktivləşdirir və foto rejimini açır."
    override val parameters: List<ToolParameter> = emptyList()
    override val requiredPermissions: List<String> = listOf("android.permission.CAMERA")
    override val riskLevel: RiskLevel = RiskLevel.HIGH

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(id, "Kamera aktivləşdirildi, çəkiliş rejimi hazırdır.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Kamera tətbiqi başladıla bilmədi: ${e.message}")
        }
    }
}
