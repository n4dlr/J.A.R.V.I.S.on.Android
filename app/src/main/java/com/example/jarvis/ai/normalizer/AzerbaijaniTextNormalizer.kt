package com.example.jarvis.ai.normalizer

class AzerbaijaniTextNormalizer {

    // Common colloquial/slang and phonetic replacements in Azerbaijani
    private val synonymMap = mapOf(
        "zaryatka" to "zaryadka",
        "zaryatqa" to "zaryadka",
        "zaryadqa" to "zaryadka",
        "batareyka" to "batareya",
        "batareykani" to "batareyani",
        "pil" to "batareya",
        "fanar" to "fener",
        "fanari" to "feneri",
        "fenari" to "feneri",
        "isigi" to "feneri",
        "isiq" to "fener",
        "isigi yandir" to "feneri yandir",
        "isigi sondur" to "feneri sondur",
        "operativka" to "ram",
        "operativ" to "ram",
        "pamyat" to "yaddas",
        "budilnik" to "zengli saat",
        "budilniki" to "zengli saati",
        "alarm" to "zengli saat",
        "alarmi" to "zengli saati",
        "foto" to "sekil",
        "selfi" to "sekil",
        "fotokamera" to "kamera",
        "kilidle" to "kilidle",
        "blakirofka" to "kilidle",
        "blokirofka" to "kilidle",
        "blokla" to "kilidle",
        "vayfay" to "wifi",
        "vayfayi" to "wifi",
        "blutuz" to "bluetooth",
        "blutuzu" to "bluetooth",
        "wi fi" to "wifi",
        "wi-fi" to "wifi",
        "ayarlar" to "tenzimlemeler",
        "nastroyka" to "tenzimlemeler",
        "nastroykani" to "tenzimlemeler",
        "vatsap" to "whatsapp",
        "vatsapi" to "whatsapp",
        "yutub" to "youtube",
        "yutubu" to "youtube"
    )

    fun normalize(rawText: String): String {
        if (rawText.isBlank()) return ""

        var processed = rawText.lowercase(java.util.Locale.ROOT).trim()

        // 1. Replace special Azerbaijani characters with standard ASCII equivalents for resilient phonetic matching
        processed = processed
            .replace('ə', 'e')
            .replace('ı', 'i')
            .replace('ö', 'o')
            .replace('ğ', 'g')
            .replace('ş', 's')
            .replace('ç', 'c')
            .replace('ü', 'u')
            .replace('İ', 'i')
            .replace('Ə', 'e')

        // 2. Remove punctuation and excess symbols
        processed = processed.replace(Regex("""[^\w\s\d]"""), " ")

        // 3. Condense whitespaces
        processed = processed.replace(Regex("""\s+"""), " ").trim()

        // 4. Token-level and phrase-level synonym normalization
        for ((slang, standard) in synonymMap) {
            processed = processed.replace(Regex("""\b$slang\b"""), standard)
        }

        return processed
    }

    /**
     * Compute Levenshtein distance for fuzzy matching typos
     */
    fun computeLevenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    fun isFuzzyMatch(word: String, target: String, maxDistance: Int = 2): Boolean {
        if (word == target) return true
        if (kotlin.math.abs(word.length - target.length) > maxDistance) return false
        return computeLevenshteinDistance(word, target) <= maxDistance
    }
}
