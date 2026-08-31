package com.example.jarvis.ai.engine

import com.example.jarvis.ai.matcher.DeterministicIntentMatcher
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.ai.provider.AIProvider
import com.example.jarvis.domain.model.CommandAction
import com.example.jarvis.domain.model.CommandIntent
import com.example.jarvis.domain.model.ExecutionStrategy
import com.example.jarvis.domain.model.ExtractedEntity
import com.example.jarvis.domain.model.IntentCategory
import com.example.jarvis.domain.model.IntentConfidence
import com.example.jarvis.domain.model.IntentSource
import com.example.jarvis.domain.model.StructuredIntent
import org.json.JSONObject

data class IntentClassificationReport(
    val commandIntent: CommandIntent,
    val source: IntentSource,
    val latencyMs: Long,
    val routerDecisionReason: String
)

class IntentEngine(
    private val deterministicMatcher: DeterministicIntentMatcher,
    private val semanticLightweightEngine: SemanticLightweightEngine,
    private val localSLMProvider: AIProvider,
    private val geminiProvider: AIProvider,
    private val normalizer: AzerbaijaniTextNormalizer = AzerbaijaniTextNormalizer()
) {

    suspend fun classify(rawQuery: String, isOnline: Boolean = true): IntentClassificationReport {
        val startTime = System.currentTimeMillis()
        val normalized = normalizer.normalize(rawQuery)

        // ── LEVEL 1: Deterministic Matcher (< 2ms) ───────────────────────────
        val deterministic = deterministicMatcher.match(rawQuery)
        if (deterministic != null && (deterministic.isDeterministic || deterministic.confidence == IntentConfidence.EXACT_DETERMINISTIC)) {
            val cmd = mapStructuredToCommand(deterministic)
            return IntentClassificationReport(
                commandIntent = cmd,
                source = IntentSource.DETERMINISTIC_RULES,
                latencyMs = System.currentTimeMillis() - startTime,
                routerDecisionReason = "Sürətli deterministik qayda ilə təsnif edildi."
            )
        }

        // ── LEVEL 2: Semantic Lightweight Grammar & Entity Parser (< 5ms) ─────
        val semanticParsed = semanticLightweightEngine.parse(rawQuery)
        if (semanticParsed != null && semanticParsed.confidence != IntentConfidence.UNKNOWN) {
            return IntentClassificationReport(
                commandIntent = semanticParsed,
                source = IntentSource.SEMANTIC_PARSER,
                latencyMs = System.currentTimeMillis() - startTime,
                routerDecisionReason = "Semantik qrammatik təhlilçi ilə aşkar edildi."
            )
        }

        // ── LEVEL 3: Local SLM Inference (Structured JSON Prompt) ─────────────
        try {
            val prompt = buildIntentPrompt(rawQuery)
            val slmResult = localSLMProvider.generate(prompt, emptyList())
            val text = slmResult.getOrNull()?.text.orEmpty()
            val parsedJson = parseStructuredJson(text, rawQuery, normalized)
            if (parsedJson != null) {
                return IntentClassificationReport(
                    commandIntent = parsedJson,
                    source = IntentSource.LOCAL_SLM,
                    latencyMs = System.currentTimeMillis() - startTime,
                    routerDecisionReason = "Lokal SLM vasitəsilə JSON təsnif edildi."
                )
            }
        } catch (_: Throwable) {}

        // ── LEVEL 4: Optional Cloud Model (Gemini) ────────────────────────────
        if (isOnline) {
            try {
                val cloudIntent = geminiProvider.classifyIntent(rawQuery)
                if (cloudIntent.confidence != IntentConfidence.UNKNOWN && cloudIntent.confidence != IntentConfidence.LOW_AMBIGUOUS) {
                    val cmd = mapStructuredToCommand(cloudIntent)
                    return IntentClassificationReport(
                        commandIntent = cmd,
                        source = IntentSource.CLOUD_GEMINI,
                        latencyMs = System.currentTimeMillis() - startTime,
                        routerDecisionReason = "Bulud Gemini modeli ilə təsnif edildi."
                    )
                }
            } catch (_: Throwable) {}
        }

        // ── FALLBACK: General Chat / Unknown ─────────────────────────────────
        val fallback = CommandIntent.chat(rawQuery, normalized)
        return IntentClassificationReport(
            commandIntent = fallback,
            source = IntentSource.SEMANTIC_PARSER,
            latencyMs = System.currentTimeMillis() - startTime,
            routerDecisionReason = "Ümumi dialoq və ya qeyri-müəyyən sorğu."
        )
    }

    private fun mapStructuredToCommand(structured: StructuredIntent): CommandIntent {
        val category = when (structured.intentId) {
            "MEDIA_SEARCH_PLAY", "MEDIA_PLAY", "MEDIA_PAUSE", "MEDIA_NEXT", "MEDIA_PREVIOUS" -> IntentCategory.MEDIA
            "OPEN_APP", "LIST_APPS" -> IntentCategory.APP
            "WEB_SEARCH", "OPEN_URL", "OPEN_BROWSER" -> IntentCategory.BROWSER
            "SEND_MESSAGE_IN_APP", "COMPOSE_SMS", "DIAL_NUMBER", "OPEN_CONTACTS", "CALL_CONTACT" -> IntentCategory.COMMUNICATION
            "GET_BATTERY", "BATTERY_TEMPERATURE", "CHARGING_STATUS", "GET_RAM", "CPU_STATUS", "GET_STORAGE", "TORCH", "SET_VOLUME", "LOCK_SCREEN" -> IntentCategory.SYSTEM
            "READ_VISIBLE_TEXT", "SCROLL", "CLICK_UI_ELEMENT" -> IntentCategory.ACCESSIBILITY
            "CREATE_ALARM", "LIST_ALARMS", "DELETE_ALARM", "CREATE_REMINDER" -> IntentCategory.ALARM_REMINDER
            else -> IntentCategory.GENERAL_CHAT
        }

        val action = when (structured.intentId) {
            "MEDIA_SEARCH_PLAY" -> CommandAction.SEARCH_AND_PLAY
            "MEDIA_PLAY" -> CommandAction.PLAY
            "MEDIA_PAUSE" -> CommandAction.PAUSE
            "MEDIA_NEXT" -> CommandAction.NEXT
            "MEDIA_PREVIOUS" -> CommandAction.PREVIOUS
            "OPEN_APP" -> CommandAction.OPEN_APP
            "WEB_SEARCH" -> CommandAction.WEB_SEARCH
            "OPEN_URL" -> CommandAction.OPEN_URL
            "SEND_MESSAGE_IN_APP" -> CommandAction.SEND_MESSAGE
            "DIAL_NUMBER", "CALL_CONTACT" -> CommandAction.DIAL_CALL
            "TORCH" -> CommandAction.TORCH_TOGGLE
            "SET_VOLUME" -> CommandAction.SET_VOLUME
            "GET_BATTERY" -> CommandAction.GET_BATTERY
            "GET_RAM" -> CommandAction.GET_RAM
            "GET_STORAGE" -> CommandAction.GET_STORAGE
            "CREATE_ALARM" -> CommandAction.CREATE_ALARM
            "CREATE_REMINDER" -> CommandAction.CREATE_REMINDER
            "READ_VISIBLE_TEXT" -> CommandAction.READ_SCREEN
            else -> CommandAction.UNKNOWN_ACTION
        }

        val targetApp = structured.arguments["target_app"] ?: structured.arguments["app_name"]
        val query = structured.arguments["query"] ?: structured.arguments["url"] ?: structured.arguments["contact"]

        return CommandIntent(
            intentId = structured.intentId,
            category = category,
            action = action,
            targetApp = targetApp,
            query = query,
            parameters = structured.arguments,
            rawQuery = structured.rawQuery,
            normalizedQuery = structured.normalizedQuery,
            confidence = structured.confidence,
            source = if (structured.isDeterministic) IntentSource.DETERMINISTIC_RULES else IntentSource.SEMANTIC_PARSER,
            executionStrategy = when (category) {
                IntentCategory.MEDIA -> ExecutionStrategy.ACCESSIBILITY_AUTOMATION
                IntentCategory.BROWSER -> ExecutionStrategy.DIRECT_API
                IntentCategory.APP -> ExecutionStrategy.DIRECT_API
                else -> ExecutionStrategy.DIRECT_API
            },
            isDeterministic = structured.isDeterministic
        )
    }

    private fun buildIntentPrompt(rawQuery: String): String {
        return """
            Respond with JSON only. Classify the user command into:
            {"intent": "MEDIA_SEARCH_PLAY" | "OPEN_APP" | "WEB_SEARCH" | "TORCH" | "GET_BATTERY" | "CHAT", "target_app": "youtube" | "chrome" | "spotify" | null, "query": "..."}
            User: "$rawQuery"
            JSON:
        """.trimIndent()
    }

    private fun parseStructuredJson(text: String, rawQuery: String, normalized: String): CommandIntent? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val jsonStr = text.substring(start, end + 1)
        return try {
            val json = JSONObject(jsonStr)
            val intent = json.optString("intent", "UNKNOWN")
            val targetApp = json.optString("target_app", "").takeIf { it.isNotBlank() && it != "null" }
            val query = json.optString("query", "").takeIf { it.isNotBlank() && it != "null" }

            if (intent == "UNKNOWN" || intent.isBlank()) return null

            CommandIntent(
                intentId = intent,
                category = when (intent) {
                    "MEDIA_SEARCH_PLAY" -> IntentCategory.MEDIA
                    "OPEN_APP" -> IntentCategory.APP
                    "WEB_SEARCH" -> IntentCategory.BROWSER
                    "TORCH", "GET_BATTERY" -> IntentCategory.SYSTEM
                    else -> IntentCategory.GENERAL_CHAT
                },
                action = when (intent) {
                    "MEDIA_SEARCH_PLAY" -> CommandAction.SEARCH_AND_PLAY
                    "OPEN_APP" -> CommandAction.OPEN_APP
                    "WEB_SEARCH" -> CommandAction.WEB_SEARCH
                    "TORCH" -> CommandAction.TORCH_TOGGLE
                    "GET_BATTERY" -> CommandAction.GET_BATTERY
                    else -> CommandAction.GENERAL_CONVERSATION
                },
                targetApp = targetApp,
                query = query,
                rawQuery = rawQuery,
                normalizedQuery = normalized,
                confidence = IntentConfidence.SLM_CLASSIFIED,
                source = IntentSource.LOCAL_SLM
            )
        } catch (_: Exception) {
            null
        }
    }
}
