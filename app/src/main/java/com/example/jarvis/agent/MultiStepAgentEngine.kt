package com.example.jarvis.agent

import android.util.Log
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer

/**
 * Multi-step Natural Language Understanding engine.
 *
 * Parses free-form user queries like:
 *   "Instagram-a gir, son posta bax ve şərh yaz"
 *   "Əvvəlcə YouTube-u aç, sonra musiqini axtar"
 *   "WhatsApp-ı aç, Anara mesaj yaz: Salam"
 *
 * into an ordered list of [PlanStep] objects that [AgentExecutor] can execute sequentially.
 */
class MultiStepAgentEngine(
    private val normalizer: AzerbaijaniTextNormalizer = AzerbaijaniTextNormalizer()
) {

    companion object {
        private const val TAG = "MultiStepAgentEngine"

        // Conjunctions that split multi-step commands
        private val STEP_SEPARATORS = listOf(
            "sonra", "daha sonra", "ardindan", "ardından", "bundan sonra",
            "ve", "həm də", "hem de", "daha", "birincilik", "ikinci olaraq"
        )

        // Sequence trigger patterns: "əvvəlcə X, sonra Y"
        private val SEQUENCE_PATTERNS = listOf(
            Regex("""(?i)\b(evvelce|əvvəlcə|birinci)\b"""),
            Regex("""(?i)\b(sonra|daha\s*sonra|ardindan)\b"""),
            Regex("""(?i)(.+?)\s*,\s*(.+?)\s*(ve|həm de|sonra)\s*(.+)"""),
            Regex("""(?i)\b(gir|ac|tap|bax|yaz|sil|gonder|paylas)\b.*\b(ve|sonra)\b.*\b(gir|ac|tap|bax|yaz|sil|gonder|paylas)\b""")
        )

        // App-specific action vocabulary
        private val APP_OPEN_KEYWORDS = setOf(
            "gir", "ac", "aç", "baslat", "başlat", "open", "launch", "islet", "işlət"
        )
        private val SCROLL_KEYWORDS = setOf(
            "asagi", "aşağı", "yukari", "yuxarı", "scroll", "surushtur", "sürüşdür"
        )
        private val SEARCH_KEYWORDS = setOf(
            "axtar", "tap", "find", "search", "yoxla"
        )
        private val WRITE_KEYWORDS = setOf(
            "yaz", "yazdir", "yazdır", "gonder", "göndər", "mesaj"
        )
        private val VIEW_KEYWORDS = setOf(
            "bax", "gor", "gör", "oxu", "göstər", "goster"
        )
        private val LIKE_KEYWORDS = setOf("begən", "begən", "begen", "like", "like et")
        private val COMMENT_KEYWORDS = setOf("serh", "şərh", "yorum", "comment")
        private val SHARE_KEYWORDS = setOf("paylas", "paylaş", "share", "forward", "yonelt")
        private val DELETE_KEYWORDS = setOf("sil", "delete", "remove", "ləğv et")
    }

    /**
     * Detect whether the query requires multi-step agent planning.
     */
    fun isMultiStep(rawQuery: String): Boolean {
        val normalized = normalizer.normalize(rawQuery)
        val hasConjunction = STEP_SEPARATORS.any { normalized.contains(it) }
        val hasCommaSteps = rawQuery.count { it == ',' } >= 1 && rawQuery.length > 20
        val hasSequencePattern = SEQUENCE_PATTERNS.any { it.containsMatchIn(normalized) }
        return hasConjunction || hasCommaSteps || hasSequencePattern
    }

    /**
     * Parse [rawQuery] into a [MultiStepPlan] containing ordered [PlanStep] objects.
     */
    fun parse(rawQuery: String): MultiStepPlan {
        val normalized = normalizer.normalize(rawQuery)
        Log.d(TAG, "Parsing multi-step query: $rawQuery")

        // Split on conjunctions and commas to isolate individual action phrases
        val phrases = splitIntoPhrases(normalized, rawQuery)
        Log.d(TAG, "Extracted ${phrases.size} phrases: $phrases")

        val steps = mutableListOf<PlanStep>()
        phrases.forEachIndexed { i, phrase ->
            val step = phraseToStep(phrase, i + 1)
            if (step != null) steps.add(step)
        }

        // If parsing failed to produce multiple steps, fall back to minimal plan
        if (steps.isEmpty()) {
            steps.add(PlanStep("step_1", "UNKNOWN", description = rawQuery))
        }

        return MultiStepPlan(
            goal = rawQuery,
            rationale = "Çoxaddımlı əmr: ${steps.size} ardıcıl addım icra ediləcək.",
            steps = steps
        )
    }

    private fun splitIntoPhrases(normalized: String, original: String): List<String> {
        // 1. Split on commas first
        val commaSplit = original.split(Regex("""\s*,\s*""")).filter { it.isNotBlank() }
        if (commaSplit.size > 1) {
            // Further split each comma-part on conjunctions
            return commaSplit.flatMap { part ->
                splitOnConjunctions(normalizer.normalize(part), part)
            }
        }
        // 2. Split on conjunctions
        return splitOnConjunctions(normalized, original)
    }

    private fun splitOnConjunctions(normalized: String, original: String): List<String> {
        var remaining = original.trim()
        val result = mutableListOf<String>()

        // Sort separators by length (longest first) to avoid partial matches
        val sorted = STEP_SEPARATORS.sortedByDescending { it.length }
        for (sep in sorted) {
            if (remaining.contains(sep, ignoreCase = true)) {
                val idx = remaining.indexOf(sep, ignoreCase = true)
                val before = remaining.substring(0, idx).trim()
                val after = remaining.substring(idx + sep.length).trim()
                if (before.isNotBlank()) result.add(before)
                remaining = after
                break
            }
        }
        if (remaining.isNotBlank()) result.add(remaining)
        return if (result.isEmpty()) listOf(original) else result
    }

    private fun phraseToStep(phrase: String, index: Int): PlanStep? {
        val norm = normalizer.normalize(phrase).lowercase()
        val stepId = "step_$index"

        // Detect app name mentioned in phrase
        val appName = extractAppName(norm)

        return when {
            // Open an app
            APP_OPEN_KEYWORDS.any { norm.contains(it) } && appName != null -> {
                PlanStep(stepId, "OPEN_APP", mapOf("app_name" to appName), "\"$appName\" tətbiqini aç")
            }

            // Scroll action
            SCROLL_KEYWORDS.any { norm.contains(it) } -> {
                val dir = if (norm.contains("asagi") || norm.contains("aşağı")) "down" else "up"
                PlanStep(stepId, "ACC_SCROLL", mapOf("direction" to dir), "Yuxarı/aşağı sürüşdür")
            }

            // Comment on a post
            COMMENT_KEYWORDS.any { norm.contains(it) } -> {
                val text = extractQuotedOrRest(phrase)
                PlanStep(stepId, "ACC_TYPE_COMMENT", mapOf("text" to text), "Şərh yaz: $text")
            }

            // Like a post
            LIKE_KEYWORDS.any { norm.contains(it) } -> {
                PlanStep(stepId, "ACC_CLICK_LIKE", emptyMap(), "Bəyən düyməsinə bas")
            }

            // Share content
            SHARE_KEYWORDS.any { norm.contains(it) } -> {
                PlanStep(stepId, "ACC_CLICK_SHARE", emptyMap(), "Paylaş düyməsinə bas")
            }

            // Write / type / send message
            WRITE_KEYWORDS.any { norm.contains(it) } -> {
                val text = extractQuotedOrRest(phrase)
                val recipient = extractPersonName(phrase)
                PlanStep(
                    stepId, "SEND_WHATSAPP_MESSAGE",
                    mapOf("recipient" to recipient, "message" to text),
                    "Mesaj yaz: $text"
                )
            }

            // View / read content
            VIEW_KEYWORDS.any { norm.contains(it) } -> {
                PlanStep(stepId, "ACC_FIND_ELEMENT", mapOf("type" to "post"), "Son postbax")
            }

            // Search action
            SEARCH_KEYWORDS.any { norm.contains(it) } -> {
                val query = extractQueryAfterKeyword(norm, SEARCH_KEYWORDS)
                PlanStep(stepId, "WEB_SEARCH", mapOf("query" to query), "Axtar: $query")
            }

            // Delete action
            DELETE_KEYWORDS.any { norm.contains(it) } -> {
                PlanStep(stepId, "ACC_CLICK_DELETE", emptyMap(), "Sil")
            }

            // App open (fallback if app name found)
            appName != null -> {
                PlanStep(stepId, "OPEN_APP", mapOf("app_name" to appName), "\"$appName\" aç")
            }

            else -> {
                Log.w(TAG, "Could not classify phrase: $phrase")
                null
            }
        }
    }

    private fun extractAppName(norm: String): String? {
        val appMap = mapOf(
            "instagram" to "instagram",
            "whatsapp" to "whatsapp",
            "vatsap" to "whatsapp",
            "telegram" to "telegram",
            "teleqram" to "telegram",
            "youtube" to "youtube",
            "yutub" to "youtube",
            "spotify" to "spotify",
            "twitter" to "twitter",
            "tiktok" to "tiktok",
            "facebook" to "facebook",
            "gmail" to "gmail",
            "chrome" to "chrome",
            "xrom" to "chrome",
            "kamera" to "camera",
            "camera" to "camera",
            "galereya" to "gallery",
            "gallery" to "gallery",
            "hesablamalar" to "calculator",
            "calculator" to "calculator",
            "xeberler" to "news",
            "news" to "news"
        )
        return appMap.entries.firstOrNull { norm.contains(it.key) }?.value
    }

    private fun extractPersonName(phrase: String): String {
        // Look for name before "mesaj", "yaz", "gonder" keywords
        val patterns = listOf(
            Regex("""(?i)(\w+)[-ə\s]+(mesaj|yaz|gonder)"""),
            Regex("""(?i)(a|e|ya|yə)\s+(\w+)\s+(mesaj|yaz|gonder)""")
        )
        for (p in patterns) {
            val m = p.find(phrase)
            if (m != null && m.groupValues.size > 1) {
                return m.groupValues[1].trim()
            }
        }
        return ""
    }

    private fun extractQuotedOrRest(phrase: String): String {
        // Look for text in quotes first
        val quoted = Regex("""["«»""](.+?)["«»""]""").find(phrase)
        if (quoted != null) return quoted.groupValues[1]
        // Otherwise return rest after colon
        val colonIdx = phrase.indexOf(':')
        if (colonIdx >= 0 && colonIdx < phrase.length - 1) {
            return phrase.substring(colonIdx + 1).trim()
        }
        return phrase.trim()
    }

    private fun extractQueryAfterKeyword(norm: String, keywords: Set<String>): String {
        for (kw in keywords) {
            val idx = norm.indexOf(kw)
            if (idx >= 0) {
                val rest = norm.substring(idx + kw.length).trim()
                if (rest.isNotBlank()) return rest
            }
        }
        return norm
    }
}

/** Thin wrapper around AgentPlan — same structure, used for type-clarity. */
typealias MultiStepPlan = AgentPlan
