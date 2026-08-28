package com.example.jarvis.domain.repository

import com.example.jarvis.data.local.entity.KnowledgeDocEntity
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.DeviceStateSnapshot
import com.example.jarvis.domain.model.ExecutionLog
import com.example.jarvis.domain.model.MemoryFact
import com.example.jarvis.domain.model.TaskLifecycleStatus
import com.example.jarvis.domain.model.TaskRecord
import com.example.jarvis.domain.model.UserPreference
import kotlinx.coroutines.flow.Flow

interface JarvisRepository {
    // Conversations
    fun getConversations(): Flow<List<ConversationMessage>>
    suspend fun getRecentConversations(limit: Int): List<ConversationMessage>
    suspend fun saveMessage(message: ConversationMessage): Long
    suspend fun clearConversations()

    // Memory Facts
    fun getAllFacts(): Flow<List<MemoryFact>>
    suspend fun getFact(key: String): MemoryFact?
    suspend fun searchFacts(query: String): List<MemoryFact>
    suspend fun saveFact(fact: MemoryFact)
    suspend fun deleteFact(key: String): Int
    suspend fun clearFacts()

    // User Preferences
    fun getAllPreferences(): Flow<List<UserPreference>>
    suspend fun getPreference(key: String): UserPreference?
    suspend fun savePreference(preference: UserPreference)
    suspend fun deletePreference(key: String)

    // Tasks & Workflows
    fun getAllTasks(): Flow<List<TaskRecord>>
    suspend fun getTasksByStatus(status: TaskLifecycleStatus): List<TaskRecord>
    suspend fun getTaskById(taskId: String): TaskRecord?
    suspend fun saveTask(task: TaskRecord)
    suspend fun updateTaskStatus(taskId: String, status: TaskLifecycleStatus, completedAt: Long? = null, error: String? = null)
    suspend fun deleteTask(taskId: String)

    // Device Snapshots
    suspend fun recordDeviceSnapshot(snapshot: DeviceStateSnapshot): Long
    suspend fun getLatestSnapshot(): DeviceStateSnapshot?
    suspend fun getRecentSnapshots(limit: Int): List<DeviceStateSnapshot>

    // Local Knowledge Documents (RAG)
    suspend fun getAllKnowledgeDocs(): List<KnowledgeDocEntity>
    suspend fun searchKnowledgeDocs(query: String): List<KnowledgeDocEntity>
    suspend fun saveKnowledgeDoc(doc: KnowledgeDocEntity)
    suspend fun deleteKnowledgeDoc(docId: String)

    // Execution Logs
    fun getRecentLogs(limit: Int = 50): Flow<List<ExecutionLog>>
    suspend fun saveExecutionLog(log: ExecutionLog): Long
    suspend fun clearLogs()
}
