package com.example.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

class OpenAppTool : Tool {
    override val id: String = "OPEN_APP"
    override val name: String = "Tətbiqi Başlat"
    override val description: String = "İstənilən quraşdırılmış tətbiqi (YouTube, WhatsApp, Telegram, Kamera və s.) açır."
    override val parameters: List<ToolParameter> = listOf(
        ToolParameter("app_name", "String", true, "Açılacaq tətbiqin adı və ya açar sözü")
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM

    private val commonPackageAliases = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "vatsap" to "com.whatsapp",
        "telegram" to "org.telegram.messenger",
        "chrome" to "com.android.chrome",
        "google" to "com.google.android.googlequicksearchbox",
        "maps" to "com.google.android.apps.maps",
        "xerite" to "com.google.android.apps.maps",
        "camera" to "com.google.android.GoogleCamera",
        "kamera" to "com.google.android.GoogleCamera",
        "calculator" to "com.google.android.calculator",
        "kalkulyator" to "com.google.android.calculator",
        "clock" to "com.google.android.deskclock",
        "saat" to "com.google.android.deskclock",
        "settings" to "com.android.settings",
        "tenzimlemeler" to "com.android.settings"
    )

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean {
        return !params["app_name"].isNullOrBlank()
    }

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val appQuery = params["app_name"]?.trim()?.lowercase() ?: return ToolResult.failed(id, "Tətbiq adı qeyd olunmayıb.")
        val pm = context.packageManager

        // 1. Try known alias
        val aliasPkg = commonPackageAliases[appQuery]
        if (aliasPkg != null) {
            val intent = pm.getLaunchIntentForPackage(aliasPkg)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return ToolResult.success(id, "'$appQuery' tətbiqi uğurla başladıldı.", mapOf("package" to aliasPkg))
            }
        }

        // 2. Scan installed launcher apps safely
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(mainIntent, 0)
            val matched = apps.firstOrNull { resolveInfo ->
                val label = resolveInfo.loadLabel(pm).toString().lowercase()
                val pkg = resolveInfo.activityInfo.packageName.lowercase()
                label.contains(appQuery) || pkg.contains(appQuery)
            }

            if (matched != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matched.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                    val label = matched.loadLabel(pm).toString()
                    return ToolResult.success(id, "'$label' tətbiqi açıldı.", mapOf("package" to matched.activityInfo.packageName))
                }
            }
        } catch (e: Exception) {
            return ToolResult.failed(id, "Tətbiq axtarılarkən xəta: ${e.message}")
        }

        return ToolResult.failed(id, "'$appQuery' adlı tətbiq cihazda tapılmadı.")
    }
}
