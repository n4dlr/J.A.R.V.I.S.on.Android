package com.example.jarvis.tools.impl

import android.content.Context
import android.hardware.camera2.CameraManager
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

class TorchTool : Tool {
    override val id: String = "TORCH"
    override val name: String = "Fənər / İşıq"
    override val description: String = "Kamera fənərini yandırır və ya söndürür."
    override val parameters: List<ToolParameter> = listOf(
        ToolParameter("state", "String", true, "ON (yandır) və ya OFF (söndür)", "ON")
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM

    companion object {
        var isTorchOn = false
    }

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return ToolResult.unsupported(
                id,
                "Bu cihazda fənər dəstəkləyən kamera tapılmadı."
            )

            val rawState = params["state"]?.uppercase() ?: "TOGGLE"
            val targetState = when (rawState) {
                "ON", "YANDIR", "AC" -> true
                "OFF", "SONDUR", "BAGLA" -> false
                else -> !isTorchOn
            }

            cameraManager.setTorchMode(cameraId, targetState)
            isTorchOn = targetState

            val msg = if (targetState) "Fənər yandırıldı." else "Fənər söndürüldü."
            ToolResult.success(id, msg, mapOf("torchState" to targetState))
        } catch (e: Exception) {
            ToolResult.failed(id, "Fənər rejimi dəyişdirilə bilmədi: ${e.message}")
        }
    }
}
