package com.example.jarvis.tools.impl.browser

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

/** OPEN_URL — open a specific web URL. */
class OpenUrlTool : Tool {
    override val id = "OPEN_URL"
    override val name = "Veb Səhifə Aç"
    override val description = "Göstərilən veb linki (URL) standart brauzerdə açır."
    override val parameters = listOf(
        ToolParameter("url", "string", true, "Açılacaq veb link (məs: https://google.com)")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        var url = params["url"] ?: return ToolResult.failed(id, "URL qeyd edilməyib.")
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(id, "$url səhifəsi açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Səhifə açıla bilmədi: ${e.message}")
        }
    }
}

/** WEB_SEARCH — perform a web search via Google / default search engine. */
class WebSearchTool : Tool {
    override val id = "WEB_SEARCH"
    override val name = "Veb Axtarış"
    override val description = "İnternetdə axtarış aparır və nəticələri brauzerdə açır."
    override val parameters = listOf(
        ToolParameter("query", "string", true, "Axtarış sorğusu")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val query = params["query"] ?: return ToolResult.failed(id, "Axtarış sorğusu qeyd edilməyib.")
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ToolResult.success(id, "'$query' üzrə axtarış açıldı.")
            } else {
                // Fallback to Google Search URL
                val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
                ToolResult.success(id, "'$query' üzrə Google axtarışı açıldı.")
            }
        } catch (e: Exception) {
            ToolResult.failed(id, "Axtarış açıla bilmədi: ${e.message}")
        }
    }
}

/** OPEN_BROWSER — open the default web browser. */
class OpenBrowserTool : Tool {
    override val id = "OPEN_BROWSER"
    override val name = "Brauzeri Aç"
    override val description = "Standart veb brauzeri açır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(id, "Brauzer açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Brauzer açıla bilmədi: ${e.message}")
        }
    }
}
