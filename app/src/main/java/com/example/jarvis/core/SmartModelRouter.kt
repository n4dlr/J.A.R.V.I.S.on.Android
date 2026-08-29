package com.example.jarvis.core

import com.example.jarvis.agent.AgentPlanner
import com.example.jarvis.ai.matcher.DeterministicIntentMatcher
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.ai.provider.AIProvider
import com.example.jarvis.domain.model.IntentConfidence
import com.example.jarvis.domain.model.StructuredIntent

enum class RoutingTarget {
    CACHE,
    DETERMINISTIC,
    AGENT_PLANNER,
    LOCAL_SLM,
    GEMINI_CLOUD,
    FALLBACK
}

data class RoutingDecision(
    val target: RoutingTarget,
    val structuredIntent: StructuredIntent?,
    val isMultiStepPlan: Boolean = false,
    val reason: String = ""
)

class SmartModelRouter(
    private val commandCache: CommandCache,
    private val deterministicMatcher: DeterministicIntentMatcher,
    private val agentPlanner: AgentPlanner,
    private val localSLMProvider: AIProvider,
    private val geminiProvider: AIProvider,
    private val normalizer: AzerbaijaniTextNormalizer = AzerbaijaniTextNormalizer()
) {

    suspend fun route(rawQuery: String, isOnline: Boolean): RoutingDecision {
        val normalized = normalizer.normalize(rawQuery)

        // 1. FAST COMMAND: Check Semantic Command Cache (< 1ms)
        val cached = commandCache.get(rawQuery)
        if (cached != null) {
            return RoutingDecision(
                target = RoutingTarget.CACHE,
                structuredIntent = cached,
                reason = "Sürətli keşdən aşkar edildi."
            )
        }

        // 2. FAST COMMAND: Check Deterministic Regex Matcher (< 2ms)
        val deterministic = deterministicMatcher.match(rawQuery)
        if (deterministic != null && (deterministic.isDeterministic || deterministic.confidence == IntentConfidence.EXACT_DETERMINISTIC || deterministic.confidence == IntentConfidence.HIGH_HEURISTIC)) {
            commandCache.put(rawQuery, deterministic)
            return RoutingDecision(
                target = RoutingTarget.DETERMINISTIC,
                structuredIntent = deterministic,
                reason = "Deterministik qayda ilə aşkar edildi."
            )
        }

        // 3. COMPLEX REASONING: Check Agent Planner
        if (agentPlanner.shouldPlan(rawQuery)) {
            return RoutingDecision(
                target = RoutingTarget.AGENT_PLANNER,
                structuredIntent = null,
                isMultiStepPlan = true,
                reason = "Mürəkkəb diaqnostik planlaşdırma tələb olunur."
            )
        }

        // 4. NORMAL COMMAND: Local SLM Classification
        val localIntent = localSLMProvider.classifyIntent(rawQuery)
        if (localIntent.confidence != IntentConfidence.UNKNOWN && localIntent.confidence != IntentConfidence.LOW_AMBIGUOUS) {
            commandCache.put(rawQuery, localIntent)
            return RoutingDecision(
                target = RoutingTarget.LOCAL_SLM,
                structuredIntent = localIntent,
                reason = "Lokal SLM vasitəsilə təsnif edildi."
            )
        }

        // 5. LOW CONFIDENCE: Cloud Gemini if online
        if (isOnline) {
            val cloudIntent = geminiProvider.classifyIntent(rawQuery)
            if (cloudIntent.confidence != IntentConfidence.UNKNOWN) {
                return RoutingDecision(
                    target = RoutingTarget.GEMINI_CLOUD,
                    structuredIntent = cloudIntent,
                    reason = "Bulud modeli (Gemini) vasitəsilə təsnif edildi."
                )
            }
        }

        // 6. FALLBACK (Local Unknown / Generative)
        return RoutingDecision(
            target = RoutingTarget.FALLBACK,
            structuredIntent = localIntent,
            reason = "Ümumi dialoq və ya lokal köməkçi cavabı."
        )
    }
}
