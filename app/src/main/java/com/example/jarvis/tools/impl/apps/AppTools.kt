package com.example.jarvis.tools.impl.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

/** LIST_APPS — list all installed user applications. */
class ListAppsTool : Tool {
    override val id = "LIST_APPS"
    override val name = "Tətbiqlərin Siyahısı"
    override val description = "Cihazda quraşdırılmış bütün istifadəçi tətbiqlərini siyahıya alır."
    override val parameters = listOf(
        ToolParameter("filter", "string", false, "Tətbiq adına görə filtir (boş = hamısı)", "")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val pm = context.packageManager
            val filter = params["filter"]?.lowercase() ?: ""
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(intent, 0)
                .map { it.loadLabel(pm).toString() }
                .filter { filter.isEmpty() || it.lowercase().contains(filter) }
                .sorted()

            if (apps.isEmpty()) {
                ToolResult.success(id, "Heç bir tətbiq tapılmadı.", mapOf("apps" to apps))
            } else {
                val preview = apps.take(10).joinToString(", ")
                val msg = if (apps.size > 10)
                    "Quraşdırılmış tətbiqlər (${apps.size} ədəd): $preview … və daha ${ apps.size - 10} tətbiq."
                else
                    "Quraşdırılmış tətbiqlər (${apps.size} ədəd): $preview."
                ToolResult.success(id, msg, mapOf("apps" to apps, "count" to apps.size))
            }
        } catch (e: Exception) {
            ToolResult.failed(id, "Tətbiq siyahısı alına bilmədi: ${e.message}")
        }
    }
}

/** APP_INFO — show info about a specific installed app. */
class AppInfoTool : Tool {
    override val id = "APP_INFO"
    override val name = "Tətbiq Məlumatı"
    override val description = "Müəyyən bir tətbiqin versiyası, paket adı və ölçüsü haqqında məlumat verir."
    override val parameters = listOf(
        ToolParameter("app_name", "string", true, "Tətbiqin adı")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val appName = params["app_name"] ?: return ToolResult.failed(id, "Tətbiq adı göstərilməyib.")
        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = pm.queryIntentActivities(intent, 0)
                .firstOrNull { it.loadLabel(pm).toString().lowercase().contains(appName.lowercase()) }
                ?: return ToolResult.failed(id, "'$appName' adlı tətbiq tapılmadı.")

            val pkg = resolved.activityInfo.packageName
            val info = pm.getPackageInfo(pkg, 0)
            val versionName = info.versionName ?: "bilinmir"
            val label = resolved.loadLabel(pm).toString()

            ToolResult.success(
                id,
                "$label: versiya $versionName, paket: $pkg.",
                mapOf("label" to label, "package" to pkg, "version" to versionName)
            )
        } catch (e: Exception) {
            ToolResult.failed(id, "Tətbiq məlumatı alına bilmədi: ${e.message}")
        }
    }
}

/** OPEN_APP_SETTINGS — open the system settings page for a specific app. */
class OpenAppSettingsTool : Tool {
    override val id = "OPEN_APP_SETTINGS"
    override val name = "Tətbiq Parametrləri"
    override val description = "Seçilmiş tətbiqin sistem parametrləri səhifəsini açır."
    override val parameters = listOf(
        ToolParameter("app_name", "string", true, "Tətbiqin adı")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val appName = params["app_name"] ?: return ToolResult.failed(id, "Tətbiq adı göstərilməyib.")
        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = pm.queryIntentActivities(intent, 0)
                .firstOrNull { it.loadLabel(pm).toString().lowercase().contains(appName.lowercase()) }
                ?: return ToolResult.failed(id, "'$appName' adlı tətbiq tapılmadı.")

            val pkg = resolved.activityInfo.packageName
            val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", pkg, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(settingsIntent)
            ToolResult.success(id, "${resolved.loadLabel(pm)} tətbiqinin parametrləri açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Tətbiq parametrləri açıla bilmədi: ${e.message}")
        }
    }
}

/** REQUEST_APP_PERMISSION — request permission or open app permission settings. */
class RequestAppPermissionTool : Tool {
    override val id = "REQUEST_APP_PERMISSION"
    override val name = "Tətbiq İcazəsi Tələbi"
    override val description = "Tətbiq üçün tələb olunan icazələr səhifəsini açır."
    override val parameters = listOf(
        ToolParameter("app_name", "string", false, "Tətbiqin adı (boş olduqda JARVIS)", "")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val appName = params["app_name"]?.trim() ?: ""
        return try {
            val targetPackage = if (appName.isNotEmpty() && !appName.contains("jarvis", ignoreCase = true)) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(intent, 0)
                    .firstOrNull { it.loadLabel(pm).toString().lowercase().contains(appName.lowercase()) }
                    ?.activityInfo?.packageName ?: context.packageName
            } else {
                context.packageName
            }

            val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", targetPackage, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(settingsIntent)
            ToolResult.success(id, "İcazələri idarə etmək üçün tənzimləmələr səhifəsi açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "İcazə parametrləri açıla bilmədi: ${e.message}")
        }
    }
}

/** OPEN_PLAY_STORE — open Google Play Store (optionally search for an app). */
class OpenPlayStoreTool : Tool {
    override val id = "OPEN_PLAY_STORE"
    override val name = "Play Store"
    override val description = "Google Play Store-u açır. İstəyə bağlı olaraq tətbiq axtarışı edir."
    override val parameters = listOf(
        ToolParameter("query", "string", false, "Axtarış sorğusu (boş = sadəcə aç)", "")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val query = params["query"]?.trim() ?: ""
            val intent = if (query.isNotEmpty()) {
                Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(query)}"))
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse("market://"))
            }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

            context.startActivity(intent)
            val msg = if (query.isNotEmpty()) "Play Store-da '$query' axtarışı açıldı."
                      else "Google Play Store açıldı."
            ToolResult.success(id, msg)
        } catch (e: Exception) {
            // Fallback to browser if Play Store not installed
            try {
                val query = params["query"]?.trim() ?: ""
                val url = if (query.isNotEmpty())
                    "https://play.google.com/store/search?q=${Uri.encode(query)}"
                else
                    "https://play.google.com/store"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
                ToolResult.success(id, "Play Store brauzer vasitəsilə açıldı.")
            } catch (e2: Exception) {
                ToolResult.failed(id, "Play Store açıla bilmədi: ${e2.message}")
            }
        }
    }
}
