package com.example.jarvis.automation

import android.content.Context
import com.example.jarvis.agent.AgentExecutor
import com.example.jarvis.agent.AgentPlanner
import com.example.jarvis.ai.engine.IntentClassificationReport
import com.example.jarvis.ai.engine.IntentEngine
import com.example.jarvis.ai.engine.SemanticLightweightEngine
import com.example.jarvis.ai.matcher.DeterministicIntentMatcher
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.ai.provider.AIProvider
import com.example.jarvis.automation.apps.AppResolver
import com.example.jarvis.automation.media.MediaCommandEngine
import com.example.jarvis.automation.observation.ObservationEngine
import com.example.jarvis.automation.recovery.RecoveryEngine
import com.example.jarvis.automation.verification.VerificationEngine
import com.example.jarvis.context.ConversationContextManager
import com.example.jarvis.core.CommandCache
import com.example.jarvis.core.CrashRecoveryManager
import com.example.jarvis.core.PerformanceTracker
import com.example.jarvis.core.ToolSecurityValidator
import com.example.jarvis.core.ValidationResult
import com.example.jarvis.domain.model.CommandAction
import com.example.jarvis.domain.model.CommandIntent
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.ExecutionLog
import com.example.jarvis.domain.model.ExecutionStrategy
import com.example.jarvis.domain.model.IntentCategory
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
import com.example.jarvis.services.AccessibilityController
import com.example.jarvis.tools.ToolRegistry
import com.example.jarvis.voice.TextToSpeechHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

data class DiagnosticsTrace(
    val taskId: String,
    val rawInput: String,
    val detectedLanguage: String,
    val intentId: String,
    val intentCategory: String,
    val targetApp: String?,
    val resolvedPackage: String?,
    val query: String?,
    val routerDecisionReason: String,
    val selectedTool: String?,
    val executionDurationMs: Long,
    val verificationStatus: String,
    val recoveryAttempts: Int = 0,
    val status: ToolStatus
)

sealed class PipelineOutput {
    data class Executed(
        val query: String,
        val commandIntent: CommandIntent,
        val toolResult: ToolResult,
        val responseSpeech: String,
        val diagnostics: DiagnosticsTrace
    ) : PipelineOutput()

    data class ConfirmationRequired(
        val confirmation: PendingActionConfirmation
    ) : PipelineOutput()

    data class ConversationalResponse(
        val query: String,
        val commandIntent: CommandIntent,
        val replyText: String,
        val diagnostics: DiagnosticsTrace
    ) : PipelineOutput()

