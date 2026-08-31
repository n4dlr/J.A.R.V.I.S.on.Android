package com.example.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SpeechRecognitionState {
    IDLE,
    LISTENING,
    TRANSCRIBING,
    ERROR
}

enum class AssistantExecutionState {
    IDLE,
    LISTENING,
    TRANSCRIBING,
    UNDERSTANDING,
    PLANNING,
    EXECUTING,
    VERIFYING,
    SUCCESS,
    FAILED,
    RECOVERING
}

interface SpeechProvider {
    val name: String
    fun isAvailable(): Boolean
    fun startListening(locale: String, onPartial: (String) -> Unit, onResult: (String) -> Unit, onError: (String) -> Unit)
    fun stopListening()
    fun cancel()
}

class AndroidSpeechRecognizerProvider(
    private val context: Context
) : SpeechProvider {
    companion object {
        private const val TAG = "SpeechRecognizer"
    }

    override val name: String = "Android System SpeechRecognizer"

    private var speechRecognizer: SpeechRecognizer? = null

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun startListening(
        locale: String,
        onPartial: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        cancel()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        val errMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio qeydiyyat xətası"
                            SpeechRecognizer.ERROR_CLIENT -> "Müştəri xətası"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon icazəsi yoxdur"
                            SpeechRecognizer.ERROR_NETWORK -> "Şəbəkə xətası (STT üçün bağlantı tələb olunur)"
                            SpeechRecognizer.ERROR_NO_MATCH -> "Səs tanınmadı, zəhmət olmasa təkrar edin"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Səs xidməti məşğuldur"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Səs eşidilmədi"
                            else -> "Nitq tanınarkən xəta: $error"
                        }
                        Log.w(TAG, "STT onError: $errMsg (code $error)")
                        onError(errMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            onResult(text.trim())
                        } else {
                            onError("Nitq tanınmadı.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            onPartial(text.trim())
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, locale)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onError("Səs qəbulu başladıla bilmədi: ${e.message}")
        }
    }

    override fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
    }

    override fun cancel() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
    }
}

class SpeechEngine(
    private val context: Context,
    private val voskModelManager: VoskModelManager? = null
) {
    private val androidProvider = AndroidSpeechRecognizerProvider(context)
    private val voskProvider: VoskOfflineSpeechProvider? =
        voskModelManager?.let { VoskOfflineSpeechProvider(context, it) }

    private val _recognitionState = MutableStateFlow(SpeechRecognitionState.IDLE)
    val recognitionState: StateFlow<SpeechRecognitionState> = _recognitionState.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentLocale = MutableStateFlow("az-AZ")
    val currentLocale: StateFlow<String> = _currentLocale.asStateFlow()

    private val _activeProviderName = MutableStateFlow("Android STT")
    val activeProviderName: StateFlow<String> = _activeProviderName.asStateFlow()

    val isListening: Boolean get() = _recognitionState.value == SpeechRecognitionState.LISTENING

    fun setLocale(localeTag: String) {
        _currentLocale.value = localeTag
    }

    /**
     * Selects the best available STT provider:
     *  1. Android System STT — when internet is available (highest accuracy)
     *  2. Vosk Offline — when internet is not available and model is ready
     *  3. Android System STT — final fallback (will show ERROR_NETWORK if offline)
     */
    private fun selectProvider(): SpeechProvider {
        val hasInternet = isInternetAvailable()
        val voskReady = voskProvider?.isAvailable() == true

        return when {
            hasInternet -> {
                _activeProviderName.value = "Android STT (Online)"
                androidProvider
            }
            voskReady -> {
                _activeProviderName.value = "Vosk Offline (az-AZ)"
                voskProvider!!
            }
            else -> {
                _activeProviderName.value = "Android STT (Fallback)"
                androidProvider
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                    as android.net.ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (_: Exception) { true }
    }

    fun startListening(onFinalResult: (String) -> Unit) {
        val provider = selectProvider()

        if (!provider.isAvailable() && provider == androidProvider) {
            _errorMessage.value = "Bu cihazda nitqin tanınması xidməti tapılmadı."
            _recognitionState.value = SpeechRecognitionState.ERROR
            return
        }

        cancel()
        _recognitionState.value = SpeechRecognitionState.LISTENING
        _partialTranscript.value = ""
        _errorMessage.value = null

        provider.startListening(
            locale = _currentLocale.value,
            onPartial = { partial ->
                _partialTranscript.value = partial
                _recognitionState.value = SpeechRecognitionState.TRANSCRIBING
            },
            onResult = { finalResult ->
                _recognitionState.value = SpeechRecognitionState.IDLE
                _partialTranscript.value = ""
                _errorMessage.value = null
                onFinalResult(finalResult)
            },
            onError = { error ->
                // If online STT fails with a network error, try Vosk as fallback
                if (error.contains("Şəbəkə") && voskProvider?.isAvailable() == true) {
                    _activeProviderName.value = "Vosk Offline (az-AZ) [Fallback]"
                    voskProvider.startListening(
                        locale = _currentLocale.value,
                        onPartial = { p -> _partialTranscript.value = p },
                        onResult = { r ->
                            _recognitionState.value = SpeechRecognitionState.IDLE
                            _partialTranscript.value = ""
                            onFinalResult(r)
                        },
                        onError = { e ->
                            _recognitionState.value = SpeechRecognitionState.ERROR
                            _errorMessage.value = e
                            cancel()
                        }
                    )
                } else {
                    _recognitionState.value = SpeechRecognitionState.ERROR
                    _errorMessage.value = error
                    _partialTranscript.value = ""
                    cancel()
                }
            }
        )
    }

    fun stopListening() {
        androidProvider.stopListening()
        voskProvider?.stopListening()
        _recognitionState.value = SpeechRecognitionState.IDLE
    }

    fun cancel() {
        androidProvider.cancel()
        voskProvider?.cancel()
        _recognitionState.value = SpeechRecognitionState.IDLE
        _partialTranscript.value = ""
    }
}
