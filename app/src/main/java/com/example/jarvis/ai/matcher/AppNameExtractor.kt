package com.example.jarvis.ai.matcher

/**
 * Utility to extract the canonical app name from Azerbaijani inflected forms.
 *
 * In Azerbaijani, nouns take postposition suffixes depending on grammatical case:
 *   Locative:     YouTube-da, WhatsApp-da, Spotify-də
 *   Dative:       YouTube-a, Telegram-a
 *   Genitive:     YouTube-un, WhatsApp-ın
 *   Accusative:   YouTube-u, WhatsApp-ı
 *   Ablative:     YouTube-dan, Spotify-dən
 *   etc.
 *
 * This class strips those suffixes so "youtube-da" → "youtube".
 * It also handles spoken STT phonetic variants of common app names.
 */
object AppNameExtractor {

    /**
     * Azerbaijani postposition suffixes to strip (ordered by length, longest first
     * to avoid partial stripping). Applied after a hyphen or at word boundaries.
     */
    private val hyphenatedSuffixes = listOf(
        // Ablative
        "-dən", "-dan", "-tən", "-tan", "-ndan", "-ndən",
        // Locative
        "-də", "-da", "-tə", "-ta",
        // Dative
        "-yə", "-ya", "-ə", "-a",
        // Genitive
        "-nun", "-nün", "-nın", "-nin", "-ın", "-in", "-un", "-ün",
        // Accusative
        "-nı", "-ni", "-nu", "-nü", "-ı", "-i", "-u", "-ü",
        // Instrumental / other
        "-la", "-lə", "-yla", "-ylə"
    )

    /**
     * Common STT phonetic variants → canonical name.
     * Keys are lowercase, values are the canonical app keyword.
     */
    private val phoneticVariants = mapOf(
        "yutub" to "youtube",
        "yutubu" to "youtube",
        "yutube" to "youtube",
        "youtubu" to "youtube",
        "vatsap" to "whatsapp",
        "vatsapi" to "whatsapp",
        "vatsapda" to "whatsapp",
        "telqram" to "telegram",
        "telqrami" to "telegram",
        "instaqram" to "instagram",
        "instaqrami" to "instagram",
        "spoitify" to "spotify",
        "spotifyde" to "spotify",
        "xrom" to "chrome",
        "xromu" to "chrome",
        "krom" to "chrome"
    )

    /**
     * Extract the canonical app name token from an inflected word.
     *
     * Examples:
     *   "youtube-da"   → "youtube"
     *   "whatsapp-a"   → "whatsapp"
     *   "spotify-dən"  → "spotify"
     *   "telegram-ın"  → "telegram"
     *   "yutubda"      → "youtube"   (phonetic variant lookup)
     *   "chrome"       → "chrome"    (no suffix, returned as-is)
     */
    fun extract(word: String): String {
        val lower = word.lowercase().trim()

        // 1. Check phonetic variants first (full-word lookup)
        phoneticVariants[lower]?.let { return it }

        // 2. Strip hyphenated postposition suffix
        for (suffix in hyphenatedSuffixes) {
            if (lower.endsWith(suffix)) {
                val stripped = lower.dropLast(suffix.length)
                // Check if the stripped form is itself a phonetic variant
                return phoneticVariants[stripped] ?: stripped
            }
        }

        // 3. Strip unhyphenated suffixes (e.g. "yutubda" -> "yutub" -> "youtube", "vatsapa" -> "vatsap" -> "whatsapp")
        val unhyphenatedSuffixes = listOf("dan", "dən", "tan", "tən", "da", "də", "ta", "tə", "nın", "nin", "nun", "nün", "ın", "in", "un", "ün", "nı", "ni", "nu", "nü", "ı", "i", "u", "ü", "ya", "yə", "a", "e")
        for (suffix in unhyphenatedSuffixes) {
            if (lower.endsWith(suffix) && lower.length > suffix.length + 2) {
                val stripped = lower.dropLast(suffix.length)
                if (isKnownApp(stripped) || phoneticVariants.containsKey(stripped)) {
                    return phoneticVariants[stripped] ?: stripped
                }
            }
        }

        return lower
    }

    /**
     * Try to parse a compound locative structure from a normalized query.
     * Returns Pair(appName, restOfQuery) if pattern is found, null otherwise.
     *
     * Supported patterns:
     *   "youtube-da X aç/axtar/çal/oyna"   → ("youtube", "X")
     *   "X-i spotify-da çal"                → ("spotify", "X")
     *   "spotify-da X musiqisini açıq"       → ("spotify", "X musiqisi")
     */
    fun extractAppAndQuery(normalizedTokens: List<String>): Pair<String, String>? {
        if (normalizedTokens.isEmpty()) return null

        // Look for any token that resolves to a known app
        for (i in normalizedTokens.indices) {
            val token = normalizedTokens[i]
            val appName = extract(token)
            if (isKnownApp(appName)) {
                val postpositions = setOf("da", "de", "ta", "te", "dan", "den", "nun", "nin", "in", "un", "ni", "nu", "i", "u", "a", "e", "nda", "nde")
                val verbWords = setOf("ac", "axtar", "oyna", "cal", "oynat", "baslat", "tap", "goster", "aciq", "yandir", "dinle", "iste", "oxut", "play")
                val queryTokens = normalizedTokens.filterIndexed { idx, t ->
                    idx != i && !(idx == i + 1 && t in postpositions) && t !in verbWords
                }
                if (queryTokens.isNotEmpty()) {
                    return Pair(appName, queryTokens.joinToString(" "))
                }
            }
        }

        return null
    }

    private fun isLikelyAppWithSuffix(token: String): Boolean {
        // Tokens like "youtubeda", "whatsappin" where suffix is glued
        val knownRoots = setOf("youtube", "yutub", "whatsapp", "vatsap", "telegram", "telqram",
            "spotify", "instagram", "instaqram", "chrome", "xrom", "netflix", "tiktok",
            "google", "gmail", "maps", "xerite", "camera", "kamera", "settings", "tenzimlemeler")
        val lower = token.lowercase()
        return knownRoots.any { lower.startsWith(it) && lower.length > it.length }
    }

    private fun isKnownApp(name: String): Boolean {
        val knownApps = setOf(
            "youtube", "whatsapp", "telegram", "spotify", "instagram", "chrome",
            "netflix", "tiktok", "google", "gmail", "maps", "camera", "settings",
            "yutub", "vatsap", "telqram", "instaqram", "xrom", "kamera", "xerite"
        )
        return name in knownApps
    }
}
