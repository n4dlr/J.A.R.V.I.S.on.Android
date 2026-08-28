package com.example.jarvis.automation

import android.content.Context
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.ai.provider.AIProvider
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.ExecutionLog
import com.example.jarvis.domain.model.MessageSender
import com.example.jarvis.domain.model.PendingActionConfirmation
import com.example.jarvis.domain.model.StructuredIntent
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.domain.model.ToolStatus
import com.example.jarvis.memory.MemoryManager
import com.example.jarvis.permissions.PermissionManager
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
    private val ttsHelper: TextToSpeechHelper
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

        // 2 & 3. Intent Matching & SLM Classification
        val structuredIntent = aiProvider.classifyIntent(sanitized)

        // 4. Check if Intent maps to a Tool
        val tool = toolRegistry.getTool(structuredIntent.intentId)

        if (tool == null) {
            // General conversational reply or fallback via AIProvider
            val contextMessages = memoryManager.getShortTermContext()
            val aiResponse = aiProvider.generate(sanitized, contextMessages)
            val replyText = aiResponse.getOrNull()?.text
                ?: "Sizi başa düşdüm. Əlavə bir əmriniz var?"

            // Record assistant reply in memory
            memoryManager.addMessage(
                ConversationMessage(
                    sender = MessageSender.JARVIS,
                    text = replyText,
                    intentId = structuredIntent.intentId,
                    providerUsed = aiProvider.providerType
                )
            )

            ttsHelper.speak(replyText)
            return PipelineOutput.ConversationalResponse(sanitized, structuredIntent, replyText)
        }

        // 5. Extract typed parameters for Tool
        val extractedArgs = aiProvider.extractArguments(tool.id, sanitized).toMutableMap()
        extractedArgs.putAll(structuredIntent.arguments)

        // 6. Permission Verification
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

        // 7 & 8. Risk Assessment & Confirmation System
        val riskAssessment = riskManager.assessRisk(structuredIntent, tool.riskLevel)
        if (riskAssessment.requiresExplicitConfirmation && !isConfirmed) {
            val pendingConfirmation = riskManager.createPendingConfirmation(tool.id, structuredIntent, riskAssessment)
            return PipelineOutput.ConfirmationRequired(pendingConfirmation)
        }

        // 9. Safe Tool Execution
        val toolResult = try {
            tool.execute(context, extractedArgs)
        } catch (e: Exception) {
            ToolResult.failed(tool.id, "İcra zamanı gözlənilməz xəta: ${e.message}")
        }

        // 10. Result handling, memory logging & TTS response
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
