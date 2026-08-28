package com.example.jarvis.presentation

import com.example.jarvis.core.MemoryTelemetry
import com.example.jarvis.domain.model.AIProviderType
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.ExecutionLog
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
    val isTtsEnabled: Boolean = false,
    val hasGeminiApiKey: Boolean = false,
    val performanceMetrics: com.example.jarvis.core.PerformanceMetrics? = null,
    val confirmationProfile: com.example.jarvis.security.ConfirmationProfile = com.example.jarvis.security.ConfirmationProfile.STANDARD
)
