package com.example.jarvis.scheduler

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.jarvis.tools.impl.news.GetNewsTool
import com.example.jarvis.tools.impl.weather.GetWeatherTool
import com.example.jarvis.voice.TextToSpeechHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * WorkManager worker that runs the morning briefing.
 * Scheduled daily at the user-configured hour (default 08:00).
 *
 * Briefing content:
 *  1. Date & greeting
 *  2. Today's calendar events
 *  3. Weather forecast for the day
 *  4. Top news headlines (AZ / EN)
 */
class MorningBriefingWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    companion object {
        const val WORK_NAME = "jarvis_morning_briefing"
        private const val TAG = "MorningBriefingWorker"
    }

    private val weatherTool = GetWeatherTool()
    private val newsTool = GetNewsTool()

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting morning briefing...")
        return try {
            val briefingText = buildBriefing()
            speakBriefing(briefingText)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Morning briefing failed: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun buildBriefing(): String = coroutineScope {
        val greeting = buildGreeting()
        val calendarInfo = getCalendarEvents()

        // Fetch weather + news in parallel
        val weatherDeferred = async(Dispatchers.IO) {
            try {
                val result = weatherTool.execute(ctx, mapOf("city" to "Baku"))
                if (result.isSuccess) result.outputMessage else ""
            } catch (_: Exception) { "" }
        }
        val newsDeferred = async(Dispatchers.IO) {
            try {
                val result = newsTool.execute(ctx, mapOf("count" to "3", "lang" to "az"))
                if (result.isSuccess) result.outputMessage else ""
            } catch (_: Exception) { "" }
        }

        val (weatherText, newsText) = awaitAll(weatherDeferred, newsDeferred)

        buildString {
            append(greeting)
            append("\n\n")

            if (calendarInfo.isNotBlank()) {
                append("📅 Bugünkü tədbirlər:\n")
                append(calendarInfo)
                append("\n\n")
            }

            if (weatherText.isNotBlank()) {
                append("🌤️ Hava durumu:\n")
                append(weatherText)
                append("\n\n")
            }

            if (newsText.isNotBlank()) {
                append("📰 Son xəbərlər:\n")
                append(newsText)
            }
        }.trim()
    }

    private fun buildGreeting(): String {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Sabahınız xeyir!"
            hour < 18 -> "Günortanız xeyir!"
            else -> "Axşamınız xeyir!"
        }
        val dateStr = SimpleDateFormat("d MMMM yyyy, EEEE", Locale("az")).format(Date())
        return "$greeting Bugün $dateStr."
    }

    private fun getCalendarEvents(): String {
        return try {
            val now = System.currentTimeMillis()
            val endOfDay = now + (24 * 60 * 60 * 1000L)

            val uri = android.provider.CalendarContract.Events.CONTENT_URI
            val projection = arrayOf(
                android.provider.CalendarContract.Events.TITLE,
                android.provider.CalendarContract.Events.DTSTART,
                android.provider.CalendarContract.Events.DTEND
            )
            val selection = "${android.provider.CalendarContract.Events.DTSTART} >= ? AND " +
                    "${android.provider.CalendarContract.Events.DTSTART} <= ? AND " +
                    "${android.provider.CalendarContract.Events.DELETED} != 1"

            val cursor = ctx.contentResolver.query(
                uri, projection, selection,
                arrayOf(now.toString(), endOfDay.toString()),
                android.provider.CalendarContract.Events.DTSTART
            )

            val events = mutableListOf<String>()
            cursor?.use {
                val titleIdx = it.getColumnIndex(android.provider.CalendarContract.Events.TITLE)
                val startIdx = it.getColumnIndex(android.provider.CalendarContract.Events.DTSTART)
                while (it.moveToNext() && events.size < 5) {
                    val title = it.getString(titleIdx) ?: continue
                    val start = it.getLong(startIdx)
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(start))
                    events.add("$timeStr — $title")
                }
            }

            if (events.isEmpty()) "" else events.joinToString("\n")
        } catch (e: SecurityException) {
            Log.w(TAG, "Calendar permission not granted: ${e.message}")
            ""
        } catch (e: Exception) {
            Log.w(TAG, "Calendar fetch failed: ${e.message}")
            ""
        }
    }

    private suspend fun speakBriefing(text: String) = withContext(Dispatchers.Main) {
        // We can't use the ViewModel's TTS directly from a Worker, so we create
        // a standalone TTS instance that speaks the briefing and then shuts down.
        val tts = TextToSpeechHelper(ctx).apply { isEnabled = true }
        // Give TTS engine a moment to initialize before speaking
        kotlinx.coroutines.delay(800)
        tts.speak(text)
        Log.i(TAG, "Briefing spoken successfully.")
    }
}
