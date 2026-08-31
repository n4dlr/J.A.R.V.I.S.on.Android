package com.example.jarvis.tools.impl.news

import android.content.Context
import android.util.Log
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.HttpURLConnection
import java.net.URL

data class NewsItem(val title: String, val link: String, val pubDate: String)

class GetNewsTool : Tool {

    companion object {
        private const val TAG = "GetNewsTool"

        // RSS feeds — free, no API key required
        private val RSS_FEEDS = mapOf(
            "az" to listOf(
                "https://az.sputniknews.az/export/rss2/archive/index.xml",
                "https://report.az/feed/",
                "https://azertag.az/rss"
            ),
            "en" to listOf(
                "https://feeds.bbci.co.uk/news/rss.xml",
                "https://rss.nytimes.com/services/xml/rss/nyt/World.xml"
            ),
            "ru" to listOf(
                "https://www.rbc.ru/v10/rss/main/",
                "https://lenta.ru/rss/news"
            )
        )
    }

    override val id: String = "GET_NEWS"
    override val name: String = "Son Xəbərlər / Yeni Xəbərlər"
    override val description: String = "Son xəbər başlıqlarını RSS vasitəsilə əldə edir"
    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(name = "count", type = "integer", isRequired = false, description = "Xəbər sayı (1-10)", defaultValue = "5"),
        ToolParameter(name = "lang", type = "string", isRequired = false, description = "Dil: az / en / ru", defaultValue = "az")
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val count = params["count"]?.toIntOrNull()?.coerceIn(1, 10) ?: 5
        val lang = params["lang"]?.lowercase() ?: "az"
        val feeds = RSS_FEEDS[lang] ?: RSS_FEEDS["az"]!!

        val allItems = mutableListOf<NewsItem>()

        for (feedUrl in feeds) {
            try {
                val items = fetchRssFeed(feedUrl, count)
                allItems.addAll(items)
                if (allItems.size >= count) break
            } catch (e: Exception) {
                Log.w(TAG, "RSS feed failed ($feedUrl): ${e.message}")
            }
        }

        if (allItems.isEmpty()) {
            return@withContext ToolResult.failed(id, "Xəbər tapılmadı. İnternet bağlantısını yoxlayın.")
        }

        val limited = allItems.take(count)
        val outputMessage = buildString {
            append("📰 Son ${limited.size} xəbər:\n\n")
            limited.forEachIndexed { i, item ->
                append("${i + 1}. ${item.title}\n")
                if (item.pubDate.isNotBlank()) append("   ${item.pubDate}\n")
                append("\n")
            }
        }

        return@withContext ToolResult.success(
            toolId = id,
            message = outputMessage.trim(),
            data = mapOf(
                "count" to limited.size,
                "headlines" to limited.map { it.title }
            )
        )
    }

    private fun fetchRssFeed(feedUrl: String, maxItems: Int): List<NewsItem> {
        val url = URL(feedUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 12000
        conn.setRequestProperty("User-Agent", "JARVIS-Android/2.0")
        conn.connect()

        val items = mutableListOf<NewsItem>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(conn.inputStream, "UTF-8")

        var inItem = false
        var title = ""
        var link = ""
        var pubDate = ""
        var currentTag = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && items.size < maxItems) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name ?: ""
                    if (currentTag == "item") {
                        inItem = true
                        title = ""; link = ""; pubDate = ""
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inItem) {
                        when (currentTag) {
                            "title" -> title += parser.text ?: ""
                            "link" -> link += parser.text ?: ""
                            "pubDate" -> pubDate += parser.text ?: ""
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && inItem) {
                        if (title.isNotBlank()) {
                            items.add(NewsItem(
                                title = title.trim(),
                                link = link.trim(),
                                pubDate = formatDate(pubDate.trim())
                            ))
                        }
                        inItem = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        conn.disconnect()
        return items
    }

    private fun formatDate(raw: String): String {
        if (raw.isBlank()) return ""
        return try {
            // RSS pubDate format: "Mon, 31 Aug 2026 12:00:00 +0000"
            val parts = raw.split(" ")
            if (parts.size >= 4) "${parts[1]} ${parts[2]} ${parts[3]}" else raw
        } catch (_: Exception) {
            raw
        }
    }
}
