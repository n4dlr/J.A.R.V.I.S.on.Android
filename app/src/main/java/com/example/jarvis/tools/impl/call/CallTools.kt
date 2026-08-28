package com.example.jarvis.tools.impl.call

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** DIAL_NUMBER — open dialer with a pre-filled number (user must press call). */
class DialNumberTool : Tool {
    override val id = "DIAL_NUMBER"
    override val name = "Nömrəni Yığ"
    override val description = "Telefon yığıcısını açır. İstifadəçi zəng düyməsini özü basmalıdır."
    override val parameters = listOf(
        ToolParameter("number", "string", true, "Telefon nömrəsi")
    )
    override val requiredPermissions = emptyList<String>() // ACTION_DIAL doesn't need CALL_PHONE
    override val riskLevel = RiskLevel.HIGH

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val number = params["number"] ?: return ToolResult.failed(id, "Telefon nömrəsi göstərilməyib.")
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(id, "$number nömrəsi üçün yığıcı açıldı. Zəng etmək üçün düyməyə basın.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Yığıcı açıla bilmədi: ${e.message}")
        }
    }
}

/** CALL_CONTACT — dial a number directly without UI (requires CALL_PHONE permission). */
class CallContactTool : Tool {
    override val id = "CALL_CONTACT"
    override val name = "Kontaktı Zəng Et"
    override val description = "Kontakta birbaşa zəng edir. CALL_PHONE icazəsi tələb olunur."
    override val parameters = listOf(
        ToolParameter("number", "string", true, "Telefon nömrəsi"),
        ToolParameter("name", "string", false, "Kontakt adı (yalnız mesaj üçün)", "")
    )
    override val requiredPermissions = listOf(Manifest.permission.CALL_PHONE)
    override val riskLevel = RiskLevel.HIGH

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val number = params["number"] ?: return ToolResult.failed(id, "Telefon nömrəsi göstərilməyib.")
        val name   = params["name"] ?: number
        return try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(id, "$name nömrəsinə zəng edilir…")
        } catch (e: SecurityException) {
            ToolResult.permissionRequired(id, listOf(Manifest.permission.CALL_PHONE),
                "Birbaşa zəng etmək üçün Zəng İcazəsi tələb olunur.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Zəng uğursuz oldu: ${e.message}")
        }
    }
}

/** OPEN_CALL_LOG — open recent calls screen. */
class OpenCallLogTool : Tool {
    override val id = "OPEN_CALL_LOG"
    override val name = "Zəng Tarixçəsi"
    override val description = "Son zənglərin tarixçəsini açır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, CallLog.Calls.CONTENT_URI).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(id, "Zəng tarixçəsi açıldı.")
        } catch (e: Exception) {
            // Fallback to dialer
            try {
                context.startActivity(Intent(Intent.ACTION_DIAL).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                ToolResult.success(id, "Telefon yığıcısı açıldı.")
            } catch (e2: Exception) {
                ToolResult.failed(id, "Zəng tarixçəsi açıla bilmədi: ${e2.message}")
            }
        }
    }
}
