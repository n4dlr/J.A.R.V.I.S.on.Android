package com.example.jarvis.domain.model

enum class MessageSender {
    USER,
    JARVIS,
    SYSTEM
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
