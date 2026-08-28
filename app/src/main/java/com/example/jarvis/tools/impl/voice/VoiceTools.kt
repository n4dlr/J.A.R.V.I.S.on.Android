package com.example.jarvis.tools.impl.voice

import android.content.Context
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool
import com.example.jarvis.voice.TextToSpeechHelper

/** SPEAK — speak out a provided text via TTS. */
class SpeakTool(private val ttsHelper: TextToSpeechHelper? = null) : Tool {
    override val id = "SPEAK"
    override val name = "Mətni Səsləndir"
    override val description = "Daxil edilən mətni səsli oxuyur."
    override val parameters = listOf(
        ToolParameter("text", "string", true, "Səsləndiriləcək mətn")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val text = params["text"] ?: return ToolResult.failed(id, "Səsləndiriləcək mətn göstərilməyib.")
        return try {
            ttsHelper?.speak(text, force = true)
            ToolResult.success(id, "Səsləndirildi: $text", mapOf("spokenText" to text))
        } catch (e: Exception) {
            ToolResult.failed(id, "Səsləndirmə xətası: ${e.message}")
        }
    }
}

/** START_LISTENING — trigger speech recognition. */
class StartListeningTool : Tool {
    override val id = "START_LISTENING"
    override val name = "Dinləməni Başlat"
    override val description = "Mikrofon vasitəsilə səs tanıma dinləməsini aktivləşdirir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = listOf(android.Manifest.permission.RECORD_AUDIO)
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return ToolResult.success(id, "Səsli dinləmə rejimi aktivdir. Danışa bilərsiniz.")
    }
}

/** STOP_LISTENING — stop speech recognition. */
class StopListeningTool : Tool {
    override val id = "STOP_LISTENING"
    override val name = "Dinləməni Dayandır"
    override val description = "Mikrofon səs tanıma dinləməsini dayandırır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return ToolResult.success(id, "Dinləmə dayandırıldı.")
    }
}
