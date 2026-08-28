package com.example.jarvis.data.repository

import com.example.jarvis.data.local.JarvisDatabase
import com.example.jarvis.data.local.entity.ConversationEntity
import com.example.jarvis.data.local.entity.ExecutionLogEntity
import com.example.jarvis.data.local.entity.MemoryEntity
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.ExecutionLog
import com.example.jarvis.domain.model.MemoryFact
import com.example.jarvis.domain.repository.JarvisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JarvisRepositoryImpl(
    private val database: JarvisDatabase
) : JarvisRepository {

    private val conversationDao = database.conversationDao()
    private val memoryDao = database.memoryDao()
    private val executionLogDao = database.executionLogDao()

    override fun getConversations(): Flow<List<ConversationMessage>> {
        return conversationDao.getAllConversations().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getRecentConversations(limit: Int): List<ConversationMessage> {
        return conversationDao.getRecentConversations(limit).map { it.toDomain() }
    }

    override suspend fun saveMessage(message: ConversationMessage): Long {
        return conversationDao.insertMessage(ConversationEntity.fromDomain(message))
    }

    override suspend fun clearConversations() {
        conversationDao.clearAll()
    }

    override fun getAllFacts(): Flow<List<MemoryFact>> {
        return memoryDao.getAllFacts().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getFact(key: String): MemoryFact? {
        return memoryDao.getFactByKey(key)?.toDomain()
    }

    override suspend fun saveFact(fact: MemoryFact) {
        memoryDao.saveFact(MemoryEntity.fromDomain(fact))
    }

    override suspend fun deleteFact(key: String) {
        memoryDao.deleteFact(key)
    }

    override suspend fun clearFacts() {
        memoryDao.clearMemory()
    }

    override fun getRecentLogs(limit: Int): Flow<List<ExecutionLog>> {
        return executionLogDao.getRecentLogs(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveExecutionLog(log: ExecutionLog): Long {
        return executionLogDao.insertLog(ExecutionLogEntity.fromDomain(log))
    }

    override suspend fun clearLogs() {
        executionLogDao.clearLogs()
    }
}
