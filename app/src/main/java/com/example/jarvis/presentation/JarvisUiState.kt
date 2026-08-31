package com.example.jarvis.presentation

import com.example.jarvis.automation.DiagnosticsTrace
import com.example.jarvis.core.MemoryTelemetry
import com.example.jarvis.domain.model.AIProviderType
import com.example.jarvis.domain.model.PendingActionConfirmation
import com.example.jarvis.domain.model.ProviderHealth
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.permissions.PermissionStatusInfo

data class JarvisUiState(
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isProcessing: Boolean = false,
    val audioLevel: Float = 0f,
    val currentInputText: String = "",
    val activeProviderType: AIProviderType = AIProviderType.FALLBACK_HYBRID,
    val providerHealth: ProviderHealth? = null,
    val telemetry: MemoryTelemetry? = null,
    val pendingConfirmation: PendingActionConfirmation? = null,
    val lastToolResult: ToolResult? = null,
    val permissionStatuses: List<PermissionStatusInfo> = emptyList(),
    val isLowRamModeEnforced: Boolean = false,
    val speechError: String? = null,
    val showSettingsSheet: Boolean = false,
    val showDiagnosticsScreen: Boolean = false,
    val isTtsEnabled: Boolean = false,
    val hasGeminiApiKey: Boolean = false,
    val performanceMetrics: com.example.jarvis.core.PerformanceMetrics? = null,
    val confirmationProfile: com.example.jarvis.security.ConfirmationProfile = com.example.jarvis.security.ConfirmationProfile.STANDARD,
    val recognizedSpeechPreview: String = "",
    val executionStage: String? = null,
    val diagnosticsTrace: DiagnosticsTrace? = null,
    val activeLanguage: String = "az-AZ",
    val isWakeWordEnabled: Boolean = false,
    val isShakeToWakeEnabled: Boolean = false,
    val isNotificationReadoutEnabled: Boolean = false,
    val isModelDownloaded: Boolean = false,
    val modelDownloadProgress: Int? = null,
    val modelDownloadError: String? = null,
    // Phase 6 — Continuous Voice Session
    val isContinuousSessionActive: Boolean = false,
    // Phase 6 — Morning Briefing
    val isMorningBriefingEnabled: Boolean = false,
    val briefingHour: Int = 8,
    // Phase 6 — Vosk Offline STT
    val isVoskModelReady: Boolean = false,
    val voskDownloadProgress: Int? = null,
    val activeSttProvider: String = "Android STT",
    // Phase 6 — Spotify
    val isSpotifyAuthenticated: Boolean = false,
    val isSpotifyConfigured: Boolean = false,
    // Phase 6 — Smart Home / Home Assistant
    val isHomeAssistantConfigured: Boolean = false,
    val homeAssistantServerUrl: String = "",
    // Phase 7 — Local Offline Vision SLM
    val isLocalVisionReady: Boolean = false,
    val localVisionDownloadProgress: Int? = null,
    // Phase 7 — Custom Azerbaijani Neural Voice (Piper/Sherpa)
    val isNeuralTtsReady: Boolean = false,
    val neuralTtsDownloadProgress: Int? = null,
    val activeNeuralVoice: String = "JARVIS Kişi Səsi (Studiya)"
)

