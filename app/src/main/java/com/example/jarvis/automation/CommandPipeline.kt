package com.example.jarvis.automation

import android.content.Context
import com.example.jarvis.agent.AgentExecutor
import com.example.jarvis.agent.AgentPlanner
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.ai.provider.AIProvider
import com.example.jarvis.context.ConversationContextManager
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.ExecutionLog
import com.example.jarvis.domain.model.MessageSender
import com.example.jarvis.domain.model.PendingActionConfirmation
import com.example.jarvis.domain.model.StructuredIntent
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.domain.model.ToolStatus
import com.example.jarvis.memory.MemoryManager
import com.example.jarvis.permissions.PermissionManager
import com.example.jarvis.rag.RAGEngine
import com.example.jarvis.security.CommandSanitizer
import com.example.jarvis.security.RiskManager
import com.example.jarvis.tools.ToolRegistry
import com.example.jarvis.voice.TextToSpeechHelper

sealed class PipelineOutput {
    data class Executed(
        val query: String,
        val structuredIntent: StructuredIntent,
        val toolResult: ToolResult,
        val responseSpeech: String
    ) : PipelineOutput()

    data class ConfirmationRequired(
        val confirmation: PendingActionConfirmation
    ) : PipelineOutput()

    data class ConversationalResponse(
        val query: String,
        val structuredIntent: StructuredIntent,
        val replyText: String
    ) : PipelineOutput()

    data class Error(
        val query: String,
        val reason: String
    ) : PipelineOutput()
}

