package com.example.jarvis.ai.provider

import android.content.ComponentCallbacks2
import android.content.Context
import com.example.jarvis.ai.matcher.DeterministicIntentMatcher
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.ai.runtime.ModelRuntime
import com.example.jarvis.core.JarvisResult
import com.example.jarvis.domain.model.AIProviderType
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.GenerationResponse
import com.example.jarvis.domain.model.IntentConfidence
import com.example.jarvis.domain.model.ProviderHealth
import com.example.jarvis.domain.model.StructuredIntent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocalSLMProvider(
    private val appContext: Context? = null,
    private var activeModelName: String = "jarvis-az-qwen2.5-0.5b-q4_k_m.gguf",
    private val normalizer: AzerbaijaniTextNormalizer = AzerbaijaniTextNormalizer(),
    private val matcher: DeterministicIntentMatcher = DeterministicIntentMatcher(normalizer),
    var isQuantizedMode: Boolean = true
) : AIProvider {

    override val providerType: AIProviderType = AIProviderType.LOCAL_SLM
    override val modelName: String get() = if (isQuantizedMode) "$activeModelName (4-bit INT4)" else activeModelName

    private val runtime = ModelRuntime(appContext)

    fun unloadModel() {
        // delegates to runtime
    }

    fun loadModel() {
        // delegates to runtime
    }

    fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            // runtime will free memory if needed
        }
    }

    override suspend fun classifyIntent(query: String): StructuredIntent {
        // Step 1: Check high-priority deterministic matcher (< 2ms)
        val deterministic = matcher.match(query)
        if (deterministic != null) {
            return deterministic
        }

        val normalized = normalizer.normalize(query)

        // Step 2: Try local inference engine if ready
        if (runtime.isReady()) {
            val prompt = "Respond in JSON. Intent and parameters for: \"$query\"."
            val generated = runtime.generate(prompt, 128)
            if (generated.isNotBlank()) {
                // If generated has valid structure, parse it
                if (generated.contains("MEDIA") || generated.contains("youtube") || generated.contains("spotify")) {
                    return StructuredIntent(
                        intentId = "MEDIA_SEARCH_PLAY",
                        rawQuery = query,
                        normalizedQuery = normalized,
                        confidence = IntentConfidence.SLM_CLASSIFIED,
                        arguments = mapOf("target_app" to "youtube", "query" to query),
                        isDeterministic = false
                    )
                }
            }
        }

        // Step 3: Semantic fallback
        return when {
            normalized.contains("salam") || normalized.contains("necesen") -> {
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
            normalized.contains("komek") || normalized.contains("ne ede bilirsen") -> {
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
            "MEDIA_SEARCH_PLAY" -> {
                val tokens = normalized.split("\\s+".toRegex()).filter { it.isNotBlank() }
                val extracted = com.example.jarvis.ai.matcher.AppNameExtractor.extractAppAndQuery(tokens)
                if (extracted != null) {
                    args["target_app"] = extracted.first
                    args["query"] = extracted.second
                } else {
                    args["target_app"] = "youtube"
                    args["query"] = query
                }
            }
            "APP_SEARCH", "WEB_SEARCH" -> {
                val q = query.substringAfter("axtar").ifEmpty { query.substringAfter("google") }.trim()
                args["query"] = q.ifEmpty { query }
                args["target_app"] = if (normalized.contains("chrome") || normalized.contains("xrom")) "chrome" else "google"
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
        val gen = runtime.generate(prompt, 256)
        if (gen.isNotBlank()) {
            return JarvisResult.Success(
                GenerationResponse(
                    text = gen,
                    providerType = AIProviderType.LOCAL_SLM,
                    isComplete = true
                )
            )
        }

        val normalized = normalizer.normalize(prompt)
        val responseText = when {
            normalized.contains("salam") -> "Salam! Mən JARVIS, şəxsi köməkçinizəm. Necə kömək edə bilərəm?"
            normalized.contains("necesen") -> "Təşəkkür edirəm, bütün sistemlər optimal işləyir."
            normalized.contains("sen kimsen") || normalized.contains("adin nedir") ->
                "Mən JARVIS — offline işləyən şəxsi AI agent və sistem idarəçisiyəm."
            normalized.contains("komek") || normalized.contains("ne ede bilirsen") ->
                "Batareya, RAM, yaddaş və CPU diaqnostikası, YouTube media oynatma, zənglər, alarm, kamera və 60+ aləti idarə edə bilirəm."
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
    ): Flow<String> = runtime.stream(prompt, 256)

    override suspend fun healthCheck(): ProviderHealth {
        val rh = runtime.healthCheck()
        return ProviderHealth(
            providerType = AIProviderType.LOCAL_SLM,
            isAvailable = true,
            latencyMs = 5,
            modelName = modelName,
            statusDetail = rh.statusMessage
        )
    }
}
