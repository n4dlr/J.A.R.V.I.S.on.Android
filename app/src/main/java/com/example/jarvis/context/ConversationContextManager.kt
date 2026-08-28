package com.example.jarvis.context

import com.example.jarvis.domain.model.StructuredIntent

data class ActiveContextState(
    val lastIntentId: String? = null,
    val lastTopic: String? = null, // e.g. "WIFI", "BLUETOOTH", "APP", "BATTERY", "FILE", "VOLUME"
    val lastEntities: Map<String, String> = emptyMap(),
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

class ConversationContextManager {

    @Volatile
    private var currentState: ActiveContextState = ActiveContextState()

    fun updateContext(intent: StructuredIntent) {
        val topic = when (intent.intentId) {
            "WIFI_STATUS", "WIFI_SETTINGS" -> "WIFI"
            "BLUETOOTH_STATUS", "BLUETOOTH_SETTINGS" -> "BLUETOOTH"
            "GET_BATTERY", "BATTERY_STATUS", "BATTERY_TEMPERATURE", "CHARGING_STATUS" -> "BATTERY"
            "OPEN_APP", "APP_INFO", "OPEN_APP_SETTINGS" -> "APP"
            "SET_VOLUME", "GET_VOLUME", "MUTE", "UNMUTE" -> "VOLUME"
            "SEARCH_FILES", "OPEN_FILE", "DELETE_FILE" -> "FILE"
            else -> currentState.lastTopic
        }

        currentState = ActiveContextState(
            lastIntentId = intent.intentId,
            lastTopic = topic,
            lastEntities = intent.arguments,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Resolve contextual references when user says e.g. "indi vəziyyətini yoxla" or "parametrlərini aç".
     */
    fun resolveContextualQuery(normalizedQuery: String): StructuredIntent? {
        val topic = currentState.lastTopic ?: return null

        // "indi surətini yoxla" / "veziyyetine bax" / "yoxla"
        if (normalizedQuery.contains("veziyyet") || normalizedQuery.contains("yoxla") || normalizedQuery.contains("suret") || normalizedQuery.contains("necedir")) {
            return when (topic) {
                "WIFI" -> StructuredIntent("WIFI_STATUS", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                "BLUETOOTH" -> StructuredIntent("BLUETOOTH_STATUS", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                "BATTERY" -> StructuredIntent("GET_BATTERY", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                "VOLUME" -> StructuredIntent("GET_VOLUME", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                else -> null
            }
        }

        // "parametrlərini aç" / "nastroykasini ac"
        if (normalizedQuery.contains("parametr") || normalizedQuery.contains("tenzimleme") || normalizedQuery.contains("nastroyka")) {
            return when (topic) {
                "WIFI" -> StructuredIntent("WIFI_SETTINGS", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                "BLUETOOTH" -> StructuredIntent("BLUETOOTH_SETTINGS", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                "BATTERY" -> StructuredIntent("OPEN_BATTERY_SETTINGS", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                "APP" -> {
                    val appName = currentState.lastEntities["app_name"] ?: ""
                    StructuredIntent("OPEN_APP_SETTINGS", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, mapOf("app_name" to appName), isDeterministic = true)
                }
                else -> null
            }
        }

        return null
    }

    fun getCurrentContext(): ActiveContextState = currentState

    fun clearContext() {
        currentState = ActiveContextState()
    }
}
