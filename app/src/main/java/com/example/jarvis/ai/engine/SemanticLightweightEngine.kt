package com.example.jarvis.ai.engine

import com.example.jarvis.ai.matcher.AppNameExtractor
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.domain.model.CommandAction
import com.example.jarvis.domain.model.CommandIntent
import com.example.jarvis.domain.model.ExecutionStrategy
import com.example.jarvis.domain.model.ExtractedEntity
import com.example.jarvis.domain.model.IntentCategory
import com.example.jarvis.domain.model.IntentConfidence
import com.example.jarvis.domain.model.IntentSource
import java.util.Locale

class SemanticLightweightEngine(
    private val normalizer: AzerbaijaniTextNormalizer = AzerbaijaniTextNormalizer()
) {

    private val mediaKeywords = setOf(
        "mahni", "mahnisini", "mahnini", "musiqi", "musiqisini", "musiqini", "parca", "parcani",
        "sarki", "sarkisini", "song", "music", "track", "video", "videonu", "klip", "klipi"
    )

    private val mediaVerbs = setOf(
        "ac", "acaq", "cal", "oynat", "oyna", "baslat", "tap", "dinle", "iste", "oxut", "play", "listen"
    )

    private val searchVerbs = setOf(
        "axtar", "axtaris", "tap", "haqqinda", "haqda", "search", "find", "google"
    )

    private val messageVerbs = setOf(
        "yaz", "gonder", "yolla", "send", "write", "text"
    )

    /**
     * Parses the raw query into a structured CommandIntent if semantic structures match.
     */
    fun parse(rawQuery: String): CommandIntent? {
        val normalized = normalizer.normalize(rawQuery)
        val cleanNormalized = normalizer.stripFillers(normalized)
        if (cleanNormalized.isBlank()) return null

        val tokens = cleanNormalized.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        // 1. English patterns ("Play X on YouTube", "Open YouTube", "Search X on Google")
        parseEnglishCommand(rawQuery, cleanNormalized)?.let { return it }

        // 2. Media in App Patterns (e.g. "YouTube-da INNA Caliente mahnısını aç", "Spotify-da Caliente çal")
        parseMediaInApp(rawQuery, cleanNormalized, tokens)?.let { return it }

        // 3. Web & App Search Patterns (e.g. "Google-da Android 16 haqqında axtar")
        parseSearchInApp(rawQuery, cleanNormalized, tokens)?.let { return it }

        // 4. Messaging in App Patterns (e.g. "WhatsApp-da Nadirə salam yaz")
        parseMessageInApp(rawQuery, cleanNormalized, tokens)?.let { return it }

        // 5. Clean App Launch (e.g. "YouTube aç", "WhatsApp-ı aç")
        parseAppLaunch(rawQuery, cleanNormalized, tokens)?.let { return it }

        return null
    }

    private fun parseEnglishCommand(rawQuery: String, normalized: String): CommandIntent? {
        // "play [query] on [app]"
        val playMatch = Regex("""(?i)^\s*play\s+(.+?)\s+on\s+(\w+)\s*$""").find(rawQuery)
        if (playMatch != null) {
            val query = playMatch.groupValues[1].trim()
            val rawApp = playMatch.groupValues[2].trim()
            val targetApp = AppNameExtractor.extract(rawApp)
            return CommandIntent(
                intentId = "MEDIA_SEARCH_PLAY",
                category = IntentCategory.MEDIA,
                action = CommandAction.SEARCH_AND_PLAY,
                targetApp = targetApp,
                query = query,
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.HIGH_HEURISTIC,
                source = IntentSource.SEMANTIC_PARSER,
                executionStrategy = ExecutionStrategy.ACCESSIBILITY_AUTOMATION,
                entities = listOf(ExtractedEntity("target_app", targetApp), ExtractedEntity("query", query))
            )
        }

        // "search [query] on [google/chrome]"
        val searchMatch = Regex("""(?i)^\s*search\s+(.+?)\s+on\s+(\w+)\s*$""").find(rawQuery)
        if (searchMatch != null) {
            val query = searchMatch.groupValues[1].trim()
            val rawApp = searchMatch.groupValues[2].trim()
            val targetApp = AppNameExtractor.extract(rawApp)
            return CommandIntent(
                intentId = "WEB_SEARCH",
                category = IntentCategory.BROWSER,
                action = CommandAction.WEB_SEARCH,
                targetApp = targetApp,
                query = query,
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.HIGH_HEURISTIC,
                source = IntentSource.SEMANTIC_PARSER,
                executionStrategy = ExecutionStrategy.DIRECT_API,
                entities = listOf(ExtractedEntity("query", query), ExtractedEntity("target_app", targetApp))
            )
        }

        return null
    }

    private fun parseMediaInApp(rawQuery: String, normalized: String, tokens: List<String>): CommandIntent? {
        // Find if any token is a known media app or if AppNameExtractor can extract app and query
        val extracted = AppNameExtractor.extractAppAndQuery(tokens)
        if (extracted != null) {
            val (appName, queryTokens) = extracted
            val isMediaTarget = appName in setOf("youtube", "spotify", "soundcloud", "youtubemusic", "tidal", "deezer", "apple music")
            val hasMediaIndicator = tokens.any { it in mediaKeywords || it in mediaVerbs }

            if (isMediaTarget || hasMediaIndicator) {
                val cleanQuery = extractCasedQuery(rawQuery, appName, queryTokens)
                return CommandIntent(
                    intentId = "MEDIA_SEARCH_PLAY",
                    category = IntentCategory.MEDIA,
                    action = CommandAction.SEARCH_AND_PLAY,
                    targetApp = appName,
                    query = cleanQuery,
                    rawQuery = rawQuery,
                    normalizedQuery = normalized,
                    confidence = IntentConfidence.EXACT_DETERMINISTIC,
                    source = IntentSource.SEMANTIC_PARSER,
                    executionStrategy = ExecutionStrategy.ACCESSIBILITY_AUTOMATION,
                    parameters = mapOf("target_app" to appName, "query" to cleanQuery),
                    entities = listOf(
                        ExtractedEntity("target_app", appName),
                        ExtractedEntity("query", cleanQuery)
                    )
                )
            }
        }

        // Check for "[Query] [App]-da [çal|oynat|aç]"
        for (i in tokens.indices) {
            val token = tokens[i]
            val canonicalApp = AppNameExtractor.extract(token)
            if (canonicalApp in setOf("youtube", "spotify", "soundcloud", "apple music") && i > 0) {
                val queryPart = tokens.subList(0, i).filter { it !in mediaKeywords }.joinToString(" ")
                if (queryPart.isNotBlank()) {
                    val cleanQuery = extractCasedQuery(rawQuery, canonicalApp, queryPart)
                    return CommandIntent(
                        intentId = "MEDIA_SEARCH_PLAY",
                        category = IntentCategory.MEDIA,
                        action = CommandAction.SEARCH_AND_PLAY,
                        targetApp = canonicalApp,
                        query = cleanQuery,
                        rawQuery = rawQuery,
                        normalizedQuery = normalized,
                        confidence = IntentConfidence.HIGH_HEURISTIC,
                        source = IntentSource.SEMANTIC_PARSER,
                        executionStrategy = ExecutionStrategy.ACCESSIBILITY_AUTOMATION,
                        parameters = mapOf("target_app" to canonicalApp, "query" to cleanQuery),
                        entities = listOf(
                            ExtractedEntity("target_app", canonicalApp),
                            ExtractedEntity("query", cleanQuery)
                        )
                    )
                }
            }
        }

        return null
    }

    private fun parseSearchInApp(rawQuery: String, normalized: String, tokens: List<String>): CommandIntent? {
        val browsers = setOf("chrome", "xrom", "google", "browser", "brauzer", "firefox")
        for (i in tokens.indices) {
            val token = tokens[i]
            val canonicalApp = AppNameExtractor.extract(token)
            if (canonicalApp in browsers || tokens.any { it in searchVerbs }) {
                val queryTokens = tokens.filterIndexed { idx, t ->
                    idx != i && t !in searchVerbs && t !in setOf("da", "de", "nda", "nde", "haqqinda", "haqda", "uzre", "gore")
                }
                if (queryTokens.isNotEmpty()) {
                    val query = queryTokens.joinToString(" ")
                    val cleanQuery = extractCasedQuery(rawQuery, canonicalApp, query)
                    val targetApp = if (canonicalApp in browsers) canonicalApp else "chrome"
                    return CommandIntent(
                        intentId = "WEB_SEARCH",
                        category = IntentCategory.BROWSER,
                        action = CommandAction.WEB_SEARCH,
                        targetApp = targetApp,
                        query = cleanQuery,
                        rawQuery = rawQuery,
                        normalizedQuery = normalized,
                        confidence = IntentConfidence.EXACT_DETERMINISTIC,
                        source = IntentSource.SEMANTIC_PARSER,
                        executionStrategy = ExecutionStrategy.DIRECT_API,
                        parameters = mapOf("query" to cleanQuery, "target_app" to targetApp),
                        entities = listOf(
                            ExtractedEntity("query", cleanQuery),
                            ExtractedEntity("target_app", targetApp)
                        )
                    )
                }
            }
        }
        return null
    }

    private fun parseMessageInApp(rawQuery: String, normalized: String, tokens: List<String>): CommandIntent? {
        val messagingApps = setOf("whatsapp", "telegram", "vatsap", "telqram", "signal", "viber")
        for (i in tokens.indices) {
            val token = tokens[i]
            val canonicalApp = AppNameExtractor.extract(token)
            if (canonicalApp in messagingApps && tokens.any { it in messageVerbs || it == "salam" }) {
                // e.g. "WhatsApp-da Nadirə salam yaz"
                val otherTokens = tokens.filterIndexed { idx, t -> idx != i && t !in setOf("da", "de", "nda", "nde") }
                if (otherTokens.isNotEmpty()) {
                    val contactCandidate = otherTokens.firstOrNull() ?: ""
                    val messageCandidate = otherTokens.drop(1).filter { it !in messageVerbs }.joinToString(" ")
                    return CommandIntent(
                        intentId = "SEND_MESSAGE_IN_APP",
                        category = IntentCategory.COMMUNICATION,
                        action = CommandAction.SEND_MESSAGE,
                        targetApp = canonicalApp,
                        targetEntity = contactCandidate,
                        query = messageCandidate.ifEmpty { "salam" },
                        rawQuery = rawQuery,
                        normalizedQuery = normalized,
                        confidence = IntentConfidence.HIGH_HEURISTIC,
                        source = IntentSource.SEMANTIC_PARSER,
                        executionStrategy = ExecutionStrategy.ACCESSIBILITY_AUTOMATION,
                        parameters = mapOf(
                            "target_app" to canonicalApp,
                            "contact" to contactCandidate,
                            "message" to (messageCandidate.ifEmpty { "salam" })
                        ),
                        entities = listOf(
                            ExtractedEntity("target_app", canonicalApp),
                            ExtractedEntity("contact", contactCandidate)
                        )
                    )
                }
            }
        }
        return null
    }

    private fun parseAppLaunch(rawQuery: String, normalized: String, tokens: List<String>): CommandIntent? {
        // App launch: "[App]-u aç" e.g. "YouTube-u aç", "WhatsApp aç", "Telegramı başlat"
        val verbs = setOf("ac", "baslat", "ise", "sal", "tetbiqini", "tetbiqi", "open", "launch", "ise sal")
        val caseSuffixes = setOf("u", "i", "a", "e", "da", "de", "nda", "nde", "nu", "ni", "ya", "ye", "ni", "nu", "nu")
        if (tokens.any { it in verbs || tokens.size == 1 }) {
            val nonVerbTokens = tokens.filter { it !in verbs && it !in caseSuffixes }
            if (nonVerbTokens.size in 1..2) {
                val rawApp = nonVerbTokens.joinToString(" ")
                val canonicalApp = AppNameExtractor.extract(nonVerbTokens.first())
                val blacklist = setOf("fener", "tenzimleme", "kamera", "video", "sms", "mesaj", "brauzer", "xerite", "teqvim", "wifi", "bluetooth", "mahni", "musiqi")
                if (canonicalApp.isNotBlank() && canonicalApp !in blacklist) {
                    return CommandIntent(
                        intentId = "OPEN_APP",
                        category = IntentCategory.APP,
                        action = CommandAction.OPEN_APP,
                        targetApp = canonicalApp,
                        rawQuery = rawQuery,
                        normalizedQuery = normalized,
                        confidence = IntentConfidence.HIGH_HEURISTIC,
                        source = IntentSource.SEMANTIC_PARSER,
                        executionStrategy = ExecutionStrategy.DIRECT_API,
                        parameters = mapOf("app_name" to canonicalApp),
                        entities = listOf(ExtractedEntity("app_name", canonicalApp))
                    )
                }
            }
        }
        return null
    }

    private fun extractCasedQuery(rawQuery: String, appName: String, queryFallback: String): String {
        // Locate query region in rawQuery to preserve original casing (e.g. INNA Caliente)
        val rawLower = rawQuery.lowercase(Locale.ROOT)
        val queryLower = queryFallback.lowercase(Locale.ROOT)

        val firstWord = queryLower.split(" ").firstOrNull() ?: ""
        if (firstWord.length >= 2) {
            val startIdx = rawLower.indexOf(firstWord)
            if (startIdx >= 0) {
                var candidate = rawQuery.substring(startIdx)
                val trailingVerbs = listOf(" aç", " açaq", " çal", " oynat", " tap", " axtar", " dinlə", " open", " play", " yaz", " axtarış")
                for (v in trailingVerbs) {
                    if (candidate.lowercase().endsWith(v.lowercase())) {
                        candidate = candidate.dropLast(v.length)
                    }
                }
                val mediaWords = listOf("mahnısını", "mahnını", "musiqisini", "musiqini", "parçanı", "şarkısını", "mahnisini", "mahnini")
                for (m in mediaWords) {
                    if (candidate.trimEnd().lowercase().endsWith(m.lowercase())) {
                        candidate = candidate.trimEnd().dropLast(m.length)
                    }
                }
                if (candidate.trim().isNotBlank()) {
                    return candidate.trim()
                }
            }
        }
        return queryFallback
    }
}
