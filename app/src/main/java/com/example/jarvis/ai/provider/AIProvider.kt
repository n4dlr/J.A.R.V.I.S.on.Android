package com.example.jarvis.ai.provider

import com.example.jarvis.core.JarvisResult
import com.example.jarvis.domain.model.AIProviderType
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.GenerationResponse
import com.example.jarvis.domain.model.ProviderHealth
import com.example.jarvis.domain.model.StructuredIntent
import kotlinx.coroutines.flow.Flow

interface AIProvider {
    val providerType: AIProviderType
    val modelName: String

    suspend fun generate(
        prompt: String,
        context: List<ConversationMessage> = emptyList()
    ): JarvisResult<GenerationResponse>

    fun stream(
        prompt: String,
        context: List<ConversationMessage> = emptyList()
    ): Flow<String>

    suspend fun classifyIntent(query: String): StructuredIntent

    suspend fun extractArguments(intentId: String, query: String): Map<String, String>

    suspend fun healthCheck(): ProviderHealth
}
