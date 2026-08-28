package com.example.jarvis.ai.provider

import com.example.BuildConfig
import com.example.jarvis.core.JarvisResult
import com.example.jarvis.domain.model.AIProviderType
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.GenerationResponse
import com.example.jarvis.domain.model.IntentConfidence
import com.example.jarvis.domain.model.ProviderHealth
import com.example.jarvis.domain.model.StructuredIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiProvider(
    private val runtimeApiKey: () -> String = { "" },
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) : AIProvider {

    override val providerType: AIProviderType = AIProviderType.GEMINI_CLOUD
    override val modelName: String = "gemini-3.5-flash"

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    override suspend fun generate(
        prompt: String,
        context: List<ConversationMessage>
    ): JarvisResult<GenerationResponse> = withContext(Dispatchers.IO) {
        val apiKey = runtimeApiKey().ifBlank {
            try { BuildConfig.GEMINI_API_KEY } catch (_: Throwable) { "" }
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext JarvisResult.Error("Gemini API açarı təyin edilməyib və ya boşdur.")
        }

        try {
            val systemInstruction = "Sən Android üçün 'JARVIS' adlı ağıllı şəxsi köməkçisən. İstifadəçiyə Azərbaycan dilində qısa, dəqiq və texniki cavablar ver."

            val contentsArray = JSONArray()
            // Add short bounded context turns
            context.takeLast(4).forEach { msg ->
                val role = if (msg.sender == com.example.jarvis.domain.model.MessageSender.USER) "user" else "model"
                contentsArray.put(
                    JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                    }
                )
            }

            // Current prompt
            contentsArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }
            )

            val jsonBody = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 512)
                })
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext JarvisResult.Error("Gemini xətası: HTTP ${response.code} $responseBody")
            }

            val json = JSONObject(responseBody)
            val text = json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text", "Cavab tapılmadı.") ?: "Cavab boşdur."

            JarvisResult.Success(
                GenerationResponse(
                    text = text.trim(),
                    providerType = AIProviderType.GEMINI_CLOUD,
                    isComplete = true
                )
            )
        } catch (e: Exception) {
            JarvisResult.Error("Gemini əlaqə xətası: ${e.message}", e)
        }
    }

    override fun stream(
        prompt: String,
        context: List<ConversationMessage>
    ): Flow<String> = flow {
        val result = generate(prompt, context)
        if (result is JarvisResult.Success) {
            emit(result.data.text)
        } else if (result is JarvisResult.Error) {
            emit("Bulud xətası: ${result.message}")
        }
    }

    override suspend fun classifyIntent(query: String): StructuredIntent {
        // Fallback or LLM-based intent extraction
        return StructuredIntent(
            intentId = "GENERAL_CHAT",
            rawQuery = query,
            normalizedQuery = query.lowercase().trim(),
            confidence = IntentConfidence.GEMINI_CLASSIFIED,
            isDeterministic = false
        )
    }

    override suspend fun extractArguments(intentId: String, query: String): Map<String, String> {
        return emptyMap()
    }

    override suspend fun healthCheck(): ProviderHealth {
        val apiKey = runtimeApiKey().ifBlank {
            try { BuildConfig.GEMINI_API_KEY } catch (_: Throwable) { "" }
        }
        val hasKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"
        return ProviderHealth(
            providerType = AIProviderType.GEMINI_CLOUD,
            isAvailable = hasKey,
            latencyMs = 120,
            modelName = modelName,
            statusDetail = if (hasKey) "Hazır (API Key mövcuddur)" else "API Key yoxdur (Offline Rejim aktivdir)"
        )
    }
}
