package com.example.jarvis.tools.impl.sms

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

/** OPEN_MESSAGES — open the default SMS/messaging app. */
class OpenMessagesTool : Tool {
    override val id = "OPEN_MESSAGES"
    override val name = "Mesajları Aç"
    override val description = "Cihazın mesajlaşma tətbiqini açır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(id, "Mesajlaşma tətbiqi açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Mesajlaşma tətbiqi açıla bilmədi: ${e.message}")
        }
    }
}

/** COMPOSE_SMS — open SMS composer with pre-filled number and optional body. */
class ComposeSMSTool : Tool {
    override val id = "COMPOSE_SMS"
    override val name = "SMS Yaz"
    override val description = "SMS yazma ekranını açır. İstifadəçi göndər düyməsini özü basmalıdır."
    override val parameters = listOf(
        ToolParameter("number", "string", true, "Alıcı telefon nömrəsi"),
        ToolParameter("body", "string", false, "Mesajın mətni", "")
    )
    override val requiredPermissions = emptyList<String>() // smsTo intent doesn't need SEND_SMS
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val number = params["number"] ?: return ToolResult.failed(id, "Telefon nömrəsi göstərilməyib.")
        val body   = params["body"] ?: ""
        return try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")).apply {
                putExtra("sms_body", body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(id, "$number nömrəsinə SMS yazma ekranı açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "SMS yazma ekranı açıla bilmədi: ${e.message}")
        }
    }
}
