package com.example.jarvis.tools.impl.reminder

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

/** LIST_REMINDERS — show reminder/alarm list. */
class ListRemindersTool : Tool {
    override val id = "LIST_REMINDERS"
    override val name = "Xatırlatmalar Siyahısı"
    override val description = "Mövcud xatırlatma və siqnalların siyahısını açır."
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
            ToolResult.success(id, "Xatırlatmalar və siqnallar siyahısı açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Xatırlatmalar siyahısı açıla bilmədi: ${e.message}")
        }
    }
}

/** DELETE_REMINDER — dismiss/delete a reminder or open alarms screen. */
class DeleteReminderTool : Tool {
    override val id = "DELETE_REMINDER"
    override val name = "Xatırlatmanı Sil"
    override val description = "Aktiv xatırlatmanı ləğv edir və ya xatırlatmaları idarəetmə səhifəsini açır."
    override val parameters = listOf(
        ToolParameter("title", "string", false, "Silinəcək xatırlatmanın adı", "")
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
            ToolResult.success(id, "Xatırlatma ləğv edildi.")
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                ToolResult.success(id, "Xatırlatmalar açıldı, ləğv etmək istədiyinizi seçin.")
            } catch (e2: Exception) {
                ToolResult.failed(id, "Xatırlatma ləğv edilə bilmədi: ${e2.message}")
            }
        }
    }
}
