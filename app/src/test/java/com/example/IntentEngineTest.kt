package com.example

import com.example.jarvis.ai.engine.IntentEngine
import com.example.jarvis.ai.engine.SemanticLightweightEngine
import com.example.jarvis.ai.matcher.DeterministicIntentMatcher
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.ai.provider.LocalSLMProvider
import com.example.jarvis.domain.model.CommandAction
import com.example.jarvis.domain.model.IntentCategory
import com.example.jarvis.domain.model.IntentSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IntentEngineTest {

    private lateinit var normalizer: AzerbaijaniTextNormalizer
    private lateinit var deterministicMatcher: DeterministicIntentMatcher
    private lateinit var semanticEngine: SemanticLightweightEngine
    private lateinit var localSLMProvider: LocalSLMProvider
    private lateinit var intentEngine: IntentEngine

    @Before
    fun setUp() {
        normalizer = AzerbaijaniTextNormalizer()
        deterministicMatcher = DeterministicIntentMatcher(normalizer)
        semanticEngine = SemanticLightweightEngine(normalizer)
        localSLMProvider = LocalSLMProvider(appContext = null, normalizer = normalizer, matcher = deterministicMatcher)
        intentEngine = IntentEngine(
            deterministicMatcher = deterministicMatcher,
            semanticLightweightEngine = semanticEngine,
            localSLMProvider = localSLMProvider,
            geminiProvider = localSLMProvider,
            normalizer = normalizer
        )
    }

    @Test
    fun `level 1 deterministic commands route instantly`() = runBlocking {
        val torchReport = intentEngine.classify("Fənəri yandır", isOnline = false)
        assertEquals("TORCH", torchReport.commandIntent.intentId)
        assertEquals(IntentSource.DETERMINISTIC_RULES, torchReport.source)

        val batteryReport = intentEngine.classify("Batareya neçə faizdir?", isOnline = false)
        assertEquals("GET_BATTERY", batteryReport.commandIntent.intentId)
        assertEquals(IntentSource.DETERMINISTIC_RULES, batteryReport.source)

        val ramReport = intentEngine.classify("RAM nə qədərdir?", isOnline = false)
        assertEquals("GET_RAM", ramReport.commandIntent.intentId)
    }

    @Test
    fun `level 2 semantic engine routes compound media commands`() = runBlocking {
        val ytReport = intentEngine.classify("YouTube-da INNA Caliente mahnısını aç", isOnline = false)
        assertEquals("MEDIA_SEARCH_PLAY", ytReport.commandIntent.intentId)
        assertEquals(IntentCategory.MEDIA, ytReport.commandIntent.category)
        assertEquals("youtube", ytReport.commandIntent.targetApp)
        assertTrue(ytReport.commandIntent.query?.contains("INNA Caliente", ignoreCase = true) == true)

        val spotifyReport = intentEngine.classify("Spotify-da Caliente çal", isOnline = false)
        assertEquals("MEDIA_SEARCH_PLAY", spotifyReport.commandIntent.intentId)
        assertEquals("spotify", spotifyReport.commandIntent.targetApp)
    }

    @Test
    fun `multi-lingual English commands route correctly`() = runBlocking {
        val playReport = intentEngine.classify("Play INNA Caliente on YouTube", isOnline = false)
        assertEquals("MEDIA_SEARCH_PLAY", playReport.commandIntent.intentId)
        assertEquals("youtube", playReport.commandIntent.targetApp)

        val searchReport = intentEngine.classify("Search Android 16 on Google", isOnline = false)
        assertEquals("WEB_SEARCH", searchReport.commandIntent.intentId)
        assertTrue(searchReport.commandIntent.query?.contains("Android 16") == true)
    }

    @Test
    fun `polite filler words are stripped cleanly`() = runBlocking {
        val politeReport = intentEngine.classify("Zəhmət olmasa fənəri söndür", isOnline = false)
        assertEquals("TORCH", politeReport.commandIntent.intentId)

        val politeYtReport = intentEngine.classify("Bir zəhmət YouTube-da INNA Caliente mahnısını aç", isOnline = false)
        assertEquals("MEDIA_SEARCH_PLAY", politeYtReport.commandIntent.intentId)
        assertEquals("youtube", politeYtReport.commandIntent.targetApp)
    }
}
