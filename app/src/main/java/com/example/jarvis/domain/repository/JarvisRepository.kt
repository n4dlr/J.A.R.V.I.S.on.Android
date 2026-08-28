package com.example.jarvis.domain.repository

import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.ExecutionLog
import com.example.jarvis.domain.model.MemoryFact
import kotlinx.coroutines.flow.Flow

interface JarvisRepository {
    fun getConversations(): Flow<List<ConversationMessage>>
    suspend fun getRecentConversations(limit: Int): List<ConversationMessage>
    suspend fun saveMessage(message: ConversationMessage): Long
    suspend fun clearConversations()

    fun getAllFacts(): Flow<List<MemoryFact>>
    suspend fun getFact(key: String): MemoryFact?
    suspend fun saveFact(fact: MemoryFact)
    suspend fun deleteFact(key: String)
    suspend fun clearFacts()

    fun getRecentLogs(limit: Int = 50): Flow<List<ExecutionLog>>
    suspend fun saveExecutionLog(log: ExecutionLog): Long
    suspend fun clearLogs()
}
