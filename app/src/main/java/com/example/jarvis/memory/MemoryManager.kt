package com.example.jarvis.memory

import com.example.jarvis.core.LowRamManager
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.ExecutionLog
import com.example.jarvis.domain.model.MemoryFact
import com.example.jarvis.domain.repository.JarvisRepository
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.CopyOnWriteArrayList

class MemoryManager(
    private val repository: JarvisRepository,
    private val lowRamManager: LowRamManager
) {
    // In-memory bounded cache for instantaneous conversational context
    private val shortTermContext = CopyOnWriteArrayList<ConversationMessage>()

    fun getShortTermContext(): List<ConversationMessage> {
        val maxWindow = lowRamManager.getMaxContextWindowSize()
        return shortTermContext.takeLast(maxWindow)
    }

    suspend fun addMessage(message: ConversationMessage) {
        val savedId = repository.saveMessage(message)
        val savedMessage = message.copy(id = savedId)

        shortTermContext.add(savedMessage)
        trimContextIfNeeded()
    }

    fun trimContextIfNeeded() {
        val maxLimit = lowRamManager.getMaxContextWindowSize()
        while (shortTermContext.size > maxLimit) {
            shortTermContext.removeAt(0)
        }
    }

    suspend fun loadRecentHistory() {
        val maxLimit = lowRamManager.getMaxContextWindowSize()
        val history = repository.getRecentConversations(maxLimit)
        shortTermContext.clear()
        shortTermContext.addAll(history.reversed())
    }

    fun getLiveConversations(): Flow<List<ConversationMessage>> {
        return repository.getConversations()
    }

    suspend fun clearConversations() {
        shortTermContext.clear()
        repository.clearConversations()
    }

    suspend fun saveFact(key: String, value: String, category: String = "preference") {
        repository.saveFact(MemoryFact(key = key, value = value, category = category))
    }

    suspend fun getFact(key: String): String? {
        return repository.getFact(key)?.value
    }

    fun getAllFacts(): Flow<List<MemoryFact>> {
        return repository.getAllFacts()
    }

    suspend fun logExecution(log: ExecutionLog) {
        repository.saveExecutionLog(log)
    }

    fun getRecentLogs(limit: Int = 30): Flow<List<ExecutionLog>> {
        return repository.getRecentLogs(limit)
    }
}
