package com.example.jarvis.tools.impl.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/** CREATE_EVENT — insert a calendar event. */
class CreateEventTool : Tool {
    override val id = "CREATE_EVENT"
    override val name = "Təqvim Hadisəsi Yarat"
    override val description = "Təqvimdə yeni hadisə / görüş yaradır."
    override val parameters = listOf(
        ToolParameter("title", "string", true, "Hadisənin başlığı"),
        ToolParameter("description", "string", false, "Hadisə təsviri", ""),
        ToolParameter("location", "string", false, "Məkan", ""),
        ToolParameter("begin_time", "long", false, "Başlama vaxtı (millis)", "")
    )
    override val requiredPermissions = listOf(Manifest.permission.WRITE_CALENDAR)
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val title = params["title"] ?: return ToolResult.failed(id, "Hadisə başlığı qeyd edilməyib.")
        val desc  = params["description"] ?: ""
        val loc   = params["location"] ?: ""
        val start = params["begin_time"]?.toLongOrNull() ?: System.currentTimeMillis()

        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, desc)
                putExtra(CalendarContract.Events.EVENT_LOCATION, loc)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(id, "'$title' hadisəsini təqvimə əlavə etmək üçün təqvim açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Təqvim hadisəsi yaradıla bilmədi: ${e.message}")
        }
    }
}

/** LIST_EVENTS — read upcoming calendar events or open calendar. */
class ListEventsTool : Tool {
    override val id = "LIST_EVENTS"
    override val name = "Təqvim Hadisələri"
    override val description = "Qarşıdakı təqvim hadisələrini göstərir və ya təqvimi açır."
    override val parameters = listOf(
        ToolParameter("limit", "int", false, "Maksimum hadisə sayı", "5")
    )
    override val requiredPermissions = listOf(Manifest.permission.READ_CALENDAR)
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val limit = params["limit"]?.toIntOrNull() ?: 5
        return@withContext try {
            val events = mutableListOf<String>()
            val now = System.currentTimeMillis()
            val uri = CalendarContract.Events.CONTENT_URI
            val projection = arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART
            )
            val selection = "(${CalendarContract.Events.DTSTART} >= ?)"
            val selArgs = arrayOf(now.toString())
            val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

            context.contentResolver.query(uri, projection, selection, selArgs, sortOrder)?.use { cursor ->
                val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                while (cursor.moveToNext() && events.size < limit) {
                    val title = cursor.getString(titleIdx) ?: "Adsız"
                    val start = cursor.getLong(startIdx)
                    val dateStr = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(start))
                    events.add("$title ($dateStr)")
                }
            }

            if (events.isEmpty()) {
                ToolResult.success(id, "Yaxın zaman üçün təqvimdə heç bir hadisə tapılmadı.")
            } else {
                ToolResult.success(
                    id,
                    "Qarşıdakı hadisələr:\n" + events.joinToString("\n"),
                    mapOf("events" to events)
                )
            }
        } catch (e: SecurityException) {
            // If permission denied, try opening calendar view
            try {
                val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                ContentUris.appendId(builder, System.currentTimeMillis())
                val intent = Intent(Intent.ACTION_VIEW).setData(builder.build()).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                ToolResult.success(id, "Təqvim açıldı.")
            } catch (e2: Exception) {
                ToolResult.permissionRequired(id, listOf(Manifest.permission.READ_CALENDAR), "Təqvim oxuma icazəsi tələb olunur.")
            }
        } catch (e: Exception) {
            ToolResult.failed(id, "Təqvim hadisələri oxuna bilmədi: ${e.message}")
        }
    }
}

/** DELETE_EVENT — open calendar to manage and delete events. */
class DeleteEventTool : Tool {
    override val id = "DELETE_EVENT"
    override val name = "Təqvim Hadisəsini Sil"
    override val description = "Təqvim hadisəsini silmək üçün təqvimi açır."
    override val parameters = listOf(
        ToolParameter("title", "string", false, "Hadisənin başlığı", "")
    )
    override val requiredPermissions = listOf(Manifest.permission.WRITE_CALENDAR)
    override val riskLevel = RiskLevel.HIGH

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            ContentUris.appendId(builder, System.currentTimeMillis())
            val intent = Intent(Intent.ACTION_VIEW).setData(builder.build()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(id, "Hadisəni silmək üçün təqvim açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Təqvim açıla bilmədi: ${e.message}")
        }
    }
}
