package com.example.jarvis.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.ai.matcher.DeterministicIntentMatcher
import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer
import com.example.jarvis.ai.provider.AIProvider
import com.example.jarvis.ai.provider.FallbackProvider
import com.example.jarvis.ai.provider.GeminiProvider
import com.example.jarvis.ai.provider.LocalSLMProvider
import com.example.jarvis.automation.CommandPipeline
import com.example.jarvis.automation.PipelineOutput
import com.example.jarvis.core.LowRamManager
import com.example.jarvis.data.local.JarvisDatabase
import com.example.jarvis.data.repository.JarvisRepositoryImpl
import com.example.jarvis.domain.model.AIProviderType
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.PendingActionConfirmation
import com.example.jarvis.memory.MemoryManager
import com.example.jarvis.permissions.PermissionManager
import com.example.jarvis.rag.RAGEngine
import com.example.jarvis.security.CommandSanitizer
import com.example.jarvis.security.RiskManager
import com.example.jarvis.tools.ToolRegistry
import com.example.jarvis.voice.TextToSpeechHelper
import com.example.jarvis.voice.VoiceRecognizerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val preferences = context.getSharedPreferences("jarvis_settings", android.content.Context.MODE_PRIVATE)
    private var storedGeminiApiKey: String
        get() = preferences.getString("gemini_api_key", "").orEmpty()
        set(value) = preferences.edit().putString("gemini_api_key", value.trim()).apply()

    // Core managers & dependencies
    val lowRamManager = LowRamManager(context)
    val permissionManager = PermissionManager(context)
    private val sanitizer = CommandSanitizer()
    private val riskManager = RiskManager(sanitizer)
    private val normalizer = AzerbaijaniTextNormalizer()
    private val matcher = DeterministicIntentMatcher(normalizer)

    // Data & memory
    private val database = JarvisDatabase.getInstance(context)
    private val repository = JarvisRepositoryImpl(database)
    val memoryManager = MemoryManager(repository, lowRamManager)

    // AI Providers
    private val localSLMProvider = LocalSLMProvider(appContext = context, normalizer = normalizer, matcher = matcher)
    private val geminiProvider = GeminiProvider(runtimeApiKey = { storedGeminiApiKey })
    private val fallbackProvider = FallbackProvider(localSLMProvider, geminiProvider, true)
    private var activeProvider: AIProvider = fallbackProvider

    // Tool Registry
    val toolRegistry = ToolRegistry()

    // Voice & TTS
    val voiceHelper = VoiceRecognizerHelper(context)
    val ttsHelper = TextToSpeechHelper(context).apply {
        isEnabled = preferences.getBoolean("tts_enabled", false)
    }

    // RAG Engine
    val ragEngine = RAGEngine(repository)

    // Command Pipeline
    private var commandPipeline = CommandPipeline(
        context = context,
        normalizer = normalizer,
        aiProvider = activeProvider,
        toolRegistry = toolRegistry,
        permissionManager = permissionManager,
        riskManager = riskManager,
        sanitizer = sanitizer,
        memoryManager = memoryManager,
        ttsHelper = ttsHelper,
        ragEngine = ragEngine
    )

    // UI State
    private val _uiState = MutableStateFlow(
        JarvisUiState(
            telemetry = lowRamManager.refreshTelemetry(),
            permissionStatuses = permissionManager.getAllPermissionStatuses(),
            isLowRamModeEnforced = lowRamManager.isLowRamEnvironment(),
            isTtsEnabled = preferences.getBoolean("tts_enabled", false),
            hasGeminiApiKey = storedGeminiApiKey.isNotBlank()
        )
    )
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    // Realtime Database Conversation Flow
    val conversations: StateFlow<List<ConversationMessage>> = memoryManager.getLiveConversations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Observe voice recognizer states
        viewModelScope.launch {
            voiceHelper.isListening.collect { listening ->
                _uiState.update { it.copy(isListening = listening, recognizedSpeechPreview = if (!listening) "" else it.recognizedSpeechPreview) }
            }
        }
        viewModelScope.launch {
            voiceHelper.partialText.collect { partial ->
                _uiState.update { it.copy(recognizedSpeechPreview = partial) }
            }
        }
        viewModelScope.launch {
            voiceHelper.rmsAudioLevel.collect { level ->
                _uiState.update { it.copy(audioLevel = level) }
            }
        }
        viewModelScope.launch {
            voiceHelper.speechError.collect { error ->
                _uiState.update { it.copy(speechError = error) }
            }
        }
        viewModelScope.launch {
            ttsHelper.isSpeaking.collect { speaking ->
                _uiState.update { it.copy(isSpeaking = speaking) }
            }
        }
        viewModelScope.launch {
            lowRamManager.telemetry.collect { telemetry ->
                _uiState.update { it.copy(telemetry = telemetry) }
            }
        }

        // Initial health check & history preload
        viewModelScope.launch(Dispatchers.IO) {
            memoryManager.loadRecentHistory()
            refreshProviderHealth()
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(currentInputText = text) }
    }

    fun startVoiceListening() {
        ttsHelper.stop()
        voiceHelper.startListening { recognizedText ->
            processUserCommand(recognizedText)
        }
    }

    fun stopVoiceListening() {
        voiceHelper.stopListening()
    }

    fun setTtsEnabled(enabled: Boolean) {
        ttsHelper.isEnabled = enabled
        preferences.edit().putBoolean("tts_enabled", enabled).apply()
        if (!enabled) ttsHelper.stop()
        _uiState.update { it.copy(isTtsEnabled = enabled) }
    }

    fun setGeminiApiKey(apiKey: String) {
        storedGeminiApiKey = apiKey
        _uiState.update { it.copy(hasGeminiApiKey = apiKey.trim().isNotBlank()) }
        viewModelScope.launch(Dispatchers.IO) { refreshProviderHealth() }
    }

    fun submitTextCommand() {
        val query = _uiState.value.currentInputText.trim()
        if (query.isNotBlank()) {
            _uiState.update { it.copy(currentInputText = "") }
            processUserCommand(query)
        }
    }

    fun processUserCommand(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isProcessing = true, speechError = null) }

            val output = commandPipeline.processCommand(query, isConfirmed = false)

            when (output) {
                is PipelineOutput.Executed -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            lastToolResult = output.toolResult,
                            pendingConfirmation = null
                        )
                    }
                }
                is PipelineOutput.ConfirmationRequired -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            pendingConfirmation = output.confirmation
                        )
                    }
                }
                is PipelineOutput.ConversationalResponse -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            pendingConfirmation = null
                        )
                    }
                }
                is PipelineOutput.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            speechError = output.reason
                        )
                    }
                }
            }

            _uiState.update {
                it.copy(performanceMetrics = commandPipeline.performanceTracker.getMetrics())
            }

            lowRamManager.refreshTelemetry()
        }
    }

    fun setConfirmationProfile(profile: com.example.jarvis.security.ConfirmationProfile) {
        riskManager.confirmationProfile = profile
        _uiState.update { it.copy(confirmationProfile = profile) }
    }

    fun confirmPendingAction(confirmation: PendingActionConfirmation) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(pendingConfirmation = null, isProcessing = true) }
            val output = commandPipeline.processCommand(confirmation.structuredIntent.rawQuery, isConfirmed = true)
            if (output is PipelineOutput.Executed) {
                _uiState.update { it.copy(isProcessing = false, lastToolResult = output.toolResult, performanceMetrics = commandPipeline.performanceTracker.getMetrics()) }
            } else {
                _uiState.update { it.copy(isProcessing = false, performanceMetrics = commandPipeline.performanceTracker.getMetrics()) }
            }
        }
    }

    fun dismissPendingConfirmation() {
        _uiState.update { it.copy(pendingConfirmation = null) }
    }

    fun setAIProvider(providerType: AIProviderType) {
        activeProvider = when (providerType) {
            AIProviderType.LOCAL_SLM -> localSLMProvider
            AIProviderType.GEMINI_CLOUD -> geminiProvider
            AIProviderType.FALLBACK_HYBRID -> fallbackProvider
        }

        commandPipeline = CommandPipeline(
            context = context,
            normalizer = normalizer,
            aiProvider = activeProvider,
            toolRegistry = toolRegistry,
            permissionManager = permissionManager,
            riskManager = riskManager,
            sanitizer = sanitizer,
            memoryManager = memoryManager,
            ttsHelper = ttsHelper,
            ragEngine = ragEngine
        )

        _uiState.update { it.copy(activeProviderType = providerType) }
        viewModelScope.launch(Dispatchers.IO) {
            refreshProviderHealth()
        }
    }

    fun toggleLowRamMode(enabled: Boolean) {
        _uiState.update { it.copy(isLowRamModeEnforced = enabled) }
        if (enabled) {
            localSLMProvider.unloadModel()
            memoryManager.trimContextIfNeeded()
        }
    }

    fun toggleSettingsSheet(show: Boolean) {
        _uiState.update {
            it.copy(
                showSettingsSheet = show,
                permissionStatuses = permissionManager.getAllPermissionStatuses()
            )
        }
    }

    fun clearConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            memoryManager.clearConversations()
        }
    }

    private suspend fun refreshProviderHealth() {
        val health = activeProvider.healthCheck()
        _uiState.update { it.copy(providerHealth = health) }
    }

    override fun onCleared() {
        super.onCleared()
        voiceHelper.stopListening()
        ttsHelper.shutdown()
        lowRamManager.unregister()
    }
}
