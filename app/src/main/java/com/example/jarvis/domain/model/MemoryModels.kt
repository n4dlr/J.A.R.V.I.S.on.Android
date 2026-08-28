package com.example.jarvis.domain.model

enum class MessageSender {
    USER,
    JARVIS,
    SYSTEM
}

enum class TaskLifecycleStatus {
    PENDING,
    RUNNING,
    WAITING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class ConversationMessage(
    val id: Long = 0,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val intentId: String? = null,
    val toolResultStatus: ToolStatus? = null,
    val providerUsed: AIProviderType? = null
)

data class MemoryFact(
    val key: String,
    val value: String,
    val category: String = "general",
    val updatedAt: Long = System.currentTimeMillis()
)

data class UserPreference(
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

data class TaskRecord(
    val id: String,
    val title: String,
    val description: String = "",
    val status: TaskLifecycleStatus = TaskLifecycleStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorDetails: String? = null
)

data class DeviceStateSnapshot(
    val id: Long = 0,
    val batteryPct: Int,
    val isCharging: Boolean,
    val ramUsedPercent: Int,
    val storageFreeGb: Double,
    val networkType: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ExecutionLog(
    val id: Long = 0,
    val query: String,
    val intentId: String,
    val toolId: String,
    val status: ToolStatus,
    val output: String,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
