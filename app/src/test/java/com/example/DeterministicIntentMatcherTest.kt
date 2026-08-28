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
    fun `match open app command`() {
        val appIntent = matcher.match("YouTube aç")
        assertNotNull(appIntent)
        assertEquals("OPEN_APP", appIntent?.intentId)
        assertEquals("youtube", appIntent?.arguments?.get("app_name"))
    }
}
