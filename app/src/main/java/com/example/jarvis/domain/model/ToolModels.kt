package com.example.jarvis.domain.model

enum class ToolStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED,
    DENIED,
    PERMISSION_REQUIRED,
    ACCESSIBILITY_REQUIRED,
    VERIFICATION_FAILED,
    UNSUPPORTED,
    CONFIRMATION_REQUIRED,
    TIMEOUT,
    CANCELLED
}

/** Pre-flight capability detection result for a tool. */
enum class CapabilityStatus {
    /** Tool is fully supported and permissions are granted. */
    SUPPORTED,
    /** Tool cannot work on this Android version or device. */
    UNSUPPORTED,
    /** Tool needs one or more runtime permissions. */
    PERMISSION_REQUIRED,
    /** Tool needs non-runtime special access (Accessibility, Notification Listener, Device Admin, etc.) */
    SPECIAL_ACCESS_REQUIRED
}

/** Summary of a single capability check. */
data class CapabilityInfo(
    val toolId: String,
    val status: CapabilityStatus,
    val reason: String,
    val missingPermissions: List<String> = emptyList(),
    val minApiLevel: Int = 0,
    val currentApiLevel: Int = android.os.Build.VERSION.SDK_INT
)

data class ToolParameter(
    val name: String,
    val type: String,
    val isRequired: Boolean = true,
    val description: String = "",
    val defaultValue: String? = null
)

data class ToolResult(
    val toolId: String,
    val status: ToolStatus,
    val outputMessage: String,
    val rawData: Map<String, Any?> = emptyMap(),
    val errorDetails: String? = null,
    val missingPermissions: List<String> = emptyList(),
    val verificationEvidence: String? = null,
    val executionDurationMs: Long = 0L
) {
    val isSuccess: Boolean get() = status == ToolStatus.SUCCESS
    val isPartialSuccess: Boolean get() = status == ToolStatus.PARTIAL_SUCCESS

    companion object {
        fun success(
            toolId: String,
            message: String,
            data: Map<String, Any?> = emptyMap(),
            verification: String? = null
        ): ToolResult =
            ToolResult(
                toolId = toolId,
                status = ToolStatus.SUCCESS,
                outputMessage = message,
                rawData = data,
                verificationEvidence = verification
            )

        fun partialSuccess(
            toolId: String,
            message: String,
            data: Map<String, Any?> = emptyMap(),
            reason: String? = null
        ): ToolResult =
            ToolResult(
                toolId = toolId,
                status = ToolStatus.PARTIAL_SUCCESS,
                outputMessage = message,
                rawData = data,
                errorDetails = reason
            )

        fun failed(toolId: String, error: String): ToolResult =
            ToolResult(
                toolId = toolId,
                status = ToolStatus.FAILED,
                outputMessage = error,
                errorDetails = error
            )

        fun permissionRequired(toolId: String, permissions: List<String>, message: String): ToolResult =
            ToolResult(
                toolId = toolId,
                status = ToolStatus.PERMISSION_REQUIRED,
                outputMessage = message,
                missingPermissions = permissions
            )

        fun accessibilityRequired(toolId: String, message: String): ToolResult =
            ToolResult(
                toolId = toolId,
                status = ToolStatus.ACCESSIBILITY_REQUIRED,
                outputMessage = message,
                missingPermissions = listOf("android.permission.BIND_ACCESSIBILITY_SERVICE")
            )

        fun verificationFailed(toolId: String, message: String, data: Map<String, Any?> = emptyMap()): ToolResult =
            ToolResult(
                toolId = toolId,
                status = ToolStatus.VERIFICATION_FAILED,
                outputMessage = message,
                rawData = data,
                errorDetails = "VERIFICATION_FAILED"
            )

        fun confirmationRequired(toolId: String, message: String): ToolResult =
            ToolResult(
                toolId = toolId,
                status = ToolStatus.CONFIRMATION_REQUIRED,
                outputMessage = message
            )

        fun denied(toolId: String, reason: String): ToolResult =
            ToolResult(
                toolId = toolId,
                status = ToolStatus.DENIED,
                outputMessage = reason
            )

        fun unsupported(toolId: String, reason: String): ToolResult =
            ToolResult(
                toolId = toolId,
                status = ToolStatus.UNSUPPORTED,
                outputMessage = reason
            )

        fun timeout(toolId: String, message: String): ToolResult =
            ToolResult(
                toolId = toolId,
                status = ToolStatus.TIMEOUT,
                outputMessage = message,
                errorDetails = "TIMEOUT"
            )

        fun specialAccessRequired(toolId: String, accessType: String, message: String): ToolResult =
            ToolResult(
                toolId = toolId,
                status = ToolStatus.PERMISSION_REQUIRED,
                outputMessage = message,
                missingPermissions = listOf(accessType),
                errorDetails = "SPECIAL_ACCESS: $accessType"
            )
    }
}
