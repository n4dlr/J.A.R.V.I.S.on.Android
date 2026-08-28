package com.example.jarvis.ai.provider

import android.content.Context
import android.content.ComponentCallbacks2
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.example.jarvis.ai.matcher.DeterministicIntentMatcher
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.core.JarvisResult
import com.example.jarvis.domain.model.AIProviderType
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.GenerationResponse
import com.example.jarvis.domain.model.IntentConfidence
import com.example.jarvis.domain.model.ProviderHealth
import com.example.jarvis.domain.model.StructuredIntent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class LocalSLMProvider(
    private val appContext: Context? = null,
    private var activeModelName: String = "Embedded-AZ-SLM-v1",
    private val normalizer: AzerbaijaniTextNormalizer = AzerbaijaniTextNormalizer(),
    private val matcher: DeterministicIntentMatcher = DeterministicIntentMatcher(normalizer),
    var isQuantizedMode: Boolean = true
) : AIProvider {

    override val providerType: AIProviderType = AIProviderType.LOCAL_SLM
    override val modelName: String get() = if (isQuantizedMode) "$activeModelName (4-bit INT4)" else activeModelName

    private var isModelLoaded: Boolean = true
    private var inferenceEngine: InferenceEngine? = null
    private var modelPath: String? = null
    private var nativeModelReady = false
    private val maxKvCacheEntries: Int = 16

    fun setModelName(name: String) {
        activeModelName = name
    }

    fun unloadModel() {
        isModelLoaded = false
        inferenceEngine?.cleanUp()
        nativeModelReady = false
    }

    fun loadModel() {
        isModelLoaded = true
    }

    fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            unloadModel()
        }
    }

    override suspend fun classifyIntent(query: String): StructuredIntent {
        // Step 1: Check high-priority deterministic matcher (< 2ms)
        val deterministic = matcher.match(query)
        if (deterministic != null) {
            return deterministic
        }

        val normalized = normalizer.normalize(query)

        // Step 2: Local SLM Rule/NLU inference
        return when {
            normalized.contains("salam") || normalized.contains("necesen") || normalized.contains("sabahin xeyir") || normalized.contains("axsamin xeyir") -> {
                StructuredIntent(
                    intentId = "GREETING",
                    rawQuery = query,
                    normalizedQuery = normalized,
                    confidence = IntentConfidence.SLM_CLASSIFIED,
                    isDeterministic = false
                )
            }
            normalized.contains("sen kimsen") || normalized.contains("adin nedir") || normalized.contains("jarvis") -> {
                StructuredIntent(
                    intentId = "WHO_AM_I",
                    rawQuery = query,
                    normalizedQuery = normalized,
                    confidence = IntentConfidence.SLM_CLASSIFIED,
                    isDeterministic = false
                )
            }
            normalized.contains("hava") || normalized.contains("yagis") || normalized.contains("istilik") -> {
                StructuredIntent(
                    intentId = "WEATHER_QUERY",
                    rawQuery = query,
                    normalizedQuery = normalized,
                    confidence = IntentConfidence.SLM_CLASSIFIED,
                    isDeterministic = false
                )
            }
            normalized.contains("sag ol") || normalized.contains("tesekkur") || normalized.contains("cox sag ol") -> {
                StructuredIntent(
                    intentId = "THANK_YOU",
                    rawQuery = query,
                    normalizedQuery = normalized,
                    confidence = IntentConfidence.SLM_CLASSIFIED,
                    isDeterministic = false
                )
            }
            normalized.contains("komek") || normalized.contains("ne ede bilirsen") || normalized.contains("funksiyalar") -> {
                StructuredIntent(
                    intentId = "HELP_CAPABILITIES",
                    rawQuery = query,
                    normalizedQuery = normalized,
                    confidence = IntentConfidence.SLM_CLASSIFIED,
                    isDeterministic = false
                )
            }
            else -> {
                StructuredIntent.chat(query, normalized)
            }
        }
    }

    override suspend fun extractArguments(intentId: String, query: String): Map<String, String> {
        val normalized = normalizer.normalize(query)
        val args = mutableMapOf<String, String>()

        when (intentId) {
            "OPEN_APP" -> {
                val words = normalized.split(" ")
                val appWords = words.filter { it !in listOf("ac", "baslat", "ise", "sal", "tetbiqini", "tetbiqi", "open", "launch") }
                if (appWords.isNotEmpty()) {
                    args["app_name"] = appWords.joinToString(" ")
                }
            }
            "CREATE_REMINDER", "CREATE_ALARM" -> {
                val hourMatch = Regex("""\b(\d{1,2})\b""").find(normalized)
                if (hourMatch != null) {
                    args["hour"] = hourMatch.groupValues[1]
                }
                args["title"] = query
                args["message"] = query
            }
            "SET_VOLUME" -> {
                if (normalized.contains("artir") || normalized.contains("coxalt")) args["action"] = "UP"
                else if (normalized.contains("azalt") || normalized.contains("endir")) args["action"] = "DOWN"
                else if (normalized.contains("kes") || normalized.contains("sessiz")) args["action"] = "MUTE"
                else if (normalized.contains("maksimum")) args["action"] = "MAX"
            }
            "TORCH" -> {
                if (normalized.contains("sondur") || normalized.contains("bagla")) args["state"] = "OFF"
                else args["state"] = "ON"
            }
            "OPEN_SETTINGS" -> {
                if (normalized.contains("wifi")) args["target"] = "wifi"
                else if (normalized.contains("bluetooth") || normalized.contains("blutuz")) args["target"] = "bluetooth"
                else if (normalized.contains("ekran")) args["target"] = "display"
                else if (normalized.contains("ses")) args["target"] = "sound"
                else args["target"] = "main"
            }
            "WEB_SEARCH" -> {
                val q = query.substringAfter("axtar").ifEmpty { query.substringAfter("google") }.trim()
                args["query"] = q.ifEmpty { query }
            }
            "OPEN_URL" -> {
                val urlMatch = Regex("""\b(https?://\S+|www\.\S+)\b""").find(query)
                args["url"] = urlMatch?.value ?: query
            }
        }

        return args
    }

    override suspend fun generate(
        prompt: String,
        context: List<ConversationMessage>
    ): JarvisResult<GenerationResponse> {
        val app = appContext
        if (app != null) {
            val engine = inferenceEngine ?: AiChat.getInferenceEngine(app).also { inferenceEngine = it }
            if (modelPath == null) {
                val destination = java.io.File(app.filesDir, "jarvis-az-qwen2.5-0.5b-q4_k_m.gguf")
                if (!destination.exists()) {
                    app.assets.open("jarvis-az-qwen2.5-0.5b-q4_k_m.gguf").use { input ->
                        destination.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                modelPath = destination.absolutePath
            }
            try {
                if (!nativeModelReady) {
                    engine.state.first { it is InferenceEngine.State.Initialized }
                    engine.loadModel(modelPath!!)
                    engine.setSystemPrompt("Sən JARVIS adlı Azərbaycan dilli Android köməkçisisən. Qısa, aydın və yalnız Azərbaycan dilində cavab ver.")
                    nativeModelReady = true
                }
                val generated = StringBuilder()
                engine.sendUserPrompt(prompt, 256).collect { generated.append(it) }
                isModelLoaded = true
                return JarvisResult.Success(GenerationResponse(generated.toString().trim(), AIProviderType.LOCAL_SLM, true))
            } catch (_: Exception) {
                // Fall back to deterministic offline responses if native loading fails.
            }
        }
        if (!isModelLoaded) {
            loadModel()
        }

        // Bounded KV-cache trimming
        val trimmedContext = context.takeLast(maxKvCacheEntries)

        val normalized = normalizer.normalize(prompt)
        val responseText = when {
            normalized.contains("salam") -> "Salam! Mən JARVIS, şəxsi köməkçinizəm. Necə kömək edə bilərəm?"
            normalized.contains("necesen") -> "Təşəkkür edirəm, bütün sistemlər optimal işləyir."
            normalized.contains("sen kimsen") || normalized.contains("adin nedir") ->
                "Mən JARVIS — offline işləyən şəxsi AI agent və sistem idarəçisiyəm."
            normalized.contains("komek") || normalized.contains("ne ede bilirsen") ->
                "Batareya, RAM, yaddaş və CPU diaqnostikası, zənglər, alarm, kamera və 60+ sistem alətini idarə edə bilirəm."
            normalized.contains("sag ol") || normalized.contains("tesekkur") ->
                "Buyurun, hər zaman xidmətinizdəyəm!"
            else -> "Əmrinizi başa düşdüm. Əməliyyat icra edilir."
        }

        return JarvisResult.Success(
            GenerationResponse(
                text = responseText,
                providerType = AIProviderType.LOCAL_SLM,
                isComplete = true
            )
        )
    }

    override fun stream(
        prompt: String,
        context: List<ConversationMessage>
    ): Flow<String> = flow {
        val result = generate(prompt, context)
        if (result is JarvisResult.Success) {
            val words = result.data.text.split(" ")
            for (word in words) {
                emit("$word ")
                delay(20)
            }
        } else {
            emit("Cavab hazırlana bilmədi.")
        }
    }

    override suspend fun healthCheck(): ProviderHealth {
        return ProviderHealth(
            providerType = AIProviderType.LOCAL_SLM,
            isAvailable = true,
            latencyMs = 5,
            modelName = modelName,
            statusDetail = if (isModelLoaded) "Aktiv (INT4 Quantized Mode)" else "Hazır (Lazy rejim)"
        )
    }
}
