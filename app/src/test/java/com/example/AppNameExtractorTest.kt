package com.example

import com.example.jarvis.ai.matcher.AppNameExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AppNameExtractorTest {

    @Test
    fun `extract strips Azerbaijani locative suffixes from app names`() {
        assertEquals("youtube", AppNameExtractor.extract("YouTube-da"))
        assertEquals("youtube", AppNameExtractor.extract("youtube-da"))
        assertEquals("spotify", AppNameExtractor.extract("Spotify-də"))
        assertEquals("whatsapp", AppNameExtractor.extract("WhatsApp-da"))
        assertEquals("telegram", AppNameExtractor.extract("Telegram-da"))
        assertEquals("chrome", AppNameExtractor.extract("Chrome-da"))
    }

    @Test
    fun `extract strips dative, genitive, accusative and ablative suffixes`() {
        assertEquals("youtube", AppNameExtractor.extract("YouTube-a"))
        assertEquals("youtube", AppNameExtractor.extract("YouTube-un"))
        assertEquals("youtube", AppNameExtractor.extract("YouTube-u"))
        assertEquals("youtube", AppNameExtractor.extract("YouTube-dan"))
        assertEquals("spotify", AppNameExtractor.extract("Spotify-dən"))
        assertEquals("whatsapp", AppNameExtractor.extract("WhatsApp-a"))
        assertEquals("whatsapp", AppNameExtractor.extract("WhatsApp-ın"))
    }

    @Test
    fun `extract resolves spoken STT phonetic variants`() {
        assertEquals("youtube", AppNameExtractor.extract("yutub"))
        assertEquals("youtube", AppNameExtractor.extract("yutubda"))
        assertEquals("youtube", AppNameExtractor.extract("yutubu"))
        assertEquals("whatsapp", AppNameExtractor.extract("vatsap"))
        assertEquals("whatsapp", AppNameExtractor.extract("vatsapi"))
        assertEquals("telegram", AppNameExtractor.extract("telqram"))
        assertEquals("instagram", AppNameExtractor.extract("instaqram"))
        assertEquals("chrome", AppNameExtractor.extract("xrom"))
    }

    @Test
    fun `extractAppAndQuery parses locative query structure`() {
        val tokens = listOf("youtube-da", "inna", "caliente", "mahnisini", "ac")
        val result = AppNameExtractor.extractAppAndQuery(tokens)
        assertNotNull(result)
        assertEquals("youtube", result?.first)
        assertEquals("inna caliente mahnisini", result?.second)
    }

    @Test
    fun `extractAppAndQuery parses inverted object before app structure`() {
        val tokens = listOf("inna", "caliente", "youtube-da", "oynat")
        val result = AppNameExtractor.extractAppAndQuery(tokens)
        assertNotNull(result)
        assertEquals("youtube", result?.first)
        assertEquals("inna caliente", result?.second)
    }
}
