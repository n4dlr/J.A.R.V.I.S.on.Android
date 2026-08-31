package com.example.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import com.example.jarvis.automation.apps.AppResolver
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

class OpenAppTool(
    private val appResolver: AppResolver? = null
) : Tool {
    override val id: String = "OPEN_APP"
    override val name: String = "Tətbiqi Başlat"
    override val description: String = "İstənilən quraşdırılmış tətbiqi (YouTube, WhatsApp, Telegram, Kamera və s.) açır."
    override val parameters: List<ToolParameter> = listOf(
        ToolParameter("app_name", "String", true, "Açılacaq tətbiqin adı və ya açar sözü")
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean {
        return !params["app_name"].isNullOrBlank()
    }

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val rawInput = params["app_name"]?.trim() ?: return ToolResult.failed(id, "Tətbiq adı qeyd olunmayıb.")
        val resolver = appResolver ?: AppResolver(context)
        val resolution = resolver.resolveApp(rawInput)

        if (resolution.matched && resolution.packageName != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(resolution.packageName)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return ToolResult.success(
                    id,
                    "'${resolution.appLabel ?: rawInput}' tətbiqi başladıldı.",
                    mapOf("package" to resolution.packageName, "resolvedLabel" to resolution.appLabel)
                )
            }
        }

        return ToolResult.failed(id, "'$rawInput' adlı tətbiq cihazda tapılmadı.")
    }
}
