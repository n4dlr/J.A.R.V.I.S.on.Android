package com.example.jarvis.ai.provider

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
import kotlinx.coroutines.flow.flow

class LocalSLMProvider(
    private var activeModelName: String = "Embedded-AZ-SLM-v1",
    private val normalizer: AzerbaijaniTextNormalizer = AzerbaijaniTextNormalizer(),
    private val matcher: DeterministicIntentMatcher = DeterministicIntentMatcher(normalizer)
) : AIProvider {

    override val providerType: AIProviderType = AIProviderType.LOCAL_SLM
    override val modelName: String get() = activeModelName

    private var isModelLoaded: Boolean = true

    fun setModelName(name: String) {
        activeModelName = name
    }

    fun unloadModel() {
        isModelLoaded = false
    }

    fun loadModel() {
        isModelLoaded = true
    }

    override suspend fun classifyIntent(query: String): StructuredIntent {
        // Step 1: Check high-priority deterministic matcher
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
            "CREATE_REMINDER" -> {
                val hourMatch = Regex("""\b(\d{1,2})\b""").find(normalized)
                if (hourMatch != null) {
                    args["hour"] = hourMatch.groupValues[1]
                }
                args["title"] = query
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
        }

        return args
    }

    override suspend fun generate(
        prompt: String,
        context: List<ConversationMessage>
    ): JarvisResult<GenerationResponse> {
        if (!isModelLoaded) {
            loadModel()
        }

        val normalized = normalizer.normalize(prompt)
        val responseText = when {
            normalized.contains("salam") -> "Salam! Mən JARVIS, şəxsi köməkçinizəm. Sizə necə kömək edə bilərəm?"
            normalized.contains("necesen") -> "Təşəkkür edirəm, bütün sistemlər optimal vəziyyətdə işləyir. Siz necəsiniz?"
            normalized.contains("sen kimsen") || normalized.contains("adin nedir") ->
                "Mən JARVIS — Android 9+ üçün hazırlanmış, offline işləyən şəxsi AI assistant və sistem idarəçisiyəm."
            normalized.contains("komek") || normalized.contains("ne ede bilirsen") ->
                "Mən batareya, RAM və yaddaşı yoxlaya, fənəri və kameranı idarə edə, səsi tənzimləyə, istədiyiniz tətbiqi aça və xatırlatmalar qura bilərəm."
            normalized.contains("sag ol") || normalized.contains("tesekkur") ->
                "Buyurun, hər zaman xidmətinizdəyəm!"
            else -> "Əmrinizi başa düşdüm. Əgər konkret sistem əməliyyatı (məs: 'batareya', 'fənəri yandır', 'youtube aç') icra etmək istəyirsinizsə, buyurun deyin."
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
                delay(30)
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
            modelName = activeModelName,
            statusDetail = if (isModelLoaded) "Aktiv (Yaddaşda yüklənib)" else "Hazır (Lazy rejim)"
        )
    }
}
