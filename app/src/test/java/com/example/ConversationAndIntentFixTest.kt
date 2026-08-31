package com.example

import com.example.jarvis.ai.engine.SemanticLightweightEngine
import com.example.jarvis.ai.matcher.DeterministicIntentMatcher
import com.example.jarvis.automation.apps.AppResolver
import com.example.jarvis.voice.WakeWordDetector
import com.example.jarvis.voice.WakeWordEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationAndIntentFixTest {

    private val matcher = DeterministicIntentMatcher()
    private val semanticEngine = SemanticLightweightEngine()
    private val dummyContext = TestContext()
    private val appResolver = AppResolver(dummyContext)
    private val wakeWordDetector = WakeWordDetector(dummyContext, com.example.jarvis.voice.VoiceRecognizerHelper(dummyContext))

    @Test
    fun testGreetingAndSmalltalkMatching() {
        val salamIntent = matcher.match("salam")
        assertNotNull(salamIntent)
        assertEquals("GREETING_AND_CHAT", salamIntent?.intentId)

        val necesenIntent = matcher.match("necesen")
        assertNotNull(necesenIntent)
        assertEquals("GREETING_AND_CHAT", necesenIntent?.intentId)

        val kimsenIntent = matcher.match("sen kimsen")
        assertNotNull(kimsenIntent)
        assertEquals("GREETING_AND_CHAT", kimsenIntent?.intentId)

        val sagolIntent = matcher.match("cox sag ol")
        assertNotNull(sagolIntent)
        assertEquals("GREETING_AND_CHAT", sagolIntent?.intentId)
    }

    @Test
    fun testSalamDoesNotMatchClockAppInResolver() {
        val resolution = appResolver.resolveApp("salam")
        assertFalse("salam should not resolve to Saat/Clock app", resolution.matched)
    }

    @Test
    fun testNecesenDoesNotTriggerAppLaunch() {
        val intent = semanticEngine.parse("necesen")
        // Should not be OPEN_APP
        assertTrue(intent == null || intent.intentId != "OPEN_APP")
    }

    @Test
    fun testYouTubeMediaSearchPlay() {
        val intent = matcher.match("YouTube-da INNA Caliente mahnısını aç")
        assertNotNull(intent)
        assertEquals("MEDIA_SEARCH_PLAY", intent?.intentId)
        assertEquals("youtube", intent?.arguments?.get("target_app")?.lowercase())
        assertTrue(intent?.arguments?.get("query")?.contains("INNA Caliente", ignoreCase = true) == true)
    }

    @Test
    fun testWakeWordWordBoundaryMatching() {
        val eventOnly = wakeWordDetector.extractWakeWordCommand("hey jarvis")
        assertNotNull(eventOnly)
        assertTrue(eventOnly is WakeWordEvent.WakeWordOnly)

        val eventWithCmd = wakeWordDetector.extractWakeWordCommand("jarvis fənəri yandır")
        assertNotNull(eventWithCmd)
        assertTrue(eventWithCmd is WakeWordEvent.WakeWordWithCommand)

        // Random noise or unrelated word should return null
        val noiseEvent = wakeWordDetector.extractWakeWordCommand("bu gün hava çox gözəldir")
        assertTrue(noiseEvent == null)
    }
}