    data class Error(
        val query: String,
        val reason: String,
        val diagnostics: DiagnosticsTrace? = null
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
    private val agentExecutor: AgentExecutor = AgentExecutor(toolRegistry, memoryManager),
    val performanceTracker: PerformanceTracker = PerformanceTracker(),
    val commandCache: CommandCache = CommandCache(normalizer),
    val securityValidator: ToolSecurityValidator = ToolSecurityValidator(toolRegistry),
    val crashRecoveryManager: CrashRecoveryManager = CrashRecoveryManager()
) {

    private val taskCounter = AtomicInteger(1)

    val appResolver = AppResolver(context, normalizer)
    val accessibilityController = AccessibilityController(context)
    val observationEngine = ObservationEngine(context, accessibilityController)
    val verificationEngine = VerificationEngine(context, observationEngine, accessibilityController)
    val recoveryEngine = RecoveryEngine(context)
    val mediaCommandEngine = MediaCommandEngine(
        context,
        appResolver,
        accessibilityController,
        observationEngine,
        verificationEngine
    )

    private val deterministicMatcher = DeterministicIntentMatcher(normalizer)
    private val semanticLightweightEngine = SemanticLightweightEngine(normalizer)

    val intentEngine = IntentEngine(
        deterministicMatcher = deterministicMatcher,
        semanticLightweightEngine = semanticLightweightEngine,
        localSLMProvider = aiProvider,
        geminiProvider = aiProvider,
        normalizer = normalizer
    )

    private var latestDiagnostics: DiagnosticsTrace? = null
    fun getLatestDiagnostics(): DiagnosticsTrace? = latestDiagnostics

    private fun generateTaskId(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(Date())
        val seq = taskCounter.getAndIncrement()
        return "TASK-$dateFormat-%04d".format(seq)
    }

    suspend fun processCommand(
        rawInput: String,
        isConfirmed: Boolean = false,
        isOnline: Boolean = true,
        onStateChange: ((String) -> Unit)? = null
    ): PipelineOutput {
        val startTime = System.currentTimeMillis()
        val taskId = generateTaskId()

        // 1. Sanitize & Normalize Input
        onStateChange?.invoke("UNDERSTANDING")
        val sanitized = sanitizer.sanitizeInput(rawInput)
        if (sanitized.isBlank()) {
            return PipelineOutput.Error(rawInput, "Daxil edilən mətn boşdur.")
        }
        val normalized = normalizer.normalize(sanitized)
        val detectedLanguage = normalizer.detectLanguage(sanitized).label

        // Add user message to conversation memory
        memoryManager.addMessage(
            ConversationMessage(
                sender = MessageSender.USER,
                text = sanitized
            )
        )

        // 2. MEMORY EXPLICIT COMMANDS ("Bunu yadda saxla", "Bunu unut", "Yaddaşda nə var")
        if (normalized.contains("yadda saxla") || normalized.contains("yaddasinda saxla")) {
            val factContent = sanitized.substringAfter("saxla").trim(':', ' ', '"', '\'')
                .ifEmpty { sanitized }
            memoryManager.rememberFact(factContent)
            val reply = "Yadda saxlanıldı: '$factContent'."
            ttsHelper.speak(reply)
            val cmd = CommandIntent("REMEMBER_FACT", IntentCategory.SYSTEM, CommandAction.UNKNOWN_ACTION, query = factContent, rawQuery = sanitized, normalizedQuery = normalized)
            val trace = DiagnosticsTrace(taskId, sanitized, detectedLanguage, "REMEMBER_FACT", "SYSTEM", null, null, factContent, "Fakt yaddaşa yazıldı", "MEMORY", System.currentTimeMillis() - startTime, "VERIFIED", 0, ToolStatus.SUCCESS)
            latestDiagnostics = trace
            recordAssistantMessage(sanitized, "REMEMBER_FACT", reply, startTime)
            return PipelineOutput.ConversationalResponse(sanitized, cmd, reply, trace)
        }

        if (normalized.contains("bunu unut") || normalized.contains("yaddasdan sil") || normalized.contains("unut")) {
            val target = sanitized.substringAfter("unut").ifEmpty { sanitized.substringAfter("sil") }.trim(':', ' ', '"', '\'')
            val deleted = memoryManager.forgetFact(target.ifEmpty { sanitized })
            val reply = if (deleted) "Fakt yaddaşdan silindi." else "'$target' üzrə yaddaşda heç bir fakt tapılmadı."
            ttsHelper.speak(reply)
            val cmd = CommandIntent("FORGET_FACT", IntentCategory.SYSTEM, CommandAction.UNKNOWN_ACTION, query = target, rawQuery = sanitized, normalizedQuery = normalized)
            val trace = DiagnosticsTrace(taskId, sanitized, detectedLanguage, "FORGET_FACT", "SYSTEM", null, null, target, "Fakt yaddaşdan silindi", "MEMORY", System.currentTimeMillis() - startTime, "VERIFIED", 0, ToolStatus.SUCCESS)
            latestDiagnostics = trace
            recordAssistantMessage(sanitized, "FORGET_FACT", reply, startTime)
            return PipelineOutput.ConversationalResponse(sanitized, cmd, reply, trace)
        }

        // 3. MULTI-STEP AGENT PLANNING
        if (agentPlanner.shouldPlan(sanitized)) {
            onStateChange?.invoke("PLANNING")
            val planStart = System.currentTimeMillis()
            val plan = agentPlanner.createPlan(sanitized)
            onStateChange?.invoke("EXECUTING")
            val execResult = agentExecutor.executePlan(context, plan)
            val planDuration = System.currentTimeMillis() - planStart
            performanceTracker.recordToolLatency(planDuration)
            performanceTracker.recordOperationTotal(System.currentTimeMillis() - startTime)

            onStateChange?.invoke("VERIFYING")
            ttsHelper.speak(execResult.summary)
            recordAssistantMessage(sanitized, "AGENT_DIAGNOSTICS", execResult.summary, startTime)
            val cmd = CommandIntent("AGENT_DIAGNOSTICS", IntentCategory.DIAGNOSTIC, CommandAction.SYSTEM_DIAGNOSTIC, rawQuery = sanitized, normalizedQuery = normalized)
            val trace = DiagnosticsTrace(taskId, sanitized, detectedLanguage, "AGENT_DIAGNOSTICS", "DIAGNOSTIC", null, null, null, "Agent diaqnostik planı icra edildi", "AGENT_EXECUTOR", System.currentTimeMillis() - startTime, "VERIFIED", 0, ToolStatus.SUCCESS)
            latestDiagnostics = trace
            return PipelineOutput.Executed(
                query = sanitized,
                commandIntent = cmd,
                toolResult = ToolResult.success("AGENT_DIAGNOSTICS", execResult.summary),
                responseSpeech = execResult.summary,
                diagnostics = trace
            )
        }

        // 4. LAYERED INTENT CLASSIFICATION (Level 1: Deterministic -> Level 2: Semantic -> Level 3: Local SLM -> Level 4: Gemini)
        val classificationReport = intentEngine.classify(sanitized, isOnline)
        var commandIntent = classificationReport.commandIntent
        performanceTracker.recordIntentLatency(classificationReport.latencyMs)

        // 5. APP RESOLUTION & PACKAGE TARGETING
        var resolvedPackage: String? = null
        if (commandIntent.targetApp != null) {
            val res = appResolver.resolveApp(commandIntent.targetApp!!)
            if (res.matched && res.packageName != null) {
                resolvedPackage = res.packageName
                commandIntent = commandIntent.copy(targetPackage = resolvedPackage)
            }
        }

        // 6b. INSTANT GREETING & SMALLTALK
        if (commandIntent.intentId == "GREETING_AND_CHAT") {
            val replyText = generateInstantGreetingResponse(normalized)
            recordAssistantMessage(sanitized, "GREETING_AND_CHAT", replyText, startTime)
            ttsHelper.speak(replyText)
            val trace = DiagnosticsTrace(
                taskId = taskId,
                rawInput = sanitized,
                detectedLanguage = detectedLanguage,
                intentId = "GREETING_AND_CHAT",
                intentCategory = "GENERAL_CHAT",
                targetApp = null,
                resolvedPackage = null,
                query = null,
                routerDecisionReason = "Təbii salamlaşma və dialoq",
                selectedTool = null,
                executionDurationMs = System.currentTimeMillis() - startTime,
                verificationStatus = "VERIFIED",
                recoveryAttempts = 0,
                status = ToolStatus.SUCCESS
            )
            latestDiagnostics = trace
            return PipelineOutput.ConversationalResponse(sanitized, commandIntent, replyText, trace)
        }

        // 6. CHECK RAG KNOWLEDGE BASE
        if (commandIntent.category == IntentCategory.GENERAL_CHAT && ragEngine != null) {
            val ragAnswer = ragEngine.answerIfKnowledgeAvailable(sanitized)
            if (ragAnswer != null) {
                recordAssistantMessage(sanitized, "RAG_KNOWLEDGE", ragAnswer, startTime)
                ttsHelper.speak(ragAnswer)
                val trace = DiagnosticsTrace(taskId, sanitized, detectedLanguage, "RAG_KNOWLEDGE", "GENERAL_CHAT", null, null, null, "RAG lokal bilik bazasından cavablandırıldı", "RAG", System.currentTimeMillis() - startTime, "VERIFIED", 0, ToolStatus.SUCCESS)
                latestDiagnostics = trace
                return PipelineOutput.ConversationalResponse(sanitized, commandIntent, ragAnswer, trace)
            }
        }

        // 7. DIRECT DEDICATED MEDIA AUTOMATION (YouTube / Spotify Search & Play)
        if (commandIntent.intentId == "MEDIA_SEARCH_PLAY" && commandIntent.category == IntentCategory.MEDIA) {
            onStateChange?.invoke("EXECUTING")
            val targetApp = commandIntent.targetApp ?: "youtube"
            val query = commandIntent.query ?: sanitized

            val mediaResult = if (targetApp.equals("spotify", ignoreCase = true)) {
                mediaCommandEngine.searchAndPlaySpotify(query)
            } else {
                mediaCommandEngine.searchAndPlayYouTube(query)
            }

            val responseSpeech = mediaResult.outputMessage
            ttsHelper.speak(responseSpeech)
            handleToolCompletion(sanitized, commandIntent.toStructuredIntent(), "MEDIA_SEARCH_PLAY", mediaResult, startTime)

            val trace = DiagnosticsTrace(
                taskId = taskId,
                rawInput = sanitized,
                detectedLanguage = detectedLanguage,
                intentId = commandIntent.intentId,
                intentCategory = commandIntent.category.name,
                targetApp = targetApp,
                resolvedPackage = resolvedPackage ?: "com.google.android.youtube",
                query = query,
                routerDecisionReason = classificationReport.routerDecisionReason,
                selectedTool = "MEDIA_SEARCH_PLAY",
                executionDurationMs = System.currentTimeMillis() - startTime,
                verificationStatus = if (mediaResult.isSuccess) "VERIFIED_PLAYBACK" else if (mediaResult.isPartialSuccess) "PARTIAL_SEARCH_ONLY" else "FAILED",
                status = mediaResult.status
            )
            latestDiagnostics = trace

            return PipelineOutput.Executed(
                query = sanitized,
                commandIntent = commandIntent,
                toolResult = mediaResult,
                responseSpeech = responseSpeech,
                diagnostics = trace
            )
        }

        // 8. TOOL REGISTRY LOOKUP
        val tool = toolRegistry.getTool(commandIntent.intentId)
        if (tool == null) {
            // Conversational fallback
            onStateChange?.invoke("PLANNING")
            val contextMessages = memoryManager.getShortTermContext()
            val aiResponse = aiProvider.generate(sanitized, contextMessages)
            val replyText = aiResponse.getOrNull()?.text ?: "Sizi başa düşdüm. Əlavə bir əmriniz var?"

            recordAssistantMessage(sanitized, commandIntent.intentId, replyText, startTime)
            ttsHelper.speak(replyText)

            val trace = DiagnosticsTrace(
                taskId = taskId,
                rawInput = sanitized,
                detectedLanguage = detectedLanguage,
                intentId = commandIntent.intentId,
                intentCategory = commandIntent.category.name,
                targetApp = commandIntent.targetApp,
                resolvedPackage = resolvedPackage,
                query = commandIntent.query,
                routerDecisionReason = classificationReport.routerDecisionReason,
                selectedTool = null,
                executionDurationMs = System.currentTimeMillis() - startTime,
                verificationStatus = "N/A",
                status = ToolStatus.SUCCESS
            )
            latestDiagnostics = trace

            return PipelineOutput.ConversationalResponse(sanitized, commandIntent, replyText, trace)
        }

        // 9. Extract typed parameters for Tool
        val extractedArgs = aiProvider.extractArguments(tool.id, sanitized).toMutableMap()
        extractedArgs.putAll(commandIntent.parameters)
        if (commandIntent.query != null) extractedArgs["query"] = commandIntent.query!!
        if (commandIntent.targetApp != null) extractedArgs["target_app"] = commandIntent.targetApp!!
        if (commandIntent.targetApp != null) extractedArgs["app_name"] = commandIntent.targetApp!!

        // 10. Tool Security Validator (Allowlist, Path traversal check)
        when (val validation = securityValidator.validateToolExecution(tool.id, extractedArgs, sanitized)) {
            is ValidationResult.Invalid -> {
                val errorMsg = "Təhlükəsizlik xətası: ${validation.reason}"
                ttsHelper.speak(errorMsg)
                val trace = DiagnosticsTrace(taskId, sanitized, detectedLanguage, commandIntent.intentId, commandIntent.category.name, commandIntent.targetApp, resolvedPackage, commandIntent.query, "Security check failed", tool.id, System.currentTimeMillis() - startTime, "SECURITY_REJECTED", 0, ToolStatus.DENIED)
                latestDiagnostics = trace
                return PipelineOutput.Error(sanitized, errorMsg, trace)
            }
            is ValidationResult.Valid -> { /* Proceed */ }
        }

        // 11. Permission Verification
        val missingPerms = permissionManager.getMissingPermissions(tool.requiredPermissions)
        if (missingPerms.isNotEmpty()) {
            val permResult = ToolResult.permissionRequired(
                toolId = tool.id,
                permissions = missingPerms,
                message = "'${tool.name}' əməliyyatını icra etmək üçün tələb olunan icazələr çatışmır."
            )
            handleToolCompletion(sanitized, commandIntent.toStructuredIntent(), tool.id, permResult, startTime)
            val trace = DiagnosticsTrace(taskId, sanitized, detectedLanguage, commandIntent.intentId, commandIntent.category.name, commandIntent.targetApp, resolvedPackage, commandIntent.query, "Missing permissions", tool.id, System.currentTimeMillis() - startTime, "PERMISSION_MISSING", 0, ToolStatus.PERMISSION_REQUIRED)
            latestDiagnostics = trace
            return PipelineOutput.Executed(sanitized, commandIntent, permResult, permResult.outputMessage, trace)
        }

        // 12. Risk Assessment & Confirmation System
        val riskAssessment = riskManager.assessRisk(commandIntent.toStructuredIntent(), tool.riskLevel)
        if (riskAssessment.requiresExplicitConfirmation && !isConfirmed) {
            val pendingConfirmation = riskManager.createPendingConfirmation(tool.id, commandIntent.toStructuredIntent(), riskAssessment)
            return PipelineOutput.ConfirmationRequired(pendingConfirmation)
        }

        // 13. Safe Tool Execution
        onStateChange?.invoke("EXECUTING")
        val toolExecStart = System.currentTimeMillis()
        var toolResult = try {
            tool.execute(context, extractedArgs)
        } catch (e: Exception) {
            ToolResult.failed(tool.id, "İcra zamanı xəta: ${e.message}")
        }
        performanceTracker.recordToolLatency(System.currentTimeMillis() - toolExecStart)

        // 14. Verification & Recovery
        onStateChange?.invoke("VERIFYING")
        if (!toolResult.isSuccess) {
            val recoveryAction = recoveryEngine.recoverFromFailure(commandIntent, tool.id, toolResult.outputMessage)
            toolResult = recoveryEngine.executeRecovery(recoveryAction)
        }

        // 15. Format speech response & commit to logs
        val responseSpeech = toolResult.outputMessage
        handleToolCompletion(sanitized, commandIntent.toStructuredIntent(), tool.id, toolResult, startTime)
        ttsHelper.speak(responseSpeech)
        performanceTracker.recordOperationTotal(System.currentTimeMillis() - startTime)

        val trace = DiagnosticsTrace(
            taskId = taskId,
            rawInput = sanitized,
            detectedLanguage = detectedLanguage,
            intentId = commandIntent.intentId,
            intentCategory = commandIntent.category.name,
            targetApp = commandIntent.targetApp,
            resolvedPackage = resolvedPackage,
            query = commandIntent.query,
            routerDecisionReason = classificationReport.routerDecisionReason,
            selectedTool = tool.id,
            executionDurationMs = System.currentTimeMillis() - startTime,
            verificationStatus = if (toolResult.isSuccess) "VERIFIED" else if (toolResult.isPartialSuccess) "PARTIAL" else "FAILED",
            status = toolResult.status
        )
        latestDiagnostics = trace

        return PipelineOutput.Executed(
            query = sanitized,
            commandIntent = commandIntent,
            toolResult = toolResult,
            responseSpeech = responseSpeech,
            diagnostics = trace
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

    private fun generateInstantGreetingResponse(normalized: String): String {
        return when {
            normalized.contains("necesen") || normalized.contains("necesiniz") || normalized.contains("keyfin") || normalized.contains("veziyyet") ->
                "Təşəkkür edirəm! Bütün sistemlərim aktivdir və əla işləyir. Siz necəsiniz? Sizə necə kömək edə bilərəm?"
            normalized.contains("ne var ne yox") || normalized.contains("isler") ->
                "Hər şey qaydasındadır, ser. Cihazınızı idarə etməyə və əmrlərinizi icra etməyə hazıram."
            normalized.contains("sabahin xeyir") ->
                "Sabahınız xeyir! Gününüz uğurlu və məhsuldar keçsin. Hansısa bir tapşırığınız var?"
            normalized.contains("axsamin xeyir") ->
                "Axşamınız xeyir! Sizə necə kömək edə bilərəm?"
            normalized.contains("gecen xeyre") ->
                "Gecəniz xeyrə qalsın. İstirahət edin, sistemləriniz nəzarətdədir."
            normalized.contains("kimsen") || normalized.contains("adin") || normalized.contains("jarvis kimdir") ->
                "Mən JARVIS - Android cihazınız üçün hazırlanmış fərdi süni intellekt köməkçisiyəm."
            normalized.contains("sag ol") || normalized.contains("tesekkur") || normalized.contains("minnetdaram") ->
                "Dəyməz, xoşdur! Sizə xidmət etmək mənim üçün zövqdür."
            normalized.contains("ne edirsen") || normalized.contains("ne bacarirsan") || normalized.contains("komek") ->
                "Mən telefonunuzu idarə edə, zəng vura, mesaj yaza, YouTube-da mahnı axtarıb çala, hava və xəbərləri oxuya və ağıllı ev cihazlarını idarə edə bilərəm."
            else ->
                "Salam! Mən JARVIS, sizin şəxsi süni intellekt köməkçinizəm. Əmrinizi gözləyirəm."
        }
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
