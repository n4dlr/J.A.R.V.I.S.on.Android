package com.example.jarvis.tools.impl.timer

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.util.Log
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

class SetTimerTool : Tool {

    companion object {
        private const val TAG = "SetTimerTool"
    }

    override val id: String = "SET_TIMER"
    override val name: String = "Taymer / Sayğac Qur"
    override val description: String = "Müəyyən müddətlik taymer (sayğac) qurur (məsələn: 10 dəqiqə, 30 saniyə, 1 saat)"
    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(name = "seconds", type = "integer", isRequired = false, description = "Taymerin saniyə ilə müddəti", defaultValue = "0"),
        ToolParameter(name = "minutes", type = "integer", isRequired = false, description = "Taymerin dəqiqə ilə müddəti", defaultValue = "0"),
        ToolParameter(name = "hours", type = "integer", isRequired = false, description = "Taymerin saat ilə müddəti", defaultValue = "0"),
        ToolParameter(name = "label", type = "string", isRequired = false, description = "Taymerin adı və ya məqsədi", defaultValue = "JARVIS Taymer")
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val seconds = params["seconds"]?.toIntOrNull() ?: 0
        val minutes = params["minutes"]?.toIntOrNull() ?: 0
        val hours = params["hours"]?.toIntOrNull() ?: 0
        val label = params["label"] ?: "JARVIS Taymer"

        val totalSeconds = (hours * 3600) + (minutes * 60) + seconds

        if (totalSeconds <= 0) {
            return ToolResult.failed(id, "Taymer müddəti 0-dan böyük olmalıdır.")
        }

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, totalSeconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            val timeDesc = buildString {
                if (hours > 0) append("$hours saat ")
                if (minutes > 0) append("$minutes dəqiqə ")
                if (seconds > 0) append("$seconds saniyə")
            }.trim()

            ToolResult.success(
                toolId = id,
                message = "$timeDesc müddətinə taymer quruldu ($label).",
                data = mapOf("totalSeconds" to totalSeconds, "label" to label)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set timer: ${e.message}", e)
            ToolResult.failed(id, "Taymer başladıla bilmədi: ${e.message}")
        }
    }
}
