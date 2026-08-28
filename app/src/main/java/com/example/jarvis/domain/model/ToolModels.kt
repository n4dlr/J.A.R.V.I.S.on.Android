package com.example.jarvis.domain.model

enum class ToolStatus {
    SUCCESS,
    FAILED,
    DENIED,
    PERMISSION_REQUIRED,
    UNSUPPORTED,
    CONFIRMATION_REQUIRED
}

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
    val missingPermissions: List<String> = emptyList()
) {
    val isSuccess: Boolean get() = status == ToolStatus.SUCCESS

    companion object {
        fun success(toolId: String, message: String, data: Map<String, Any?> = emptyMap()): ToolResult =
            ToolResult(
                toolId = toolId,
                status = ToolStatus.SUCCESS,
                outputMessage = message,
                rawData = data
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
    }
}
