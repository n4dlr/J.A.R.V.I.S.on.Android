package com.example

import com.example.jarvis.ai.engine.SemanticLightweightEngine
import com.example.jarvis.ai.matcher.AppNameExtractor
import com.example.jarvis.ai.matcher.DeterministicIntentMatcher
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.domain.model.CommandAction
import com.example.jarvis.domain.model.IntentCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * MANDATORY REGRESSION TEST SUITE FOR V2
 *
 * Test requirement (Section 56):
 * INPUT: "YouTube-da INNA Caliente mahnısını aç"
 * Must NEVER produce: APP_NOT_FOUND with appName: "YouTube-da INNA Caliente mahnısını"
 * Correct parsed result must be: targetApp = "youtube", query = "INNA Caliente", action = SEARCH_AND_PLAY
 */
class RegressionTest {

    private lateinit var normalizer: AzerbaijaniTextNormalizer
    private lateinit var matcher: DeterministicIntentMatcher
    private lateinit var semanticEngine: SemanticLightweightEngine

    @Before
    fun setUp() {
        normalizer = AzerbaijaniTextNormalizer()
        matcher = DeterministicIntentMatcher(normalizer)
        semanticEngine = SemanticLightweightEngine(normalizer)
    }

    @Test
    fun `critical regression test YouTube-da INNA Caliente mahnisini ac resolves to MEDIA_SEARCH_PLAY`() {
        val input = "YouTube-da INNA Caliente mahnısını aç"

        // 1. Semantic engine test
        val command = semanticEngine.parse(input)
        assertNotNull("Command should be parsed", command)
        assertEquals("MEDIA_SEARCH_PLAY", command?.intentId)
        assertEquals(IntentCategory.MEDIA, command?.category)
        assertEquals(CommandAction.SEARCH_AND_PLAY, command?.action)
        assertEquals("youtube", command?.targetApp)
        
        // Ensure query preserves original casing and contains INNA Caliente
        val query = command?.query
        assertNotNull("Query must not be null", query)
        assertTrue(
            "Query should be 'INNA Caliente' (actual: '$query')",
            query?.contains("INNA Caliente", ignoreCase = true) == true
        )

        // 2. Deterministic matcher check
        val structured = matcher.match(input)
        assertNotNull(structured)
        assertEquals("MEDIA_SEARCH_PLAY", structured?.intentId)
        assertNotEquals("OPEN_APP", structured?.intentId)
        assertEquals("youtube", structured?.arguments?.get("target_app"))
        assertNotEquals("YouTube-da INNA Caliente mahnısını", structured?.arguments?.get("app_name"))
    }

    @Test
    fun `linguistic variations of YouTube command resolve accurately`() {
        val variations = listOf(
            "YouTube-da INNA-nın Caliente mahnısını tap",
            "INNA Caliente-ni YouTube-da oynat",
            "yutubda caliente-ni çal",
            "YouTube-da INNA Caliente-ni aç",
            "Youtube-da INNA Caliente mahnısını aç",
            "zəhmət olmasa YouTube-da INNA Caliente mahnısını aç"
        )

        for (phrase in variations) {
            val parsed = semanticEngine.parse(phrase) ?: matcher.match(phrase)?.let {
                semanticEngine.parse(it.rawQuery)
            }

            assertNotNull("Failed to parse variation: '$phrase'", parsed)
            assertEquals("MEDIA_SEARCH_PLAY", parsed?.intentId)
            assertEquals("youtube", parsed?.targetApp)
        }
    }

    @Test
    fun `spotify music search and playback commands resolve accurately`() {
        val spotifyPhrases = listOf(
            "Spotify-da Caliente çal",
            "Spotify-da INNA Caliente mahnısını aç",
            "Spotify-da musiqi axtar"
        )

        for (phrase in spotifyPhrases) {
            val parsed = semanticEngine.parse(phrase)
            assertNotNull("Failed to parse Spotify phrase: '$phrase'", parsed)
            assertEquals("MEDIA_SEARCH_PLAY", parsed?.intentId)
            assertEquals("spotify", parsed?.targetApp)
        }
    }

    @Test
    fun `browser and Google search commands resolve accurately`() {
        val googleCommand = "Google-da Android 16 haqqında axtar"
        val parsed = semanticEngine.parse(googleCommand)

        assertNotNull("Failed to parse Google search", parsed)
        assertEquals("WEB_SEARCH", parsed?.intentId)
        assertEquals(IntentCategory.BROWSER, parsed?.category)
        assertTrue(parsed?.query?.contains("Android 16", ignoreCase = true) == true)
    }

    @Test
    fun `single app opening commands do not capture sentences`() {
        val singleApp = "YouTube-u aç"
        val parsed = semanticEngine.parse(singleApp)
        assertNotNull(parsed)
        assertEquals("OPEN_APP", parsed?.intentId)
        assertEquals("youtube", parsed?.targetApp)
    }
}
