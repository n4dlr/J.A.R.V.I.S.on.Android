package com.example.jarvis.domain.model

enum class RiskLevel {
    LOW,        // Read-only queries (battery, RAM, storage, time, reading)
    MEDIUM,     // Minor device adjustments (volume, flashlight, opening apps/settings)
    HIGH,       // Action affecting device state/data (lock screen, reminders, camera snap)
    CRITICAL    // Potentially destructive or sensitive actions (clearing data, system modifications)
}

data class RiskAssessment(
    val riskLevel: RiskLevel,
    val requiresExplicitConfirmation: Boolean,
    val rationale: String,
    val securityWarnings: List<String> = emptyList()
)

data class PendingActionConfirmation(
    val id: String,
    val toolId: String,
    val structuredIntent: StructuredIntent,
    val riskAssessment: RiskAssessment,
    val userPromptText: String,
    val timestamp: Long = System.currentTimeMillis()
)
