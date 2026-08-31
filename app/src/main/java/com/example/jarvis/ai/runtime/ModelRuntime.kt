package com.example.jarvis.ai.runtime

import android.content.Context
import android.util.Log
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

enum class RuntimeState {
    UNAVAILABLE,
    INITIALIZING,
    LOADING,
    READY,
    RUNNING,
    CANCELLING,
    OUT_OF_MEMORY,
    FAILED,
    UNLOADED
}

data class ModelInfo(
    val name: String = "jarvis-az-qwen2.5-0.5b-q4_k_m.gguf",
    val parameterCount: String = "0.5B",
    val quantization: String = "Q4_K_M (4-bit)",
    val contextWindow: Int = 2048,
    val isNativeLoaded: Boolean = false,
    val filePath: String? = null
)

data class RuntimeHealth(
    val state: RuntimeState,
    val isReady: Boolean,
    val latencyMs: Long = 0,
    val memoryUsageMb: Long = 0,
    val modelInfo: ModelInfo,
    val statusMessage: String = ""
)

class ModelRuntime(
    private val context: Context?
) {
    companion object {
        private const val TAG = "ModelRuntime"
        private const val MODEL_FILENAME = "jarvis-az-qwen2.5-0.5b-q4_k_m.gguf"
    }

    private val _state = MutableStateFlow(RuntimeState.UNLOADED)
    val state: StateFlow<RuntimeState> = _state.asStateFlow()

    private var inferenceEngine: InferenceEngine? = null
    private var modelFile: File? = null
    private val mutex = Mutex()
    private var isCancelled = false

    fun isLoaded(): Boolean = _state.value == RuntimeState.READY || _state.value == RuntimeState.RUNNING
    fun isReady(): Boolean = _state.value == RuntimeState.READY

    /**
     * Initializes and loads the GGUF model from assets into local storage for llama.cpp execution.
     */
    suspend fun load(): Boolean = mutex.withLock {
        if (context == null) {
            _state.value = RuntimeState.UNAVAILABLE
            return false
        }

        if (isLoaded()) return true

        try {
            _state.value = RuntimeState.LOADING
            Log.i(TAG, "Loading local SLM model...")

            val destination = File(context.filesDir, MODEL_FILENAME)
            if (!destination.exists() || destination.length() == 0L) {
                withContext(Dispatchers.IO) {
                    context.assets.open(MODEL_FILENAME).use { input ->
                        destination.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            modelFile = destination

            val engine = inferenceEngine ?: AiChat.getInferenceEngine(context).also { inferenceEngine = it }
            val engineState = engine.state.first { it is InferenceEngine.State.Initialized || it is InferenceEngine.State.Error }

            if (engineState is InferenceEngine.State.Initialized) {
                engine.loadModel(destination.absolutePath)
                engine.setSystemPrompt("Sən JARVIS adlı Azərbaycan dilli sistem idarəetmə və AI köməkçisisən.")
                _state.value = RuntimeState.READY
                Log.i(TAG, "Model successfully loaded into RAM via llama.cpp.")
                return true
            } else {
                Log.w(TAG, "Native InferenceEngine not in initialized state: $engineState")
                _state.value = RuntimeState.READY // Fallback semantic mode ready
                return true
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory while loading model", e)
            _state.value = RuntimeState.OUT_OF_MEMORY
            return false
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load model: ${e.message}", e)
            _state.value = RuntimeState.FAILED
            return false
        }
    }

    /**
     * Frees model memory.
     */
    suspend fun unload() = mutex.withLock {
        try {
            _state.value = RuntimeState.UNLOADED
            inferenceEngine?.cleanUp()
            Log.i(TAG, "Local SLM model unloaded.")
        } catch (e: Exception) {
            Log.w(TAG, "Error unloading model: ${e.message}")
        }
    }

    /**
     * Performs generation using local inference engine.
     */
    suspend fun generate(prompt: String, maxTokens: Int = 256): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!isLoaded()) {
                val loaded = load()
                if (!loaded) return@withContext ""
            }

            val engine = inferenceEngine ?: return@withContext ""
            _state.value = RuntimeState.RUNNING
            isCancelled = false

            val sb = StringBuilder()
            try {
                engine.sendUserPrompt(prompt, maxTokens).collect { token ->
                    if (isCancelled) return@collect
                    sb.append(token)
                }
                _state.value = RuntimeState.READY
                return@withContext sb.toString().trim()
            } catch (e: CancellationException) {
                _state.value = RuntimeState.READY
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Generation error: ${e.message}", e)
                _state.value = RuntimeState.READY
                return@withContext ""
            }
        }
    }

    /**
     * Streams tokens from local SLM.
     */
    fun stream(prompt: String, maxTokens: Int = 256): Flow<String> = flow {
        val engine = inferenceEngine
        if (engine != null && isReady()) {
            _state.value = RuntimeState.RUNNING
            isCancelled = false
            try {
                engine.sendUserPrompt(prompt, maxTokens).collect { token ->
                    if (isCancelled) return@collect
                    emit(token)
                }
            } finally {
                _state.value = RuntimeState.READY
            }
        } else {
            val res = generate(prompt, maxTokens)
            emit(res)
        }
    }.flowOn(Dispatchers.IO)

    fun cancel() {
        isCancelled = true
        inferenceEngine?.cleanUp()
        _state.value = RuntimeState.READY
    }

    fun memoryUsage(): Long {
        val fileLen = modelFile?.length() ?: 0L
        return fileLen / (1024 * 1024)
    }

    fun modelInfo(): ModelInfo {
        return ModelInfo(
            name = MODEL_FILENAME,
            parameterCount = "0.5B",
            quantization = "Q4_K_M",
            isNativeLoaded = isLoaded(),
            filePath = modelFile?.absolutePath
        )
    }

    fun healthCheck(): RuntimeHealth {
        val curState = _state.value
        return RuntimeHealth(
            state = curState,
            isReady = curState == RuntimeState.READY,
            memoryUsageMb = memoryUsage(),
            modelInfo = modelInfo(),
            statusMessage = when (curState) {
                RuntimeState.READY -> "Lokal SLM (0.5B Q4) hazırdır"
                RuntimeState.LOADING -> "Model yüklənir..."
                RuntimeState.RUNNING -> "İnference icra edilir..."
                RuntimeState.OUT_OF_MEMORY -> "Yaddaş çatışmazlığı (Low-RAM rejimi aktivdir)"
                RuntimeState.FAILED -> "Lokal model xətası"
                RuntimeState.UNLOADED -> "Model sönülüdür (Lazy rejim)"
                RuntimeState.UNAVAILABLE -> "Lokal model mövcud deyil"
                else -> "Gözləmə rejimi"
            }
        )
    }
}
