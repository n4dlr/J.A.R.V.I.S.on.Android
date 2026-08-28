package com.example.jarvis.tools.impl

import android.content.Context
import android.media.AudioManager
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

class SetVolumeTool : Tool {
    override val id: String = "SET_VOLUME"
    override val name: String = "Səs Səviyyəsi"
    override val description: String = "Media, zəng və ya bildiriş səsini tənzimləyir, artırır və ya azaldır."
    override val parameters: List<ToolParameter> = listOf(
        ToolParameter("action", "String", false, "UP, DOWN, MUTE, MAX, SET", "UP"),
        ToolParameter("level", "Int", false, "Səs faizi və ya dərəcəsi", "50")
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val action = params["action"]?.uppercase() ?: "UP"
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            val newVolume = when (action) {
                "UP", "ARTIR", "COXALT" -> (currentVolume + (maxVolume / 5).coerceAtLeast(1)).coerceAtMost(maxVolume)
                "DOWN", "AZALT" -> (currentVolume - (maxVolume / 5).coerceAtLeast(1)).coerceAtLeast(0)
                "MUTE", "SUS", "SESSIZ" -> 0
                "MAX", "MAKSIMUM" -> maxVolume
                "SET" -> {
                    val target = params["level"]?.toIntOrNull() ?: currentVolume
                    target.coerceIn(0, maxVolume)
                }
                else -> currentVolume
            }

            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_SHOW_UI)
            val currentPercent = if (maxVolume > 0) ((newVolume.toDouble() / maxVolume) * 100).toInt() else 0
            val msg = "Media səsi tənzimləndi: %$currentPercent (Səviyyə $newVolume / $maxVolume)."

            ToolResult.success(
                toolId = id,
                message = msg,
                data = mapOf(
                    "volume" to newVolume,
                    "maxVolume" to maxVolume,
                    "percent" to currentPercent
                )
            )
        } catch (e: Exception) {
            ToolResult.failed(id, "Səs dəyişdirilə bilmədi: ${e.message}")
        }
    }
}
