package com.example.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceRecognizerHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsAudioLevel = MutableStateFlow(0f)
    val rmsAudioLevel: StateFlow<Float> = _rmsAudioLevel.asStateFlow()

    private val _speechError = MutableStateFlow<String?>(null)
    val speechError: StateFlow<String?> = _speechError.asStateFlow()

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(onResult: (String) -> Unit) {
        if (!isRecognitionAvailable()) {
            _speechError.value = "Bu cihazda nitqin tanınması xidməti tapılmadı."
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _speechError.value = null
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _rmsAudioLevel.value = (rmsdB.coerceIn(0f, 10f) / 10f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _rmsAudioLevel.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _rmsAudioLevel.value = 0f
                        val errMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio qeydiyyat xətası"
                            SpeechRecognizer.ERROR_CLIENT -> "Müştəri xətası"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon icazəsi yoxdur"
                            SpeechRecognizer.ERROR_NETWORK -> "Şəbəkə xətası"
                            SpeechRecognizer.ERROR_NO_MATCH -> "Səs tanınmadı, zəhmət olmasa təkrar edin"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Tanıma xidməti məşğuldur"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Səs eşidilmədi"
                            else -> "Nitq tanınarkən xəta: $error"
                        }
                        _speechError.value = errMsg
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _rmsAudioLevel.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            onResult(text)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            // Partial audio feedback if needed
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "az-AZ")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "az-AZ")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "az-AZ")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            _speechError.value = "Səs qəbulu başladıla bilmədi: ${e.message}"
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
        _isListening.value = false
        _rmsAudioLevel.value = 0f
    }
}
