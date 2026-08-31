package com.example

import com.example.jarvis.ai.matcher.DeterministicIntentMatcher
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.domain.model.IntentConfidence
import com.example.jarvis.tools.ToolRegistry
import com.example.jarvis.tools.impl.messaging.SendTelegramMessageTool
import com.example.jarvis.tools.impl.messaging.SendWhatsAppMessageTool
import com.example.jarvis.tools.impl.timer.SetTimerTool
import com.example.jarvis.tools.impl.vision.AnalyzePhotoTool
import com.example.jarvis.tools.impl.weather.GetWeatherTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NewFeaturesTest {

    private lateinit var normalizer: AzerbaijaniTextNormalizer
    private lateinit var matcher: DeterministicIntentMatcher
    private lateinit var toolRegistry: ToolRegistry

    @Before
    fun setup() {
        normalizer = AzerbaijaniTextNormalizer()
        matcher = DeterministicIntentMatcher(normalizer)
        toolRegistry = ToolRegistry()
    }

    @Test
    fun testWeatherIntentMatching() {
        val intent1 = matcher.match("Bakıda hava necədir")
        assertNotNull(intent1)
        assertEquals("GET_WEATHER", intent1?.intentId)
        assertEquals("Baku", intent1?.arguments?.get("city"))

        val intent2 = matcher.match("Gəncədə hava")
        assertNotNull(intent2)
        assertEquals("GET_WEATHER", intent2?.intentId)
        assertEquals("Ganja", intent2?.arguments?.get("city"))
    }

    @Test
    fun testTimerIntentMatching() {
        val intent1 = matcher.match("10 dəqiqəlik taymer qur")
        assertNotNull(intent1)
        assertEquals("SET_TIMER", intent1?.intentId)
        assertEquals("10", intent1?.arguments?.get("minutes"))

        val intent2 = matcher.match("30 saniyəlik taymer")
        assertNotNull(intent2)
        assertEquals("SET_TIMER", intent2?.intentId)
        assertEquals("30", intent2?.arguments?.get("seconds"))
    }

    @Test
    fun testWhatsAppMessagingIntentMatching() {
        val intent = matcher.match("WhatsApp-da Anara salam yaz")
        assertNotNull(intent)
        assertEquals("SEND_WHATSAPP_MESSAGE", intent?.intentId)
    }

    @Test
    fun testTelegramMessagingIntentMatching() {
        val intent = matcher.match("Telegramda Əliyə mesaj yaz")
        assertNotNull(intent)
        assertEquals("SEND_TELEGRAM_MESSAGE", intent?.intentId)
    }

    @Test
    fun testVisionIntentMatching() {
        val intent = matcher.match("Bu şəkildə nə var")
        assertNotNull(intent)
        assertEquals("ANALYZE_PHOTO", intent?.intentId)
    }

    @Test
    fun testNewToolsRegisteredInRegistry() {
        assertTrue(toolRegistry.hasTool("GET_WEATHER"))
        assertTrue(toolRegistry.hasTool("SET_TIMER"))
        assertTrue(toolRegistry.hasTool("SEND_WHATSAPP_MESSAGE"))
        assertTrue(toolRegistry.hasTool("SEND_TELEGRAM_MESSAGE"))
        assertTrue(toolRegistry.hasTool("ANALYZE_PHOTO"))

        val weatherTool = toolRegistry.getTool("GET_WEATHER")
        assertTrue(weatherTool is GetWeatherTool)

        val timerTool = toolRegistry.getTool("SET_TIMER")
        assertTrue(timerTool is SetTimerTool)

        val wpTool = toolRegistry.getTool("SEND_WHATSAPP_MESSAGE")
        assertTrue(wpTool is SendWhatsAppMessageTool)

        val tgTool = toolRegistry.getTool("SEND_TELEGRAM_MESSAGE")
        assertTrue(tgTool is SendTelegramMessageTool)

        val visionTool = toolRegistry.getTool("ANALYZE_PHOTO")
        assertTrue(visionTool is AnalyzePhotoTool)
    }
}
