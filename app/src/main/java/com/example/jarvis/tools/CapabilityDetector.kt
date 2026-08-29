package com.example.jarvis.tools

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.jarvis.domain.model.CapabilityInfo
import com.example.jarvis.domain.model.CapabilityStatus
import com.example.jarvis.services.JarvisAccessibilityService
import com.example.jarvis.services.JarvisNotificationListenerService

/**
 * Pre-flight capability detector.
 *
 * Call [detect] before executing a tool to understand whether it can actually work
 * on the current device/state. Returns [CapabilityInfo] with a [CapabilityStatus]
 * and a human-readable reason in Azerbaijani.
 */
class CapabilityDetector(private val context: Context) {

    companion object {
        // Special access type strings (used in ToolResult.specialAccessRequired)
        const val ACCESSIBILITY_SERVICE = "ACCESSIBILITY_SERVICE"
        const val NOTIFICATION_LISTENER = "NOTIFICATION_LISTENER"
        const val DEVICE_ADMIN = "DEVICE_ADMIN"
    }

    /** Returns [CapabilityInfo] for [toolId] based on its permission/API requirements. */
    fun detect(toolId: String, requiredPermissions: List<String>, minApiLevel: Int = 0): CapabilityInfo {
        // 1. API version check
        if (Build.VERSION.SDK_INT < minApiLevel) {
            return CapabilityInfo(
                toolId = toolId,
                status = CapabilityStatus.UNSUPPORTED,
                reason = "Bu funksiya Android ${minApiLevel / 10000 + 1}+ tələb edir. Cihazınız uyğun deyil.",
                minApiLevel = minApiLevel
            )
        }

        // 2. Runtime permission check
        val missing = requiredPermissions.filter { perm ->
            try {
                ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
            } catch (_: Throwable) {
                try {
                    context.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED
                } catch (_: Throwable) {
                    true
                }
            }
        }
        if (missing.isNotEmpty()) {
            return CapabilityInfo(
                toolId = toolId,
                status = CapabilityStatus.PERMISSION_REQUIRED,
                reason = "Əməliyyat üçün tələb olunan icazələr verilməyib: ${missing.joinToString()}",
                missingPermissions = missing
            )
        }

        return CapabilityInfo(
            toolId = toolId,
            status = CapabilityStatus.SUPPORTED,
            reason = "Hazırdır."
        )
    }

    /** Check if Notification Listener service is enabled. */
    fun isNotificationListenerEnabled(): Boolean =
        JarvisNotificationListenerService.isEnabled(context)

    /** Check if Accessibility Service is enabled. */
    fun isAccessibilityServiceEnabled(): Boolean =
        JarvisAccessibilityService.isEnabled(context)

    /** Detect Notification Listener capability for a tool. */
    fun detectNotificationListener(toolId: String): CapabilityInfo {
        if (!isNotificationListenerEnabled()) {
            return CapabilityInfo(
                toolId = toolId,
                status = CapabilityStatus.SPECIAL_ACCESS_REQUIRED,
                reason = "Bildiriş oxumaq üçün Bildiris Dinləyici xidmətini aktivləşdirməlisiniz. " +
                        "Parametrlər → Xüsusi tətbiq girişi → Bildiriş girişi → JARVIS aktivləşdir."
            )
        }
        return CapabilityInfo(toolId = toolId, status = CapabilityStatus.SUPPORTED, reason = "Hazırdır.")
    }

    /** Detect Accessibility Service capability for a tool. */
    fun detectAccessibilityService(toolId: String): CapabilityInfo {
        if (!isAccessibilityServiceEnabled()) {
            return CapabilityInfo(
                toolId = toolId,
                status = CapabilityStatus.SPECIAL_ACCESS_REQUIRED,
                reason = "Bu əməliyyat JARVIS Əlçatımlılıq Xidmətini tələb edir. " +
                        "Parametrlər → Əlçatımlılıq → JARVIS → Aktivləşdir."
            )
        }
        return CapabilityInfo(toolId = toolId, status = CapabilityStatus.SUPPORTED, reason = "Hazırdır.")
    }

    /** Wi-Fi programmatic toggle is NOT possible on Android 10+ without root. */
    fun detectWifiToggle(toolId: String): CapabilityInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CapabilityInfo(
                toolId = toolId,
                status = CapabilityStatus.UNSUPPORTED,
                reason = "Android 10 və sonrasında Wi-Fi-nin proqramla söndürülməsi mümkün deyil. " +
                        "Wi-Fi parametrləri açılacaq."
            )
        } else {
            CapabilityInfo(toolId = toolId, status = CapabilityStatus.SUPPORTED, reason = "Hazırdır.")
        }
    }

    /** Bluetooth programmatic toggle requires BLUETOOTH_CONNECT on Android 12+. */
    fun detectBluetoothToggle(toolId: String): CapabilityInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val perm = android.Manifest.permission.BLUETOOTH_CONNECT
            if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                CapabilityInfo(
                    toolId = toolId,
                    status = CapabilityStatus.PERMISSION_REQUIRED,
                    reason = "Android 12+ üçün BLUETOOTH_CONNECT icazəsi tələb olunur.",
                    missingPermissions = listOf(perm)
                )
            } else {
                CapabilityInfo(toolId = toolId, status = CapabilityStatus.SUPPORTED, reason = "Hazırdır.")
            }
        } else {
            CapabilityInfo(toolId = toolId, status = CapabilityStatus.SUPPORTED, reason = "Hazırdır.")
        }
    }
}
