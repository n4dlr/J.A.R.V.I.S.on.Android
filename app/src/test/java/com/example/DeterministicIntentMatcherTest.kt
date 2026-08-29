package com.example

import com.example.jarvis.ai.matcher.DeterministicIntentMatcher
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeterministicIntentMatcherTest {

    private lateinit var matcher: DeterministicIntentMatcher

    @Before
    fun setUp() {
        matcher = DeterministicIntentMatcher(AzerbaijaniTextNormalizer())
    }

    @Test
    fun `match lock screen commands correctly`() {
        val testPhrases = listOf(
            "telefonu kilidlə",
            "ekranı bağla",
            "telefonumu kilidle",
            "lock et",
            "ekranı kilidle"
        )

        for (phrase in testPhrases) {
            val intent = matcher.match(phrase)
            assertNotNull("Phrase failed: '$phrase'", intent)
            assertEquals("LOCK_SCREEN", intent?.intentId)
            assertTrue(intent?.isDeterministic == true)
        }
    }

    @Test
    fun `match battery query accurately`() {
        val batteryQueries = listOf(
            "batareya nə qədərdir",
            "zaryadka neçədir",
            "zaryatka faizini göstər",
            "batareya"
        )

        for (query in batteryQueries) {
            val intent = matcher.match(query)
            assertNotNull("Query failed: '$query'", intent)
            assertEquals("GET_BATTERY", intent?.intentId)
        }
    }

    @Test
    fun `match torch commands and extract arguments`() {
        val onIntent = matcher.match("fənəri yandır")
        assertNotNull(onIntent)
        assertEquals("TORCH", onIntent?.intentId)
        assertEquals("ON", onIntent?.arguments?.get("state"))

        val offIntent = matcher.match("fənəri söndür")
        assertNotNull(offIntent)
        assertEquals("TORCH", offIntent?.intentId)
        assertEquals("OFF", offIntent?.arguments?.get("state"))
    }

    @Test
    fun `match ram and storage queries`() {
        val ramIntent = matcher.match("RAM nə qədərdir?")
        assertEquals("GET_RAM", ramIntent?.intentId)

        val storageIntent = matcher.match("Yaddaşda nə qədər boş yer var?")
        assertEquals("GET_STORAGE", storageIntent?.intentId)
    }

    @Test
    fun `match open app command and inflections`() {
        val appIntent1 = matcher.match("YouTube aç")
        assertNotNull(appIntent1)
        assertEquals("OPEN_APP", appIntent1?.intentId)
        assertEquals("youtube", appIntent1?.arguments?.get("app_name"))

        val appIntent2 = matcher.match("WhatsApp-ı aç")
        assertNotNull(appIntent2)
        assertEquals("OPEN_APP", appIntent2?.intentId)
        assertEquals("whatsapp", appIntent2?.arguments?.get("app_name"))
    }

    @Test
    fun `match system settings and connectivity commands`() {
        val wifiIntent = matcher.match("WiFi parametrlərini aç")
        assertNotNull(wifiIntent)
        assertEquals("WIFI_SETTINGS", wifiIntent?.intentId)

        val btIntent = matcher.match("Bluetooth-u aç")
        assertNotNull(btIntent)
        assertEquals("BLUETOOTH_SETTINGS", btIntent?.intentId)
    }

    @Test
    fun `match media playback controls`() {
        val pauseIntent = matcher.match("Media-nı dayandır")
        assertNotNull(pauseIntent)
        assertEquals("MEDIA_PAUSE", pauseIntent?.intentId)

        val nextIntent = matcher.match("Sonrakı mahnıya keç")
        assertNotNull(nextIntent)
        assertEquals("MEDIA_NEXT", nextIntent?.intentId)
    }

    @Test
    fun `match accessibility and screen reading commands`() {
        val readScreenIntent = matcher.match("Ekrandakı mətni oxu")
        assertNotNull(readScreenIntent)
        assertEquals("READ_VISIBLE_TEXT", readScreenIntent?.intentId)

        val notifIntent = matcher.match("Son bildirişi oxu")
        assertNotNull(notifIntent)
        assertEquals("READ_NOTIFICATIONS", notifIntent?.intentId)
    }
}
