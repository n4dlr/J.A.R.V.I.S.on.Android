package com.example.jarvis.domain.model

enum class IntentConfidence {
    EXACT_DETERMINISTIC,
    HIGH_HEURISTIC,
    SLM_CLASSIFIED,
    GEMINI_CLASSIFIED,
    LOW_AMBIGUOUS,
    UNKNOWN
}

data class StructuredIntent(
    val intentId: String,
    val rawQuery: String,
    val normalizedQuery: String,
    val confidence: IntentConfidence,
    val arguments: Map<String, String> = emptyMap(),
    val extractedEntities: List<String> = emptyList(),
    val isDeterministic: Boolean = false
) {
    companion object {
        fun unknown(raw: String, normalized: String): StructuredIntent =
            StructuredIntent(
                intentId = "UNKNOWN",
                rawQuery = raw,
                normalizedQuery = normalized,
                confidence = IntentConfidence.UNKNOWN,
                isDeterministic = false
            )

        fun chat(raw: String, normalized: String): StructuredIntent =
            StructuredIntent(
                intentId = "GENERAL_CHAT",
                rawQuery = raw,
                normalizedQuery = normalized,
                confidence = IntentConfidence.HIGH_HEURISTIC,
                isDeterministic = false
            )
    }
}
