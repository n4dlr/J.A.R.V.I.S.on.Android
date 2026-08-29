package com.example.jarvis.context

import com.example.jarvis.domain.model.StructuredIntent

data class ActiveContextState(
    val lastIntentId: String? = null,
    val lastTopic: String? = null, // e.g. "WIFI", "BLUETOOTH", "APP", "BATTERY", "FILE", "VOLUME", "MEDIA"
    val lastEntities: Map<String, String> = emptyMap(),
    val lastApp: String? = null,
    val lastMediaQuery: String? = null,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

class ConversationContextManager {

    @Volatile
    private var currentState: ActiveContextState = ActiveContextState()

    fun updateContext(intent: StructuredIntent) {
        val topic = when {
            intent.intentId.startsWith("WIFI") -> "WIFI"
            intent.intentId.startsWith("BLUETOOTH") -> "BLUETOOTH"
            intent.intentId.startsWith("BATTERY") || intent.intentId == "GET_BATTERY" || intent.intentId == "CHARGING_STATUS" -> "BATTERY"
            intent.intentId == "OPEN_APP" || intent.intentId == "APP_INFO" || intent.intentId == "OPEN_APP_SETTINGS" -> "APP"
            intent.intentId == "MEDIA_SEARCH_PLAY" || intent.intentId.startsWith("MEDIA_") -> "MEDIA"
            intent.intentId.startsWith("SET_VOLUME") || intent.intentId == "GET_VOLUME" || intent.intentId == "MUTE" || intent.intentId == "UNMUTE" -> "VOLUME"
            intent.intentId.endsWith("_FILE") || intent.intentId == "CREATE_FOLDER" -> "FILE"
            else -> currentState.lastTopic
        }

        val app = intent.arguments["target_app"] ?: intent.arguments["app_name"] ?: currentState.lastApp
        val mediaQuery = intent.arguments["query"] ?: currentState.lastMediaQuery

        currentState = ActiveContextState(
            lastIntentId = intent.intentId,
            lastTopic = topic,
            lastEntities = intent.arguments,
            lastApp = app,
            lastMediaQuery = mediaQuery,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Resolve contextual references when user says e.g. "indi vəziyyətini yoxla",
     * "əvvəlkini bağla", "əvvəlki tətbiqə qayıt", "onu dayandır".
     */
    fun resolveContextualQuery(normalizedQuery: String): StructuredIntent? {
        // 1. Navigation / App Contextual Commands
        if (normalizedQuery.contains("evvelkini bagla") || normalizedQuery.contains("bu proqrami bagla") || normalizedQuery.contains("tetbiqi bagla")) {
            return StructuredIntent("GO_BACK", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
        }

        if (normalizedQuery.contains("evvelki tetbiqe qayit") || normalizedQuery.contains("son acdigim tetbiq") || normalizedQuery.contains("son tetbiq")) {
            return StructuredIntent("OPEN_RECENTS", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
        }

        // 2. Media Contextual Commands
        if (currentState.lastTopic == "MEDIA") {
            if (normalizedQuery.contains("dayandir") || normalizedQuery.contains("saxla") || normalizedQuery.contains("durdur") || normalizedQuery.contains("onu dayandir")) {
                return StructuredIntent("MEDIA_PAUSE", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
            }
            if (normalizedQuery.contains("davam et") || normalizedQuery.contains("oxut") || normalizedQuery.contains("baslat") || normalizedQuery.contains("oynat")) {
                return StructuredIntent("MEDIA_PLAY", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
            }
            if (normalizedQuery.contains("novbeti") || normalizedQuery.contains("sonraki") || normalizedQuery.contains("ireli")) {
                return StructuredIntent("MEDIA_NEXT", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
            }
            if (normalizedQuery.contains("evvelki") || normalizedQuery.contains("geri")) {
                return StructuredIntent("MEDIA_PREVIOUS", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
            }
        }

        val topic = currentState.lastTopic ?: return null

        // 3. Status checks for previous topic
        if (normalizedQuery.contains("veziyyet") || normalizedQuery.contains("yoxla") || normalizedQuery.contains("suret") || normalizedQuery.contains("necedir")) {
            return when (topic) {
                "WIFI" -> StructuredIntent("WIFI_STATUS", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                "BLUETOOTH" -> StructuredIntent("BLUETOOTH_STATUS", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                "BATTERY" -> StructuredIntent("GET_BATTERY", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                "VOLUME" -> StructuredIntent("GET_VOLUME", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                else -> null
            }
        }

        // 4. Settings for previous topic
        if (normalizedQuery.contains("parametr") || normalizedQuery.contains("tenzimleme") || normalizedQuery.contains("nastroyka")) {
            return when (topic) {
                "WIFI" -> StructuredIntent("WIFI_SETTINGS", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                "BLUETOOTH" -> StructuredIntent("BLUETOOTH_SETTINGS", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                "BATTERY" -> StructuredIntent("OPEN_BATTERY_SETTINGS", normalizedQuery, normalizedQuery, com.example.jarvis.domain.model.IntentConfidence.HIGH_HEURISTIC, isDeterministic = true)
                "APP" -> {
                    val appName = currentState.lastApp ?: currentState.lastEntities["app_name"] ?: ""
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
