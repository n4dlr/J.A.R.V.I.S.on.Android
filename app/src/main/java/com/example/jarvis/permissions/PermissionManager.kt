package com.example.jarvis.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class AppPermission(
    val manifestPermission: String?,
    val titleAz: String,
    val descriptionAz: String,
    val isDangerous: Boolean,
    val minApiLevel: Int = 0
) {
    // ── Audio/Media ──────────────────────────────────────────────────────────
    MICROPHONE(
        Manifest.permission.RECORD_AUDIO,
        "Mikrofon İcazəsi",
        "Səsli əmrləri dinləmək və tanımaq üçün tələb olunur.",
        true
    ),
    // ── Camera ───────────────────────────────────────────────────────────────
    CAMERA(
        Manifest.permission.CAMERA,
        "Kamera İcazəsi",
        "Şəkil çəkmək, video yazmaq və fənəri idarə etmək üçün tələb olunur.",
        true
    ),
    // ── Network ──────────────────────────────────────────────────────────────
    INTERNET(
        Manifest.permission.INTERNET,
        "İnternet Girişi",
        "Gemini bulud modeli və onlayn axtarış üçün istifadə edilir.",
        false
    ),
    // ── Contacts ─────────────────────────────────────────────────────────────
    READ_CONTACTS(
        Manifest.permission.READ_CONTACTS,
        "Kontaktları Oxuma",
        "Kontakt axtarışı və zəng etmək üçün tələb olunur.",
        true
    ),
    WRITE_CONTACTS(
        Manifest.permission.WRITE_CONTACTS,
        "Kontakt Yazma",
        "Yeni kontakt yaratmaq üçün tələb olunur.",
        true
    ),
    // ── Telephony ────────────────────────────────────────────────────────────
    CALL_PHONE(
        Manifest.permission.CALL_PHONE,
        "Zəng Etmə İcazəsi",
        "Birbaşa zəng etmək üçün tələb olunur.",
        true
    ),
    READ_CALL_LOG(
        Manifest.permission.READ_CALL_LOG,
        "Zəng Tarixçəsi",
        "Zəng tarixçəsinə baxmaq üçün tələb olunur.",
        true
    ),
    // ── SMS ──────────────────────────────────────────────────────────────────
    SEND_SMS(
        Manifest.permission.SEND_SMS,
        "SMS Göndərmə",
        "SMS göndərmək üçün tələb olunur.",
        true
    ),
    READ_SMS(
        Manifest.permission.READ_SMS,
        "SMS Oxuma",
        "Mesajları oxumaq üçün tələb olunur.",
        true
    ),
    // ── Location ─────────────────────────────────────────────────────────────
    ACCESS_FINE_LOCATION(
        Manifest.permission.ACCESS_FINE_LOCATION,
        "Dəqiq Məkan İcazəsi",
        "GPS əsaslı dəqiq məkan məlumatı üçün tələb olunur.",
        true
    ),
    ACCESS_COARSE_LOCATION(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        "Təxmini Məkan İcazəsi",
        "Şəbəkə əsaslı məkan məlumatı üçün tələb olunur.",
        true
    ),
    // ── Storage ──────────────────────────────────────────────────────────────
    READ_MEDIA_IMAGES(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE,
        "Media Şəkilləri Oxuma",
        "Şəkil fayllarına giriş üçün tələb olunur.",
        true
    ),
    READ_MEDIA_VIDEO(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_VIDEO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE,
        "Media Video Oxuma",
        "Video fayllarına giriş üçün tələb olunur.",
        true
    ),
    WRITE_EXTERNAL_STORAGE(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        "Yaddaşa Yazma",
        "Fayl kopyalamaq, köçürmək üçün tələb olunur. (Android 9 və aşağı)",
        true,
        minApiLevel = 0
    ),
    // ── Calendar ─────────────────────────────────────────────────────────────
    READ_CALENDAR(
        Manifest.permission.READ_CALENDAR,
        "Təqvim Oxuma",
        "Təqvim hadisələrini oxumaq üçün tələb olunur.",
        true
    ),
    WRITE_CALENDAR(
        Manifest.permission.WRITE_CALENDAR,
        "Təqvimə Yazma",
        "Təqvim hadisəsi yaratmaq/silmək üçün tələb olunur.",
        true
    ),
    // ── Notifications (Android 13+) ───────────────────────────────────────────
    POST_NOTIFICATIONS(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.POST_NOTIFICATIONS
        else null,
        "Bildiriş Göndərmə",
        "JARVIS-in bildirişlər göndərməsi üçün tələb olunur.",
        true,
        minApiLevel = 33
    ),
    // ── Bluetooth ──────────────────────────────────────────────────────────
    BLUETOOTH_CONNECT(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Manifest.permission.BLUETOOTH_CONNECT
        else
            Manifest.permission.BLUETOOTH,
        "Bluetooth Bağlantı",
        "Bluetooth cihazlarına qoşulmaq üçün tələb olunur.",
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        minApiLevel = 31
    );

    companion object {
        fun fromManifest(permission: String): AppPermission? =
            entries.firstOrNull { it.manifestPermission == permission }
    }
}

data class PermissionStatusInfo(
    val permission: AppPermission,
    val isGranted: Boolean,
    val canRequest: Boolean = true
)

class PermissionManager(private val context: Context) {

    /**
     * Check if a declared [AppPermission] is granted.
     * Returns true for non-dangerous permissions or those without a manifest string.
     */
    fun isPermissionGranted(permission: AppPermission): Boolean {
        val perm = permission.manifestPermission ?: return true
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    /** Check raw manifest permission string. */
    fun isManifestPermissionGranted(manifestPermission: String): Boolean =
        ContextCompat.checkSelfPermission(context, manifestPermission) == PackageManager.PERMISSION_GRANTED

    /** Returns only the permissions that are NOT yet granted. */
    fun getMissingPermissions(requiredPermissions: List<String>): List<String> =
        requiredPermissions.filter { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
        }

    /** Snapshot of all declared AppPermission grant states. */
    fun getAllPermissionStatuses(): List<PermissionStatusInfo> =
        AppPermission.entries.map { perm ->
            PermissionStatusInfo(
                permission = perm,
                isGranted = isPermissionGranted(perm)
            )
        }

    /**
     * Human-readable explanation for why [permission] is needed.
     * Use before showing the system permission dialog.
     */
    fun explain(permission: AppPermission): String =
        "${permission.titleAz}: ${permission.descriptionAz}"

    /** Intent that opens the app's system settings page (where user can grant all permissions). */
    fun getAppSettingsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    /** Opens the Notification Listener settings page. */
    fun getNotificationListenerSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    /** Opens the Accessibility settings page. */
    fun getAccessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    /** Opens Location source settings. */
    fun getLocationSettingsIntent(): Intent =
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    /** Opens the exact alarm permission settings on Android 12+. */
    fun getExactAlarmSettingsIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else null

    /** Opens Bluetooth settings. */
    fun getBluetoothSettingsIntent(): Intent =
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    /** Opens Wi-Fi settings. */
    fun getWifiSettingsIntent(): Intent =
        Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
}
