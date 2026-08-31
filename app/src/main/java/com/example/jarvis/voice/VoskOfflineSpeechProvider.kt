package com.example.jarvis.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Vosk offline speech-to-text provider for Azerbaijani language.
 *
 * Uses the Vosk Android SDK with the `vosk-model-small-az` model (~50 MB).
 * This is the SpeechProvider implementation for completely offline recognition.
 *
 * SETUP:
 *   The model must be downloaded via VoskModelManager to:
 *   context.filesDir/vosk-model-az/
 *
 * If the Vosk AAR is not in the classpath (dependency not added),
 * all methods degrade gracefully to no-ops.
 */
class VoskOfflineSpeechProvider(
    private val context: Context,
    private val modelManager: VoskModelManager
) : SpeechProvider {

    companion object {
        private const val TAG = "VoskOfflineSTT"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(8192)
    }

    override val name: String = "Vosk Offline STT (az-AZ)"

    private var audioRecord: AudioRecord? = null
    private var isRunning = false

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    override fun isAvailable(): Boolean {
        return try {
            Class.forName("org.vosk.Model")
            modelManager.isModelReady()
        } catch (_: ClassNotFoundException) {
            Log.w(TAG, "Vosk library not in classpath — dependency not added yet")
            false
        } catch (_: Exception) {
            false
        }
    }

    override fun startListening(
        locale: String,
        onPartial: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isAvailable()) {
            onError("Vosk model yüklənməyib. Tənzimləmələrdən modeli yükləyin.")
            return
        }

        // Launch in a new thread — AudioRecord requires blocking read loop
        isRunning = true
        _isRecording.value = true

        Thread({
            runRecognitionLoop(onPartial, onResult, onError)
        }, "VoskSTTThread").start()
    }

    private fun runRecognitionLoop(
        onPartial: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Use reflection to avoid hard compile dependency on Vosk AAR
            val modelPath = modelManager.getModelPath()
            val modelClass = Class.forName("org.vosk.Model")
            val recognizerClass = Class.forName("org.vosk.KaldiRecognizer")

            val model = modelClass.getConstructor(String::class.java).newInstance(modelPath)
            val recognizer = recognizerClass.getConstructor(modelClass, Float::class.java)
                .newInstance(model, SAMPLE_RATE.toFloat())

            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                onError("Mikrofon başladıla bilmədi.")
                return
            }

            audioRecord = record
            record.startRecording()

            val buffer = ShortArray(BUFFER_SIZE / 2)

            while (isRunning) {
                val read = record.read(buffer, 0, buffer.size)
                if (read <= 0) continue

                val bytes = shortsToBytes(buffer, read)

                val acceptWaveFormMethod = recognizerClass.getMethod("acceptWaveForm", ByteArray::class.java, Int::class.java)
                val accepted = acceptWaveFormMethod.invoke(recognizer, bytes, bytes.size) as Boolean

                if (accepted) {
                    val resultMethod = recognizerClass.getMethod("getResult")
                    val resultJson = resultMethod.invoke(recognizer) as String
                    val text = parseVoskResult(resultJson)
                    if (text.isNotBlank()) {
                        isRunning = false
                        onResult(text)
                    }
                } else {
                    val partialMethod = recognizerClass.getMethod("getPartialResult")
                    val partialJson = partialMethod.invoke(recognizer) as String
                    val partial = parseVoskPartial(partialJson)
                    if (partial.isNotBlank()) onPartial(partial)
                }
            }

            // Finalize
            val finalMethod = recognizerClass.getMethod("getFinalResult")
            val finalJson = finalMethod.invoke(recognizer) as String
            val finalText = parseVoskResult(finalJson)
            if (finalText.isNotBlank()) onResult(finalText)

            record.stop()
            record.release()
            audioRecord = null
            _isRecording.value = false

        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "Vosk AAR not in classpath: ${e.message}")
            onError("Vosk kitabxanası tapılmadı.")
            _isRecording.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Vosk recognition error: ${e.message}", e)
            onError("Vosk xətası: ${e.message}")
            _isRecording.value = false
        }
    }

    override fun stopListening() {
        isRunning = false
        audioRecord?.stop()
    }

    override fun cancel() {
        isRunning = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _isRecording.value = false
    }

    private fun shortsToBytes(shorts: ShortArray, count: Int): ByteArray {
        val bytes = ByteArray(count * 2)
        for (i in 0 until count) {
            bytes[i * 2] = (shorts[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (shorts[i].toInt() shr 8 and 0xFF).toByte()
        }
        return bytes
    }

    private fun parseVoskResult(json: String): String {
        return try {
            val obj = org.json.JSONObject(json)
            obj.optString("text", "").trim()
        } catch (_: Exception) { "" }
    }

    private fun parseVoskPartial(json: String): String {
        return try {
            val obj = org.json.JSONObject(json)
            obj.optString("partial", "").trim()
        } catch (_: Exception) { "" }
    }
}
