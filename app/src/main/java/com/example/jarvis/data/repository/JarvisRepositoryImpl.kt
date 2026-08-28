package com.example.jarvis.data.repository

import com.example.jarvis.data.local.JarvisDatabase
import com.example.jarvis.data.local.entity.ConversationEntity
import com.example.jarvis.data.local.entity.DeviceStateEntity
import com.example.jarvis.data.local.entity.ExecutionLogEntity
import com.example.jarvis.data.local.entity.KnowledgeDocEntity
import com.example.jarvis.data.local.entity.MemoryEntity
import com.example.jarvis.data.local.entity.PreferenceEntity
import com.example.jarvis.data.local.entity.TaskEntity
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.DeviceStateSnapshot
import com.example.jarvis.domain.model.ExecutionLog
import com.example.jarvis.domain.model.MemoryFact
import com.example.jarvis.domain.model.TaskLifecycleStatus
import com.example.jarvis.domain.model.TaskRecord
import com.example.jarvis.domain.model.UserPreference
import com.example.jarvis.domain.repository.JarvisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JarvisRepositoryImpl(
    private val database: JarvisDatabase
) : JarvisRepository {

    private val conversationDao = database.conversationDao()
    private val memoryDao = database.memoryDao()
    private val preferenceDao = database.preferenceDao()
    private val taskDao = database.taskDao()
    private val deviceStateDao = database.deviceStateDao()
    private val knowledgeDocDao = database.knowledgeDocDao()
    private val executionLogDao = database.executionLogDao()

    // Conversations
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

    // Memory Facts
    override fun getAllFacts(): Flow<List<MemoryFact>> {
        return memoryDao.getAllFacts().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getFact(key: String): MemoryFact? {
        return memoryDao.getFactByKey(key)?.toDomain()
    }

    override suspend fun searchFacts(query: String): List<MemoryFact> {
        return memoryDao.searchFacts(query).map { it.toDomain() }
    }

    override suspend fun saveFact(fact: MemoryFact) {
        memoryDao.saveFact(MemoryEntity.fromDomain(fact))
    }

    override suspend fun deleteFact(key: String): Int {
        return memoryDao.deleteFact(key)
    }

    override suspend fun clearFacts() {
        memoryDao.clearMemory()
    }

    // User Preferences
    override fun getAllPreferences(): Flow<List<UserPreference>> {
        return preferenceDao.getAllPreferences().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getPreference(key: String): UserPreference? {
        return preferenceDao.getPreference(key)?.toDomain()
    }

    override suspend fun savePreference(preference: UserPreference) {
        preferenceDao.savePreference(PreferenceEntity.fromDomain(preference))
    }

    override suspend fun deletePreference(key: String) {
        preferenceDao.deletePreference(key)
    }

    // Tasks & Workflows
    override fun getAllTasks(): Flow<List<TaskRecord>> {
        return taskDao.getAllTasks().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getTasksByStatus(status: TaskLifecycleStatus): List<TaskRecord> {
        return taskDao.getTasksByStatus(status.name).map { it.toDomain() }
    }

    override suspend fun getTaskById(taskId: String): TaskRecord? {
        return taskDao.getTaskById(taskId)?.toDomain()
    }

    override suspend fun saveTask(task: TaskRecord) {
        taskDao.insertTask(TaskEntity.fromDomain(task))
    }

    override suspend fun updateTaskStatus(
        taskId: String,
        status: TaskLifecycleStatus,
        completedAt: Long?,
        error: String?
    ) {
        taskDao.updateTaskStatus(taskId, status.name, completedAt, error)
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteTask(taskId)
    }

    // Device Snapshots
    override suspend fun recordDeviceSnapshot(snapshot: DeviceStateSnapshot): Long {
        return deviceStateDao.insertSnapshot(DeviceStateEntity.fromDomain(snapshot))
    }

    override suspend fun getLatestSnapshot(): DeviceStateSnapshot? {
        return deviceStateDao.getLatestSnapshot()?.toDomain()
    }

    override suspend fun getRecentSnapshots(limit: Int): List<DeviceStateSnapshot> {
        return deviceStateDao.getRecentSnapshots(limit).map { it.toDomain() }
    }

    // Local Knowledge Documents (RAG)
    override suspend fun getAllKnowledgeDocs(): List<KnowledgeDocEntity> {
        return knowledgeDocDao.getAllDocuments()
    }

    override suspend fun searchKnowledgeDocs(query: String): List<KnowledgeDocEntity> {
        return knowledgeDocDao.searchDocuments(query)
    }

    override suspend fun saveKnowledgeDoc(doc: KnowledgeDocEntity) {
        knowledgeDocDao.insertDocument(doc)
    }

    override suspend fun deleteKnowledgeDoc(docId: String) {
        knowledgeDocDao.deleteDocument(docId)
    }

    // Execution Logs
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