class CommandPipeline(
    private val context: Context,
    private val normalizer: AzerbaijaniTextNormalizer,
    private val aiProvider: AIProvider,
    private val toolRegistry: ToolRegistry,
    private val permissionManager: PermissionManager,
    private val riskManager: RiskManager,
    private val sanitizer: CommandSanitizer,
    private val memoryManager: MemoryManager,
    private val ttsHelper: TextToSpeechHelper,
    private val ragEngine: RAGEngine? = null,
    private val contextManager: ConversationContextManager = ConversationContextManager(),
    private val agentPlanner: AgentPlanner = AgentPlanner(normalizer),
    private val agentExecutor: AgentExecutor = AgentExecutor(toolRegistry, memoryManager)
) {

    suspend fun processCommand(rawInput: String, isConfirmed: Boolean = false): PipelineOutput {
        val startTime = System.currentTimeMillis()

        // 1. Sanitize & Normalize Input
        val sanitized = sanitizer.sanitizeInput(rawInput)
        if (sanitized.isBlank()) {
            return PipelineOutput.Error(rawInput, "Daxil edilən mətn boşdur.")
        }
        val normalized = normalizer.normalize(sanitized)

        // Add user message to conversation memory
        memoryManager.addMessage(
            ConversationMessage(
                sender = MessageSender.USER,
                text = sanitized
            )
        )

        // 2. MEMORY EXPLICIT COMMANDS: "Bunu yadda saxla" / "Bunu unut" / "Yaddaşda nə var"
        if (normalized.contains("yadda saxla") || normalized.contains("yaddasinda saxla")) {
            val factContent = sanitized.substringAfter("saxla").trim(':', ' ', '"', '\'')
                .ifEmpty { sanitized }
            memoryManager.rememberFact(factContent)
            val reply = "Yadda saxlanıldı: '$factContent'."
            ttsHelper.speak(reply)
            recordAssistantMessage(sanitized, "REMEMBER_FACT", reply, startTime)
            return PipelineOutput.ConversationalResponse(sanitized, StructuredIntent("REMEMBER_FACT", sanitized, normalized, com.example.jarvis.domain.model.IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true), reply)
        }

        if (normalized.contains("bunu unut") || normalized.contains("yaddasdan sil") || normalized.contains("unut")) {
            val target = sanitized.substringAfter("unut").ifEmpty { sanitized.substringAfter("sil") }.trim(':', ' ', '"', '\'')
            val deleted = memoryManager.forgetFact(target.ifEmpty { sanitized })
            val reply = if (deleted) "Fakt yaddaşdan silindi." else "'$target' üzrə yaddaşda heç bir fakt tapılmadı."
            ttsHelper.speak(reply)
            recordAssistantMessage(sanitized, "FORGET_FACT", reply, startTime)
            return PipelineOutput.ConversationalResponse(sanitized, StructuredIntent("FORGET_FACT", sanitized, normalized, com.example.jarvis.domain.model.IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true), reply)
        }

        if (normalized.contains("yaddasimda ne var") || normalized.contains("yaddas faktlari") || normalized == "yaddas") {
            val facts = memoryManager.searchFacts("")
            val reply = if (facts.isEmpty()) {
                "Yaddaşınızda hələ heç bir fakt qeyd edilməyib."
            } else {
                "Yaddaşınızdakı faktlar (${facts.size} ədəd):\n" + facts.mapIndexed { i, f -> "${i + 1}. ${f.value}" }.joinToString("\n")
            }
            ttsHelper.speak(reply)
            recordAssistantMessage(sanitized, "QUERY_FACTS", reply, startTime)
            return PipelineOutput.ConversationalResponse(sanitized, StructuredIntent("QUERY_FACTS", sanitized, normalized, com.example.jarvis.domain.model.IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true), reply)
        }

        // 3. MULTI-STEP AGENT PLANNING & DIAGNOSTICS
        if (agentPlanner.shouldPlan(sanitized)) {
            val plan = agentPlanner.createPlan(sanitized)
            val execResult = agentExecutor.executePlan(context, plan)
            ttsHelper.speak(execResult.summary)
            recordAssistantMessage(sanitized, "AGENT_DIAGNOSTICS", execResult.summary, startTime)
            return PipelineOutput.Executed(
                query = sanitized,
                structuredIntent = StructuredIntent("AGENT_DIAGNOSTICS", sanitized, normalized, com.example.jarvis.domain.model.IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true),
                toolResult = ToolResult.success("AGENT_DIAGNOSTICS", execResult.summary),
                responseSpeech = execResult.summary
            )
        }

        // 4. MULTI-TURN CONTEXT RESOLUTION
        var structuredIntent = contextManager.resolveContextualQuery(normalized)
            ?: aiProvider.classifyIntent(sanitized)

        contextManager.updateContext(structuredIntent)

        // 5. Check if Intent maps to a Tool
        val tool = toolRegistry.getTool(structuredIntent.intentId)

        if (tool == null) {
            // 6. RAG LOCAL KNOWLEDGE CHECK
            val ragAnswer = ragEngine?.answerIfKnowledgeAvailable(sanitized)
            if (ragAnswer != null) {
                recordAssistantMessage(sanitized, "RAG_KNOWLEDGE", ragAnswer, startTime)
                ttsHelper.speak(ragAnswer)
                return PipelineOutput.ConversationalResponse(sanitized, structuredIntent, ragAnswer)
            }

            // General conversational reply or fallback via AIProvider
            val contextMessages = memoryManager.getShortTermContext()
            val aiResponse = aiProvider.generate(sanitized, contextMessages)
            val replyText = aiResponse.getOrNull()?.text
                ?: "Sizi başa düşdüm. Əlavə bir əmriniz var?"

            recordAssistantMessage(sanitized, structuredIntent.intentId, replyText, startTime)
            ttsHelper.speak(replyText)
            return PipelineOutput.ConversationalResponse(sanitized, structuredIntent, replyText)
        }

        // 7. Extract typed parameters for Tool
        val extractedArgs = aiProvider.extractArguments(tool.id, sanitized).toMutableMap()
        extractedArgs.putAll(structuredIntent.arguments)

        // 8. Permission Verification
        val missingPerms = permissionManager.getMissingPermissions(tool.requiredPermissions)
        if (missingPerms.isNotEmpty()) {
            val permResult = ToolResult.permissionRequired(
                toolId = tool.id,
                permissions = missingPerms,
                message = "'${tool.name}' əməliyyatını icra etmək üçün tələb olunan icazələr çatışmır."
            )
            handleToolCompletion(sanitized, structuredIntent, tool.id, permResult, startTime)
            return PipelineOutput.Executed(sanitized, structuredIntent, permResult, permResult.outputMessage)
        }

        // 9. Risk Assessment & Confirmation System
        val riskAssessment = riskManager.assessRisk(structuredIntent, tool.riskLevel)
        if (riskAssessment.requiresExplicitConfirmation && !isConfirmed) {
            val pendingConfirmation = riskManager.createPendingConfirmation(tool.id, structuredIntent, riskAssessment)
            return PipelineOutput.ConfirmationRequired(pendingConfirmation)
        }

        // 10. Safe Tool Execution
        val toolResult = try {
            tool.execute(context, extractedArgs)
        } catch (e: Exception) {
            ToolResult.failed(tool.id, "İcra zamanı gözlənilməz xəta: ${e.message}")
        }

        // 11. Result handling, memory logging & TTS response
        val responseSpeech = toolResult.outputMessage
        handleToolCompletion(sanitized, structuredIntent, tool.id, toolResult, startTime)
        ttsHelper.speak(responseSpeech)

        return PipelineOutput.Executed(
            query = sanitized,
            structuredIntent = structuredIntent,
            toolResult = toolResult,
            responseSpeech = responseSpeech
        )
    }

    private suspend fun recordAssistantMessage(
        query: String,
        intentId: String,
        reply: String,
        startTime: Long
    ) {
        val duration = System.currentTimeMillis() - startTime
        memoryManager.addMessage(
            ConversationMessage(
                sender = MessageSender.JARVIS,
                text = reply,
                intentId = intentId,
                providerUsed = aiProvider.providerType
            )
        )
        memoryManager.logExecution(
            ExecutionLog(
                query = query,
                intentId = intentId,
                toolId = intentId,
                status = ToolStatus.SUCCESS,
                output = reply,
                durationMs = duration
            )
        )
    }

    private suspend fun handleToolCompletion(
        query: String,
        intent: StructuredIntent,
        toolId: String,
        result: ToolResult,
        startTime: Long
    ) {
        val durationMs = System.currentTimeMillis() - startTime

        // Save assistant message to memory
        memoryManager.addMessage(
            ConversationMessage(
                sender = MessageSender.JARVIS,
                text = result.outputMessage,
                intentId = intent.intentId,
                toolResultStatus = result.status,
                providerUsed = aiProvider.providerType
            )
        )

        // Log execution to persistent audit log
        memoryManager.logExecution(
            ExecutionLog(
                query = query,
                intentId = intent.intentId,
                toolId = toolId,
                status = result.status,
                output = result.outputMessage,
                durationMs = durationMs
            )
        )
    }
}
