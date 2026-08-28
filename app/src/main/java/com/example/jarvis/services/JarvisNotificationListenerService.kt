package com.example.jarvis.services

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils

/**
 * JARVIS Notification Listener Service.
 *
 * Provides read access to status-bar notifications without root.
 * Must be enabled by the user in Settings → Special app access → Notification access.
 *
 * Tools access the live notification list via [StateHolder.notifications].
 */
class JarvisNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        StateHolder.connected = true
        refreshNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        StateHolder.connected = false
        StateHolder.notifications = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        refreshNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshNotifications()
    }

    private fun refreshNotifications() {
        try {
            val active = activeNotifications?.toList() ?: emptyList()
            StateHolder.notifications = active.map { sbn -> sbn.toSimple() }
        } catch (_: Exception) {
            StateHolder.notifications = emptyList()
        }
    }

    /** Cancel a notification by its key. Returns true if successful. */
    fun cancelNotificationByKey(key: String): Boolean {
        return try {
            cancelNotification(key)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ─── Companion ──────────────────────────────────────────────────────────

    companion object {
        /** Check if our Notification Listener is enabled by the user. */
        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val cn = ComponentName(context, JarvisNotificationListenerService::class.java)
            return flat.contains(cn.flattenToString())
        }
    }

    /** Singleton holder — accessible from any Tool without binding. */
    object StateHolder {
        var connected: Boolean = false
        var notifications: List<SimpleNotification> = emptyList()
    }
}

/** Lightweight, tool-friendly representation of a status-bar notification. */
data class SimpleNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val isOngoing: Boolean
)

private fun StatusBarNotification.toSimple(): SimpleNotification {
    val extras = notification?.extras
    val title = extras?.getCharSequence("android.title")?.toString() ?: ""
    val text  = extras?.getCharSequence("android.text")?.toString()  ?: ""
    val appLabel = packageName

    return SimpleNotification(
        key = key ?: "",
        packageName = packageName ?: "",
        appLabel = appLabel,
        title = title,
        text = text,
        postedAt = postTime,
        isOngoing = isOngoing
    )
}
