package com.example.jarvis.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class AppPermission(
    val manifestPermission: String?,
    val titleAz: String,
    val descriptionAz: String,
    val isDangerous: Boolean
) {
    MICROPHONE(
        Manifest.permission.RECORD_AUDIO,
        "Mikrofon İcazəsi",
        "Səsli əmrləri dinləmək və tanımaq üçün tələb olunur.",
        true
    ),
    CAMERA(
        Manifest.permission.CAMERA,
        "Kamera və Fənər İcazəsi",
        "Şəkil çəkmək və fənəri idarə etmək üçün tələb olunur.",
        true
    ),
    INTERNET(
        Manifest.permission.INTERNET,
        "İnternet Girişi",
        "Gemini bulud modeli və onlayn axtarış üçün istifadə edilir.",
        false
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

    fun isPermissionGranted(permission: AppPermission): Boolean {
        val perm = permission.manifestPermission ?: return true
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    fun isManifestPermissionGranted(manifestPermission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, manifestPermission) == PackageManager.PERMISSION_GRANTED
    }

    fun getMissingPermissions(requiredPermissions: List<String>): List<String> {
        return requiredPermissions.filter { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun getAllPermissionStatuses(): List<PermissionStatusInfo> {
        return AppPermission.entries.map { perm ->
            PermissionStatusInfo(
                permission = perm,
                isGranted = isPermissionGranted(perm)
            )
        }
    }

    fun getAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
