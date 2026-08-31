package com.example

import com.example.jarvis.agent.MultiStepAgentEngine
import com.example.jarvis.ai.matcher.DeterministicIntentMatcher
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.tools.ToolRegistry
import com.example.jarvis.tools.impl.news.GetNewsTool
import com.example.jarvis.tools.impl.smarthome.HomeAssistantClient
import com.example.jarvis.tools.impl.smarthome.SmartHomeClimateTool
import com.example.jarvis.tools.impl.smarthome.SmartHomeLightTool
import com.example.jarvis.tools.impl.smarthome.SmartHomeLockTool
import com.example.jarvis.tools.impl.smarthome.SmartHomeSceneTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NewFeaturesPhase6Test {

    private lateinit var normalizer: AzerbaijaniTextNormalizer
    private lateinit var matcher: DeterministicIntentMatcher

    @Before
    fun setup() {
        normalizer = AzerbaijaniTextNormalizer()
        matcher = DeterministicIntentMatcher(normalizer)
    }

    // ── 1. MultiStepAgentEngine Tests ──────────────────────────────────────────

    @Test
    fun testMultiStepDetectionWithConjunctions() {
        val engine = MultiStepAgentEngine(normalizer)
        assertTrue(engine.isMultiStep("Instagram-a gir, sonra son posta bax ve şərh yaz"))
        assertTrue(engine.isMultiStep("Əvvəlcə YouTube-u aç, sonra musiqi axtar"))
        assertTrue(engine.isMultiStep("WhatsApp-ı aç və Nadirə mesaj yaz"))
    }

    @Test
    fun testMultiStepPlanGenerationProducesSteps() {
        val engine = MultiStepAgentEngine(normalizer)
        val plan = engine.parse("Instagram-a gir, sonra son posta bax")
        assertNotNull(plan)
        assertTrue(plan.steps.isNotEmpty())
        assertEquals("Instagram-a gir, sonra son posta bax", plan.goal)
    }

    // ── 2. GetNewsTool Tests ───────────────────────────────────────────────────

    @Test
    fun testNewsToolParametersAndMetadata() {
        val newsTool = GetNewsTool()
        assertEquals("GET_NEWS", newsTool.id)
        assertTrue(newsTool.parameters.any { it.name == "count" })
        assertTrue(newsTool.parameters.any { it.name == "lang" })
    }

    // ── 3. Smart Home Tools Tests ──────────────────────────────────────────────

    @Test
    fun testSmartHomeToolsMetadata() {
        val fakeClient = HomeAssistantClient("http://192.168.1.100:8123", "fake_token")
        val lightTool = SmartHomeLightTool { fakeClient }
        val climateTool = SmartHomeClimateTool { fakeClient }
        val lockTool = SmartHomeLockTool { fakeClient }
        val sceneTool = SmartHomeSceneTool { fakeClient }

        assertEquals("SMART_HOME_LIGHT", lightTool.id)
        assertEquals("SMART_HOME_CLIMATE", climateTool.id)
        assertEquals("SMART_HOME_LOCK", lockTool.id)
        assertEquals("SMART_HOME_SCENE", sceneTool.id)
    }

    // ── 4. DeterministicIntentMatcher Phase 6 Patterns ─────────────────────────

    @Test
    fun testNewsIntentMatching() {
        val intent1 = matcher.match("Son xəbərləri oxu")
        assertNotNull(intent1)
        assertEquals("GET_NEWS", intent1?.intentId)

        val intent2 = matcher.match("Xəbərlər")
        assertNotNull(intent2)
        assertEquals("GET_NEWS", intent2?.intentId)
    }

    @Test
    fun testSpotifyTrackQueryMatching() {
        val intent = matcher.match("Spotify-da Tarkan Şımarık çal")
        assertNotNull(intent)
        assertTrue(intent?.intentId in listOf("SPOTIFY_PLAY", "MEDIA_SEARCH_PLAY"))
        assertEquals("spotify", intent?.arguments?.get("target_app"))
    }

    @Test
    fun testSpotifyCurrentTrackMatching() {
        val intent = matcher.match("Hansı mahnı çalınır?")
        assertNotNull(intent)
        assertEquals("SPOTIFY_CURRENT_TRACK", intent?.intentId)
    }

    @Test
    fun testSmartHomeLightIntentMatching() {
        val intent = matcher.match("Qonaq otağının işığını yandır")
        assertNotNull(intent)
        assertEquals("SMART_HOME_LIGHT", intent?.intentId)
        assertEquals("on", intent?.arguments?.get("action"))
        assertEquals("qonaq", intent?.arguments?.get("room"))
    }

    @Test
    fun testSmartHomeClimateIntentMatching() {
        val intent = matcher.match("Kondisioneri 24 dərəcə et")
        assertNotNull(intent)
        assertEquals("SMART_HOME_CLIMATE", intent?.intentId)
        assertEquals("24", intent?.arguments?.get("temperature"))
    }

    @Test
    fun testSmartHomeLockIntentMatching() {
        val intent = matcher.match("Qapını kilidlə")
        assertNotNull(intent)
        assertEquals("SMART_HOME_LOCK", intent?.intentId)
        assertEquals("lock", intent?.arguments?.get("action"))
    }

    @Test
    fun testSmartHomeSceneIntentMatching() {
        val intent = matcher.match("Film rejimini aç")
        assertNotNull(intent)
        assertEquals("SMART_HOME_SCENE", intent?.intentId)
        assertEquals("film", intent?.arguments?.get("scene"))
    }

    // ── 5. ToolRegistry Phase 6 Default Registration ───────────────────────────

    @Test
    fun testPhase6DefaultToolsRegistered() {
        val registry = ToolRegistry()
        assertTrue("GET_NEWS must be registered in default tools", registry.hasTool("GET_NEWS"))
    }
}
