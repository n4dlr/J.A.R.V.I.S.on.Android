package com.example.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

class CreateReminderTool : Tool {
    override val id: String = "CREATE_REMINDER"
    override val name: String = "Xatırlatma və ya Zəngli Saat"
    override val description: String = "Sistem saatında xatırlatma və ya siqnal (alarm) qurur."
    override val parameters: List<ToolParameter> = listOf(
        ToolParameter("title", "String", false, "Xatırlatma başlığı", "JARVIS Xatırlatma"),
        ToolParameter("hour", "Int", false, "Saat (0-23)", "8"),
        ToolParameter("minutes", "Int", false, "Dəqiqə (0-59)", "0")
    )
    override val requiredPermissions: List<String> = listOf("com.android.alarm.permission.SET_ALARM")
    override val riskLevel: RiskLevel = RiskLevel.HIGH

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val title = params["title"] ?: "JARVIS Xatırlatma"
            val hour = params["hour"]?.toIntOrNull() ?: 8
            val minutes = params["minutes"]?.toIntOrNull() ?: 0

            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, title)
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)
            val timeFormatted = String.format(java.util.Locale.US, "%02d:%02d", hour, minutes)
            val msg = "Xatırlatma qeydə alındı: '$title' saat $timeFormatted üçün quruldu."

            ToolResult.success(id, msg, mapOf("title" to title, "time" to timeFormatted))
        } catch (e: Exception) {
            ToolResult.failed(id, "Xatırlatma təyin edilə bilmədi: ${e.message}")
        }
    }
}
