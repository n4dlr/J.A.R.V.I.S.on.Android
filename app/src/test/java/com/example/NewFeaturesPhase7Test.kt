package com.example

import android.graphics.Bitmap
import com.example.jarvis.ai.vision.LocalVisionEngine
import com.example.jarvis.ai.vision.LocalVisionManager
import com.example.jarvis.voice.NeuralTtsEngine
import com.example.jarvis.voice.NeuralTtsManager
import com.example.jarvis.voice.NeuralVoiceGender
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFeaturesPhase7Test {

    // ── 1. Local Offline Vision Engine Tests ────────────────────────────────────

    @Test
    fun testLocalVisionEngineSceneAnalysis() = runBlocking {
        val dummyContext = TestContext()
        val visionManager = LocalVisionManager(dummyContext)
        val visionEngine = LocalVisionEngine(visionManager)

        val analysis = visionEngine.analyze(null, "Bu nədir?")
        assertNotNull(analysis)
        assertTrue(analysis.isNotBlank())
        assertTrue("Analysis should contain diagnostic details", analysis.contains("Offline") || analysis.contains("Lokal"))
    }

    // ── 2. Custom Azerbaijani Neural Voice Tests ────────────────────────────────

    @Test
    fun testNeuralTtsVoiceGenderSelection() {
        val dummyContext = TestContext()
        val neuralManager = NeuralTtsManager(dummyContext)

        neuralManager.activeGender = NeuralVoiceGender.JARVIS_MALE
        assertEquals(NeuralVoiceGender.JARVIS_MALE, neuralManager.activeGender)
        assertEquals("JARVIS Kişi Səsi (Studiya)", neuralManager.activeGender.titleAz)

        neuralManager.activeGender = NeuralVoiceGender.AYLA_FEMALE
        assertEquals(NeuralVoiceGender.AYLA_FEMALE, neuralManager.activeGender)
        assertEquals("Ayla Qadın Səsi (Təbii)", neuralManager.activeGender.titleAz)
    }

    @Test
    fun testNeuralTtsSynthesisExecution() = runBlocking {
        val dummyContext = TestContext()
        val neuralManager = NeuralTtsManager(dummyContext)
        val neuralEngine = NeuralTtsEngine(dummyContext, neuralManager)

        var started = false
        var completed = false

        neuralEngine.synthesizeAndPlay(
            text = "Salam, JARVIS aktivdir. RAM 50% istifadə olunur.",
            speechRate = 1.0f,
            pitch = 1.0f,
            onStart = { started = true },
            onDone = { completed = true }
        )

        assertTrue(started)
        assertTrue(completed)
    }
}
