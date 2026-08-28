package com.example.jarvis.ai.provider

import com.example.jarvis.core.JarvisResult
import com.example.jarvis.domain.model.AIProviderType
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.GenerationResponse
import com.example.jarvis.domain.model.ProviderHealth
import com.example.jarvis.domain.model.StructuredIntent
import kotlinx.coroutines.flow.Flow

class FallbackProvider(
    private val localSLM: LocalSLMProvider,
    private val geminiProvider: GeminiProvider,
    var isCloudFallbackEnabled: Boolean = true
) : AIProvider {

    override val providerType: AIProviderType = AIProviderType.FALLBACK_HYBRID
    override val modelName: String = "Hybrid [${localSLM.modelName} -> ${geminiProvider.modelName}]"

    override suspend fun classifyIntent(query: String): StructuredIntent {
        // Deterministic & Local SLM is always primary to minimize latency and save RAM/battery
        val localIntent = localSLM.classifyIntent(query)
        if (localIntent.isDeterministic || localIntent.intentId != "GENERAL_CHAT") {
            return localIntent
        }
        return localIntent
    }

    override suspend fun extractArguments(intentId: String, query: String): Map<String, String> {
        return localSLM.extractArguments(intentId, query)
    }

    override suspend fun generate(
        prompt: String,
        context: List<ConversationMessage>
    ): JarvisResult<GenerationResponse> {
        // Try Cloud if enabled and online
        if (isCloudFallbackEnabled) {
            val geminiResult = geminiProvider.generate(prompt, context)
            if (geminiResult is JarvisResult.Success) {
                return geminiResult
            }
        }
        // Fallback to local SLM provider
        return localSLM.generate(prompt, context)
    }

    override fun stream(
        prompt: String,
        context: List<ConversationMessage>
    ): Flow<String> {
        return localSLM.stream(prompt, context)
    }

    override suspend fun healthCheck(): ProviderHealth {
        val localHealth = localSLM.healthCheck()
        val geminiHealth = geminiProvider.healthCheck()

        return ProviderHealth(
            providerType = AIProviderType.FALLBACK_HYBRID,
            isAvailable = localHealth.isAvailable,
            latencyMs = localHealth.latencyMs,
            modelName = modelName,
            statusDetail = "Lokal SLM: ${localHealth.statusDetail} | Bulud: ${geminiHealth.statusDetail}"
        )
    }
}
