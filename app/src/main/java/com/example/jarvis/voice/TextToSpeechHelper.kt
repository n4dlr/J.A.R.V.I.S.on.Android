package com.example.jarvis.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class TextToSpeechHelper(
    private val context: Context,
    private val neuralManager: NeuralTtsManager = NeuralTtsManager(context)
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var isEnabled: Boolean = false
    var isNeuralTtsPreferred: Boolean = true

    val neuralEngine = NeuralTtsEngine(context, neuralManager)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            // Attempt Azerbaijani or fallback to Turkish/English
            val azLocale = Locale("az", "AZ")
            val trLocale = Locale("tr", "TR")

            val result = tts?.setLanguage(azLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val trResult = tts?.setLanguage(trLocale)
                if (trResult == TextToSpeech.LANG_MISSING_DATA || trResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
            }

            tts?.setSpeechRate(1.05f)
            tts?.setPitch(0.95f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    fun speak(text: String, force: Boolean = false) {
        if (!force && !isEnabled) return
        if (text.isBlank()) return

        stop()

        // 1. If custom Neural Voice is ready and preferred, use Neural Synthesizer
        if (isNeuralTtsPreferred && neuralEngine.isReady()) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                neuralEngine.synthesizeAndPlay(
                    text = text,
                    onStart = { _isSpeaking.value = true },
                    onDone = { _isSpeaking.value = false }
                )
            }
            return
        }

        // 2. Standard Android TTS Fallback
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_UTTERANCE_${System.currentTimeMillis()}")
        }
    }

    fun stop() {
        try {
            neuralEngine.stop()
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    fun shutdown() {
        try {
            neuralEngine.stop()
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
        isInitialized = false
        _isSpeaking.value = false
    }
}
