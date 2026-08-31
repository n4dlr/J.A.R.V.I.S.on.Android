package com.example.jarvis.domain.model

/**
 * High-level intent category for routing and execution.
 */
enum class IntentCategory {
    MEDIA,
    SYSTEM,
    COMMUNICATION,
    BROWSER,
    ACCESSIBILITY,
    APP,
    FILE,
    ALARM_REMINDER,
    DIAGNOSTIC,
    GENERAL_CHAT
}

/**
 * Concrete action to perform within an intent category.
 */
enum class CommandAction {
    SEARCH_AND_PLAY,
    PLAY,
    PAUSE,
    RESUME,
    NEXT,
    PREVIOUS,
    STOP,
    OPEN_APP,
    WEB_SEARCH,
    OPEN_URL,
    SEND_MESSAGE,
    DIAL_CALL,
    SEARCH_CONTACT,
    TOGGLE_SETTING,
    OPEN_SETTINGS,
    READ_SCREEN,
    SCROLL_SCREEN,
    CLICK_ELEMENT,
    CREATE_ALARM,
    LIST_ALARMS,
    DELETE_ALARM,
    CREATE_REMINDER,
    GET_BATTERY,
    GET_RAM,
    GET_STORAGE,
    GET_DEVICE_INFO,
    TORCH_TOGGLE,
    SET_VOLUME,
    SYSTEM_DIAGNOSTIC,
    GENERAL_CONVERSATION,
    UNKNOWN_ACTION
}

/**
 * Strategy chosen to execute the intent.
 */
enum class ExecutionStrategy {
    DIRECT_API,
    DEEP_LINK,
    ACCESSIBILITY_AUTOMATION,
    BROWSER_FALLBACK,
    CONVERSATIONAL
}

/**
 * Origin of the intent classification.
 */
enum class IntentSource {
    DETERMINISTIC_RULES,
    SEMANTIC_PARSER,
    LOCAL_SLM,
    CLOUD_GEMINI,
    CONVERSATION_CONTEXT,
    USER_MANUAL
}

/**
 * Represents an entity extracted from user input.
 */
data class ExtractedEntity(
    val name: String,
    val value: String,
    val confidence: Float = 1.0f,
    val startIndex: Int = -1,
    val endIndex: Int = -1
)

/**
 * Structured internal command representation for J.A.R.V.I.S. V2.
 * The executor never receives an unparsed sentence as an app name.
 */
data class CommandIntent(
    val intentId: String,
    val category: IntentCategory,
    val action: CommandAction,
    val targetApp: String? = null,        // e.g. "youtube", "spotify", "chrome", "whatsapp"
    val targetPackage: String? = null,    // Resolved package e.g. "com.google.android.youtube"
    val targetEntity: String? = null,     // Target contact, setting name, etc.
    val query: String? = null,            // Extracted query with original casing preserved
    val parameters: Map<String, String> = emptyMap(),
    val entities: List<ExtractedEntity> = emptyList(),
    val confidence: IntentConfidence = IntentConfidence.EXACT_DETERMINISTIC,
    val source: IntentSource = IntentSource.DETERMINISTIC_RULES,
    val executionStrategy: ExecutionStrategy = ExecutionStrategy.DIRECT_API,
    val requiresPermission: List<String> = emptyList(),
    val requiresConfirmation: Boolean = false,
    val rawQuery: String = "",
    val normalizedQuery: String = "",
    val isDeterministic: Boolean = false
) {
    fun toStructuredIntent(): StructuredIntent {
        val args = mutableMapOf<String, String>()
        args.putAll(parameters)
        targetApp?.let { args["target_app"] = it }
        targetPackage?.let { args["target_package"] = it }
        targetEntity?.let { args["target_entity"] = it }
        query?.let { args["query"] = it }
        
        return StructuredIntent(
            intentId = intentId,
            rawQuery = rawQuery,
            normalizedQuery = normalizedQuery,
            confidence = confidence,
            arguments = args,
            extractedEntities = entities.map { "${it.name}:${it.value}" },
            isDeterministic = isDeterministic
        )
    }

    companion object {
        fun unknown(raw: String, normalized: String): CommandIntent =
            CommandIntent(
                intentId = "UNKNOWN",
                category = IntentCategory.GENERAL_CHAT,
                action = CommandAction.UNKNOWN_ACTION,
                rawQuery = raw,
                normalizedQuery = normalized,
                confidence = IntentConfidence.UNKNOWN,
                source = IntentSource.DETERMINISTIC_RULES,
                executionStrategy = ExecutionStrategy.CONVERSATIONAL
            )

        fun chat(raw: String, normalized: String): CommandIntent =
            CommandIntent(
                intentId = "GENERAL_CHAT",
                category = IntentCategory.GENERAL_CHAT,
                action = CommandAction.GENERAL_CONVERSATION,
                rawQuery = raw,
                normalizedQuery = normalized,
                confidence = IntentConfidence.HIGH_HEURISTIC,
                source = IntentSource.SEMANTIC_PARSER,
                executionStrategy = ExecutionStrategy.CONVERSATIONAL
            )
    }
}
