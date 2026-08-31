package com.example.jarvis.domain.model

/**
 * Standardized Error Taxonomy for J.A.R.V.I.S. V2
 */
enum class ErrorCode {
    STT_ERROR,
    MODEL_ERROR,
    INTENT_ERROR,
    APP_RESOLUTION_ERROR,
    PERMISSION_ERROR,
    ACCESSIBILITY_ERROR,
    TOOL_ERROR,
    UI_AUTOMATION_ERROR,
    VERIFICATION_ERROR,
    TIMEOUT_ERROR,
    ANDROID_RESTRICTION,
    NETWORK_ERROR,
    STORAGE_ERROR,
    SECURITY_ERROR,
    UNKNOWN_ERROR
}

data class JarvisError(
    val code: ErrorCode,
    val message: String,
    val recoverable: Boolean = true,
    val recommendedAction: String = "",
    val underlyingException: Throwable? = null
)
