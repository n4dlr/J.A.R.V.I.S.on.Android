package com.example.jarvis.tools.impl.alarm

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

/** CREATE_ALARM — create an alarm using Android AlarmClock API. */
class CreateAlarmTool : Tool {
    override val id = "CREATE_ALARM"
    override val name = "Zəngli Saat Qur"
    override val description = "Göstərilən saata zəngli saat (alarm) qurur."
    override val parameters = listOf(
        ToolParameter("hour", "int", true, "Saat (0-23)", "7"),
        ToolParameter("minutes", "int", false, "Dəqiqə (0-59)", "0"),
        ToolParameter("message", "string", false, "Siqnal mətni", "JARVIS Alarm")
    )
    override val requiredPermissions = listOf("com.android.alarm.permission.SET_ALARM")
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val hour = params["hour"]?.toIntOrNull() ?: return ToolResult.failed(id, "Saat düzgün qeyd edilməyib.")
        val minutes = params["minutes"]?.toIntOrNull() ?: 0
        val message = params["message"] ?: "JARVIS Alarm"

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            val timeStr = "%02d:%02d".format(hour, minutes)
            ToolResult.success(
                id,
                "Zəngli saat saat $timeStr üçün quruldu ($message).",
                mapOf("hour" to hour, "minutes" to minutes, "message" to message)
            )
        } catch (e: Exception) {
            ToolResult.failed(id, "Zəngli saat qurula bilmədi: ${e.message}")
        }
    }
}

/** LIST_ALARMS — open system alarms screen. */
class ListAlarmsTool : Tool {
    override val id = "LIST_ALARMS"
    override val name = "Zəngli Saatları Göstər"
    override val description = "Qurulmuş zəngli saatlar siyahısını açır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(id, "Zəngli saatlar siyahısı açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Zəngli saatlar açıla bilmədi: ${e.message}")
        }
    }
}

/** DELETE_ALARM — dismiss/delete an alarm or open alarms to edit. */
class DeleteAlarmTool : Tool {
    override val id = "DELETE_ALARM"
    override val name = "Zəngli Saatı Sil"
    override val description = "Aktiv zəngli saatı dayandırır və ya silmə ekranını açır."
    override val parameters = listOf(
        ToolParameter("search_mode", "string", false, "Axtarış mətni və ya etiketi", "")
    )
    override val requiredPermissions = listOf("com.android.alarm.permission.SET_ALARM")
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(id, "Zəngli saat ləğv edildi.")
        } catch (e: Exception) {
            // Fallback to showing alarms
            try {
                context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                ToolResult.success(id, "Zəngli saatlar açıldı, silmək istədiyinizi seçin.")
            } catch (e2: Exception) {
                ToolResult.failed(id, "Zəngli saat ləğv edilə bilmədi: ${e2.message}")
            }
        }
    }
}
