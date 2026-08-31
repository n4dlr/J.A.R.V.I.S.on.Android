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
import com.example.jarvis.ai.runtime.ModelDownloadManager
import com.example.jarvis.ai.runtime.ModelDownloadState
import com.example.jarvis.services.JarvisNotificationListenerService
import com.example.jarvis.voice.ContinuousVoiceSession
import com.example.jarvis.voice.ShakeDetector
import com.example.jarvis.voice.TextToSpeechHelper
import com.example.jarvis.voice.VoiceRecognizerHelper
import com.example.jarvis.voice.VoskModelManager
import com.example.jarvis.voice.VoskModelManager.VoskDownloadState
import com.example.jarvis.voice.WakeWordDetector
import com.example.jarvis.voice.WakeWordEvent
import com.example.jarvis.voice.NeuralTtsManager
import com.example.jarvis.voice.NeuralVoiceGender
import com.example.jarvis.tools.impl.spotify.SpotifyAuthManager
import com.example.jarvis.scheduler.MorningBriefingWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit
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
    val toolRegistry = ToolRegistry().also { it.registerContextDependentTools(context) }

    // Vosk Offline STT Manager
    val voskModelManager = VoskModelManager(context)

    // Spotify Auth Manager
    val spotifyAuthManager = SpotifyAuthManager(context)

    // Local Offline Vision Manager (SmolVLM / Moondream GGUF ~290MB)
    val localVisionManager = com.example.jarvis.ai.vision.LocalVisionManager(context)

    // Neural TTS Manager (Piper / Sherpa-ONNX custom Azerbaijani voice)
    val neuralTtsManager = NeuralTtsManager(context)

    // Voice & TTS
    val voiceHelper = VoiceRecognizerHelper(context)
    val ttsHelper = TextToSpeechHelper(context, neuralTtsManager).apply {
        isEnabled = preferences.getBoolean("tts_enabled", false)
    }

    // Wake word detector (hands-free "Hey JARVIS" / "JARVIS" activation)
    val wakeWordDetector = WakeWordDetector(context, voiceHelper, normalizer)

    // Model Download Manager
    val modelDownloadManager = ModelDownloadManager(context)

    // Shake Detector
    private val shakeDetector = ShakeDetector(context) {
        if (!_uiState.value.isListening && !_uiState.value.isProcessing) {
            startVoiceListening()
        }
    }

    // Continuous Voice Session (hands-free duplex mode)
    private var continuousSession: ContinuousVoiceSession? = null

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
            hasGeminiApiKey = storedGeminiApiKey.isNotBlank(),
            activeLanguage = preferences.getString("active_language", "az-AZ") ?: "az-AZ",
            isShakeToWakeEnabled = preferences.getBoolean("shake_to_wake_enabled", false),
            isNotificationReadoutEnabled = preferences.getBoolean("notif_readout_enabled", false),
            isModelDownloaded = modelDownloadManager.isModelDownloaded(),
            isVoskModelReady = voskModelManager.isModelReady(),
            isMorningBriefingEnabled = preferences.getBoolean("morning_briefing_enabled", false),
            briefingHour = preferences.getInt("briefing_hour", 8),
            isSpotifyAuthenticated = spotifyAuthManager.isAuthenticated(),
            isSpotifyConfigured = spotifyAuthManager.isConfigured(),
            isHomeAssistantConfigured = preferences.getString("ha_server_url", "")?.isNotBlank() == true,
            homeAssistantServerUrl = preferences.getString("ha_server_url", "") ?: "",
            isLocalVisionReady = localVisionManager.isModelReady(),
            isNeuralTtsReady = neuralTtsManager.isModelReady(),
            activeNeuralVoice = neuralTtsManager.activeGender.titleAz
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
                _uiState.update { it.copy(speechError = error, executionStage = null) }
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

        // Restore wake word state from preferences
        val savedWakeWord = preferences.getBoolean("wake_word_enabled", false)
        if (savedWakeWord) {
            _uiState.update { it.copy(isWakeWordEnabled = true) }
            startWakeWordListening()
        }

        // Restore shake to wake
        val savedShake = preferences.getBoolean("shake_to_wake_enabled", false)
        if (savedShake) {
            shakeDetector.start()
        }

        // Connect notification listener callback
        JarvisNotificationListenerService.StateHolder.notificationCallback = { notif ->
            if (_uiState.value.isNotificationReadoutEnabled && _uiState.value.isTtsEnabled) {
                ttsHelper.speak("Yeni bildiriş: ${notif.appLabel} tətbiqindən. ${notif.title}. ${notif.text}")
            }
        }

        // Observe model download state
        viewModelScope.launch {
            modelDownloadManager.downloadState.collect { dState ->
                when (dState) {
                    is ModelDownloadState.Idle -> {
                        _uiState.update { it.copy(modelDownloadProgress = null, modelDownloadError = null, isModelDownloaded = modelDownloadManager.isModelDownloaded()) }
                    }
                    is ModelDownloadState.Downloading -> {
                        _uiState.update { it.copy(modelDownloadProgress = dState.progressPercent, modelDownloadError = null) }
                    }
                    is ModelDownloadState.Completed -> {
                        _uiState.update { it.copy(modelDownloadProgress = null, isModelDownloaded = true, modelDownloadError = null) }
                        ttsHelper.speak("Lokal SLM modeli uğurla yükləndi və hazır vəziyyətə gətirildi.")
                    }
                    is ModelDownloadState.Failed -> {
                        _uiState.update { it.copy(modelDownloadProgress = null, modelDownloadError = dState.error) }
                    }
                }
            }
        }

        // Observe Vosk model download state
        viewModelScope.launch {
            voskModelManager.downloadState.collect { dState ->
                when (dState) {
                    is VoskDownloadState.Idle ->
                        _uiState.update { it.copy(voskDownloadProgress = null, isVoskModelReady = voskModelManager.isModelReady()) }
                    is VoskDownloadState.Downloading ->
                        _uiState.update { it.copy(voskDownloadProgress = dState.progressPercent) }
                    is VoskDownloadState.Completed -> {
                        _uiState.update { it.copy(voskDownloadProgress = null, isVoskModelReady = true) }
                        ttsHelper.speak("Offline Azərbaycan nitq tanıma modeli yükləndi.")
                    }
                    is VoskDownloadState.Failed ->
                        _uiState.update { it.copy(voskDownloadProgress = null) }
                }
            }
        }

        // Observe Local Vision model download state
        viewModelScope.launch {
            localVisionManager.downloadState.collect { vState ->
                when (vState) {
                    is com.example.jarvis.ai.vision.LocalVisionManager.VisionDownloadState.Idle ->
                        _uiState.update { it.copy(localVisionDownloadProgress = null, isLocalVisionReady = localVisionManager.isModelReady()) }
                    is com.example.jarvis.ai.vision.LocalVisionManager.VisionDownloadState.Downloading ->
                        _uiState.update { it.copy(localVisionDownloadProgress = vState.progressPercent) }
                    is com.example.jarvis.ai.vision.LocalVisionManager.VisionDownloadState.Completed -> {
                        _uiState.update { it.copy(localVisionDownloadProgress = null, isLocalVisionReady = true) }
                        ttsHelper.speak("Lokal Offline Vision modeli yükləndi.")
                    }
                    is com.example.jarvis.ai.vision.LocalVisionManager.VisionDownloadState.Failed ->
                        _uiState.update { it.copy(localVisionDownloadProgress = null) }
                }
            }
        }

        // Observe Neural TTS model download state
        viewModelScope.launch {
            neuralTtsManager.downloadState.collect { nState ->
                when (nState) {
                    is NeuralTtsManager.NeuralDownloadState.Idle ->
                        _uiState.update { it.copy(neuralTtsDownloadProgress = null, isNeuralTtsReady = neuralTtsManager.isModelReady()) }
                    is NeuralTtsManager.NeuralDownloadState.Downloading ->
                        _uiState.update { it.copy(neuralTtsDownloadProgress = nState.progressPercent) }
                    is NeuralTtsManager.NeuralDownloadState.Completed -> {
                        _uiState.update { it.copy(neuralTtsDownloadProgress = null, isNeuralTtsReady = true) }
                        ttsHelper.speak("Xüsusi Azərbaycan neyron səsi uğurla yükləndi.")
                    }
                    is NeuralTtsManager.NeuralDownloadState.Failed ->
                        _uiState.update { it.copy(neuralTtsDownloadProgress = null) }
                }
            }
        }

        // Restore morning briefing schedule
        if (preferences.getBoolean("morning_briefing_enabled", false)) {
            scheduleMorningBriefing(preferences.getInt("briefing_hour", 8))
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
        val loc = _uiState.value.activeLanguage
        _uiState.update { it.copy(executionStage = "LISTENING") }
        voiceHelper.startListening(loc) { recognizedText ->
            processUserCommand(recognizedText)
        }
    }

    fun stopVoiceListening() {
        voiceHelper.stopListening()
        _uiState.update { it.copy(executionStage = null) }
    }

    // ── Wake Word ─────────────────────────────────────────────────────────────

    fun toggleWakeWordMode(enabled: Boolean) {
        preferences.edit().putBoolean("wake_word_enabled", enabled).apply()
        _uiState.update { it.copy(isWakeWordEnabled = enabled) }
        if (enabled) {
            startWakeWordListening()
        } else {
            wakeWordDetector.stopContinuousHotwordListening()
        }
    }

    private fun startWakeWordListening() {
        val locale = _uiState.value.activeLanguage
        wakeWordDetector.startContinuousHotwordListening(locale = locale) { event ->
            when (event) {
                is WakeWordEvent.WakeWordOnly -> {
                    // Wake word only → start active listening session
                    ttsHelper.speak("Bəli, əmrinizi gözləyirəm.")
                    viewModelScope.launch(Dispatchers.Main) {
                        startVoiceListening()
                    }
                }
                is WakeWordEvent.WakeWordWithCommand -> {
                    // Wake word + inline command → process immediately
                    processUserCommand(event.command)
                }
            }
        }
    }

    // ── Shake & Gesture ───────────────────────────────────────────────────────

    fun toggleShakeToWake(enabled: Boolean) {
        preferences.edit().putBoolean("shake_to_wake_enabled", enabled).apply()
        _uiState.update { it.copy(isShakeToWakeEnabled = enabled) }
        if (enabled) {
            shakeDetector.start()
        } else {
            shakeDetector.stop()
        }
    }

    // ── Notification Readout ──────────────────────────────────────────────────

    fun toggleNotificationReadout(enabled: Boolean) {
        preferences.edit().putBoolean("notif_readout_enabled", enabled).apply()
        _uiState.update { it.copy(isNotificationReadoutEnabled = enabled) }
    }

    // ── Model Download ────────────────────────────────────────────────────────

    fun downloadLocalModel() {
        viewModelScope.launch(Dispatchers.IO) {
            modelDownloadManager.downloadModel()
        }
    }

    fun deleteLocalModel() {
        modelDownloadManager.deleteModel()
        _uiState.update { it.copy(isModelDownloaded = false) }
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

    fun setActiveLanguage(localeTag: String) {
        preferences.edit().putString("active_language", localeTag).apply()
        _uiState.update { it.copy(activeLanguage = localeTag) }
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
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    speechError = null,
                    executionStage = "UNDERSTANDING"
                )
            }

            val output = commandPipeline.processCommand(
                rawInput = query,
                isConfirmed = false,
                onStateChange = { stage ->
                    _uiState.update { it.copy(executionStage = stage) }
                }
            )

            when (output) {
                is PipelineOutput.Executed -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            executionStage = "COMPLETED",
                            lastToolResult = output.toolResult,
                            diagnosticsTrace = output.diagnostics,
                            pendingConfirmation = null
                        )
                    }
                }
                is PipelineOutput.ConfirmationRequired -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            executionStage = null,
                            pendingConfirmation = output.confirmation
                        )
                    }
                }
                is PipelineOutput.ConversationalResponse -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            executionStage = "COMPLETED",
                            diagnosticsTrace = output.diagnostics,
                            pendingConfirmation = null
                        )
                    }
                }
                is PipelineOutput.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            executionStage = "FAILED",
                            speechError = output.reason,
                            diagnosticsTrace = output.diagnostics
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
            _uiState.update { it.copy(pendingConfirmation = null, isProcessing = true, executionStage = "EXECUTING") }
            val output = commandPipeline.processCommand(
                rawInput = confirmation.structuredIntent.rawQuery,
                isConfirmed = true,
                onStateChange = { stage -> _uiState.update { it.copy(executionStage = stage) } }
            )
            if (output is PipelineOutput.Executed) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        executionStage = "COMPLETED",
                        lastToolResult = output.toolResult,
                        diagnosticsTrace = output.diagnostics,
                        performanceMetrics = commandPipeline.performanceTracker.getMetrics()
                    )
                }
            } else {
                _uiState.update { it.copy(isProcessing = false, executionStage = "COMPLETED", performanceMetrics = commandPipeline.performanceTracker.getMetrics()) }
            }
        }
    }

    fun dismissPendingConfirmation() {
        _uiState.update { it.copy(pendingConfirmation = null, executionStage = null) }
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

    fun toggleDiagnosticsScreen(show: Boolean) {
        _uiState.update {
            it.copy(
                showDiagnosticsScreen = show,
                diagnosticsTrace = it.diagnosticsTrace ?: commandPipeline.getLatestDiagnostics()
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

    // ── Continuous Voice Session ───────────────────────────────────────────────

    fun startContinuousSession() {
        if (_uiState.value.isContinuousSessionActive) return
        ttsHelper.speak("Davamlı danışıq rejimi başladı. Əmrlərimi dinləyirəm. Bitirmək üçün 'saxla' deyin.")
        val locale = _uiState.value.activeLanguage
        continuousSession = ContinuousVoiceSession(
            context = context,
            voiceHelper = voiceHelper,
            ttsHelper = ttsHelper,
            normalizer = normalizer,
            onCommand = { query -> processUserCommandSuspend(query) },
            onSessionStarted = { _uiState.update { it.copy(isContinuousSessionActive = true) } },
            onSessionStopped = { _uiState.update { it.copy(isContinuousSessionActive = false) } }
        )
        continuousSession?.start(locale)
    }

    fun stopContinuousSession() {
        continuousSession?.stop()
        continuousSession = null
        _uiState.update { it.copy(isContinuousSessionActive = false) }
    }

    private suspend fun processUserCommandSuspend(query: String) {
        _uiState.update { it.copy(isProcessing = true, executionStage = "UNDERSTANDING") }
        val output = commandPipeline.processCommand(
            rawInput = query,
            isConfirmed = false,
            onStateChange = { stage -> _uiState.update { it.copy(executionStage = stage) } }
        )
        _uiState.update { it.copy(isProcessing = false, executionStage = "COMPLETED") }
    }

    // ── Morning Briefing ──────────────────────────────────────────────────────

    fun toggleMorningBriefing(enabled: Boolean, hour: Int = _uiState.value.briefingHour) {
        preferences.edit()
            .putBoolean("morning_briefing_enabled", enabled)
            .putInt("briefing_hour", hour)
            .apply()
        _uiState.update { it.copy(isMorningBriefingEnabled = enabled, briefingHour = hour) }
        if (enabled) scheduleMorningBriefing(hour)
        else WorkManager.getInstance(context).cancelUniqueWork(MorningBriefingWorker.WORK_NAME)
    }

    private fun scheduleMorningBriefing(hour: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelayMs = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<MorningBriefingWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MorningBriefingWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    // ── Vosk Offline STT ──────────────────────────────────────────────────────

    fun downloadVoskModel() {
        viewModelScope.launch(Dispatchers.IO) {
            voskModelManager.downloadModel()
        }
    }

    fun deleteVoskModel() {
        voskModelManager.deleteModel()
        _uiState.update { it.copy(isVoskModelReady = false) }
    }

    // ── Spotify Auth ──────────────────────────────────────────────────────────

    fun startSpotifyLogin() {
        spotifyAuthManager.openAuthInBrowser()
    }

    fun handleSpotifyCallback(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = spotifyAuthManager.handleCallback(code)
            _uiState.update {
                it.copy(
                    isSpotifyAuthenticated = spotifyAuthManager.isAuthenticated(),
                    isSpotifyConfigured = spotifyAuthManager.isConfigured()
                )
            }
            if (success) ttsHelper.speak("Spotify hesabına uğurla daxil oldunuz.")
        }
    }

    fun logoutSpotify() {
        spotifyAuthManager.logout()
        _uiState.update { it.copy(isSpotifyAuthenticated = false) }
    }

    // ── Home Assistant Config ─────────────────────────────────────────────────

    fun saveHomeAssistantConfig(serverUrl: String, token: String) {
        preferences.edit()
            .putString("ha_server_url", serverUrl.trim())
            .putString("ha_token", token.trim())
            .apply()
        // Re-register HA tools with new credentials
        toolRegistry.registerContextDependentTools(context)
        _uiState.update {
            it.copy(
                isHomeAssistantConfigured = serverUrl.isNotBlank() && token.isNotBlank(),
                homeAssistantServerUrl = serverUrl
            )
        }
        if (serverUrl.isNotBlank() && token.isNotBlank()) {
            ttsHelper.speak("Home Assistant konfigureyşını yadda saxladım.")
        }
    }

    // ── Local Vision SLM ─────────────────────────────────────────────────────

    fun downloadLocalVisionModel() {
        viewModelScope.launch(Dispatchers.IO) {
            localVisionManager.downloadModel()
        }
    }

    fun deleteLocalVisionModel() {
        localVisionManager.deleteModel()
        _uiState.update { it.copy(isLocalVisionReady = false) }
    }

    // ── Custom Azerbaijani Neural Voice (Piper/Sherpa) ────────────────────────

    fun downloadNeuralTtsModel() {
        viewModelScope.launch(Dispatchers.IO) {
            neuralTtsManager.downloadModel()
        }
    }

    fun deleteNeuralTtsModel() {
        neuralTtsManager.deleteModel()
        _uiState.update { it.copy(isNeuralTtsReady = false) }
    }

    fun setNeuralVoiceGender(gender: NeuralVoiceGender) {
        neuralTtsManager.activeGender = gender
        _uiState.update { it.copy(activeNeuralVoice = gender.titleAz) }
        ttsHelper.speak("Səs profili dəyişdirildi: ${gender.titleAz}")
    }

    override fun onCleared() {
        super.onCleared()
        continuousSession?.stop()
        shakeDetector.stop()
        wakeWordDetector.stopContinuousHotwordListening()
        voiceHelper.stopListening()
        ttsHelper.shutdown()
        lowRamManager.unregister()
    }
}
