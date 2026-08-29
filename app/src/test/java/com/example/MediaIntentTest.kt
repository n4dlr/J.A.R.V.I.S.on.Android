package com.example

import com.example.jarvis.ai.matcher.DeterministicIntentMatcher
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MediaIntentTest {

    private lateinit var matcher: DeterministicIntentMatcher

    @Before
    fun setUp() {
        matcher = DeterministicIntentMatcher(AzerbaijaniTextNormalizer())
    }

    @Test
    fun `user reported command YouTube-da INNA Caliente mahnisini ac matches MEDIA_SEARCH_PLAY`() {
        val raw = "YouTube-da INNA Caliente mahnısını aç"
        val intent = matcher.match(raw)

        assertNotNull("Intent should not be null for '$raw'", intent)
        assertEquals("MEDIA_SEARCH_PLAY", intent?.intentId)
        assertEquals("youtube", intent?.arguments?.get("target_app"))
        val query = intent?.arguments?.get("query")
        assertNotNull("Query should not be null", query)
        assertTrue(
            "Query should contain 'INNA Caliente' (actual: '$query')",
            query?.contains("INNA Caliente", ignoreCase = true) == true || query?.contains("inna caliente", ignoreCase = true) == true
        )
    }

    @Test
    fun `variants of YouTube media command resolve properly`() {
        val testCases = listOf(
            "YouTube-da INNA-nın Caliente mahnısını tap" to "youtube",
            "INNA Caliente-ni YouTube-da oynat" to "youtube",
            "Caliente mahnısını aç" to "MEDIA_PLAY",
            "Spotify-da INNA Caliente çal" to "spotify",
            "Spotify-da musiqi axtar" to "spotify"
        )

        for ((phrase, expectedTarget) in testCases) {
            val intent = matcher.match(phrase)
            assertNotNull("Intent should not be null for '$phrase'", intent)
            if (expectedTarget == "MEDIA_PLAY") {
                assertEquals("MEDIA_PLAY", intent?.intentId)
            } else {
                assertEquals("MEDIA_SEARCH_PLAY", intent?.intentId)
                assertEquals(expectedTarget, intent?.arguments?.get("target_app"))
            }
        }
    }

    @Test
    fun `browser and Google search commands resolve properly`() {
        val googleSearch = "Google-da Android 16 haqqında axtar"
        val intent1 = matcher.match(googleSearch)
        assertNotNull(intent1)
        assertTrue(intent1?.intentId in listOf("APP_SEARCH", "WEB_SEARCH"))

        val chromeOpen = "Chrome-da Google-u aç"
        val intent2 = matcher.match(chromeOpen)
        assertNotNull(intent2)
    }

    @Test
    fun `bare app open does not treat full sentence as app name`() {
        val intent = matcher.match("YouTube aç")
        assertNotNull(intent)
        assertEquals("OPEN_APP", intent?.intentId)
        assertEquals("youtube", intent?.arguments?.get("app_name"))
    }
}
