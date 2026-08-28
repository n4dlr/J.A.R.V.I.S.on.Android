package com.example.jarvis.domain.model

enum class AIProviderType {
    LOCAL_SLM,
    GEMINI_CLOUD,
    FALLBACK_HYBRID
}

data class ProviderHealth(
    val providerType: AIProviderType,
    val isAvailable: Boolean,
    val latencyMs: Long,
    val modelName: String,
    val statusDetail: String
)

data class GenerationResponse(
    val text: String,
    val providerType: AIProviderType,
    val isComplete: Boolean = true,
    val finishReason: String? = null
)
