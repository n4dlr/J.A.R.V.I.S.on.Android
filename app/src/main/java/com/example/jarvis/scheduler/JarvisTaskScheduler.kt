package com.example.jarvis.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.jarvis.domain.model.TaskLifecycleStatus
import com.example.jarvis.memory.MemoryManager
import java.util.Calendar

class JarvisTaskScheduler(
    private val context: Context,
    private val memoryManager: MemoryManager
) {

    /**
     * Schedules a time-based task or workflow trigger without continuously running background CPU.
     */
    suspend fun scheduleTask(
        title: String,
        hour: Int,
        minute: Int,
        isRecurring: Boolean = false
    ): String {
        val task = memoryManager.createTask(title, "Scheduled for $hour:$minute (daily: $isRecurring)")
        memoryManager.updateTaskStatus(task.id, TaskLifecycleStatus.WAITING)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return task.id

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.jarvis.ACTION_EXECUTE_TASK"
            putExtra("EXTRA_TASK_ID", task.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            intent,
            flags
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        return task.id
    }
}
