package com.example

import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AzerbaijaniTextNormalizerTest {

    private lateinit var normalizer: AzerbaijaniTextNormalizer

    @Before
    fun setUp() {
        normalizer = AzerbaijaniTextNormalizer()
    }

    @Test
    fun `normalize replaces Azerbaijani specific letters to phonetic ASCII equivalents`() {
        val raw = "Zəhmət olmasa işığı yandır və qapını bağla"
        val normalized = normalizer.normalize(raw)
        assertEquals("zehmet olmasa feneri yandir ve qapini bagla", normalized)
    }

    @Test
    fun `normalize maps colloquial and slang keywords accurately`() {
        val rawZaryadka = "zaryatka ne qederdir"
        assertEquals("zaryadka ne qederdir", normalizer.normalize(rawZaryadka))

        val rawFanar = "fanari ac"
        assertEquals("feneri ac", normalizer.normalize(rawFanar))

        val rawOperativka = "operativka necedir"
        assertEquals("ram necedir", normalizer.normalize(rawOperativka))

        val rawBlokirofka = "ekrani blakirofka et"
        assertEquals("ekrani kilidle et", normalizer.normalize(rawBlokirofka))
    }

    @Test
    fun `fuzzy matching detects typo within distance threshold`() {
        assertTrue(normalizer.isFuzzyMatch("batarey", "batareya", 2))
        assertTrue(normalizer.isFuzzyMatch("kilidle", "kilidlə", 2))
    }
}
