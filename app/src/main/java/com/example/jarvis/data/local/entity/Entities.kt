package com.example.jarvis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.jarvis.domain.model.AIProviderType
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.ExecutionLog
import com.example.jarvis.domain.model.MemoryFact
import com.example.jarvis.domain.model.MessageSender
import com.example.jarvis.domain.model.ToolStatus

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val intentId: String? = null,
    val toolResultStatus: String? = null,
    val providerUsed: String? = null
) {
    fun toDomain(): ConversationMessage =
        ConversationMessage(
            id = id,
            sender = try { MessageSender.valueOf(sender) } catch (_: Exception) { MessageSender.SYSTEM },
            text = text,
            timestamp = timestamp,
            intentId = intentId,
            toolResultStatus = toolResultStatus?.let { try { ToolStatus.valueOf(it) } catch (_: Exception) { null } },
            providerUsed = providerUsed?.let { try { AIProviderType.valueOf(it) } catch (_: Exception) { null } }
        )

    companion object {
        fun fromDomain(msg: ConversationMessage): ConversationEntity =
            ConversationEntity(
                id = msg.id,
                sender = msg.sender.name,
                text = msg.text,
                timestamp = msg.timestamp,
                intentId = msg.intentId,
                toolResultStatus = msg.toolResultStatus?.name,
                providerUsed = msg.providerUsed?.name
            )
    }
}

@Entity(tableName = "memory_facts")
data class MemoryEntity(
    @PrimaryKey val factKey: String,
    val factValue: String,
    val category: String = "general",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): MemoryFact =
        MemoryFact(
            key = factKey,
            value = factValue,
            category = category,
            updatedAt = updatedAt
        )

    companion object {
        fun fromDomain(fact: MemoryFact): MemoryEntity =
            MemoryEntity(
                factKey = fact.key,
                factValue = fact.value,
                category = fact.category,
                updatedAt = fact.updatedAt
            )
    }
}

@Entity(tableName = "execution_logs")
data class ExecutionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val intentId: String,
    val toolId: String,
    val status: String,
    val output: String,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): ExecutionLog =
        ExecutionLog(
            id = id,
            query = query,
            intentId = intentId,
            toolId = toolId,
            status = try { ToolStatus.valueOf(status) } catch (_: Exception) { ToolStatus.FAILED },
            output = output,
            durationMs = durationMs,
            timestamp = timestamp
        )

    companion object {
        fun fromDomain(log: ExecutionLog): ExecutionLogEntity =
            ExecutionLogEntity(
                id = log.id,
                query = log.query,
                intentId = log.intentId,
                toolId = log.toolId,
                status = log.status.name,
                output = log.output,
                durationMs = log.durationMs,
                timestamp = log.timestamp
            )
    }
}
