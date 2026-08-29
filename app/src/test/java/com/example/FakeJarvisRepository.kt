package com.example

import com.example.jarvis.data.local.entity.KnowledgeDocEntity
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.DeviceStateSnapshot
import com.example.jarvis.domain.model.ExecutionLog
import com.example.jarvis.domain.model.MemoryFact
import com.example.jarvis.domain.model.TaskLifecycleStatus
import com.example.jarvis.domain.model.TaskRecord
import com.example.jarvis.domain.model.UserPreference
import com.example.jarvis.domain.repository.JarvisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class FakeJarvisRepository : JarvisRepository {

    private val conversations = CopyOnWriteArrayList<ConversationMessage>()
    private val conversationsFlow = MutableStateFlow<List<ConversationMessage>>(emptyList())

    private val facts = ConcurrentHashMap<String, MemoryFact>()
    private val factsFlow = MutableStateFlow<List<MemoryFact>>(emptyList())

    private val preferences = ConcurrentHashMap<String, UserPreference>()
    private val preferencesFlow = MutableStateFlow<List<UserPreference>>(emptyList())

    private val tasks = ConcurrentHashMap<String, TaskRecord>()
    private val tasksFlow = MutableStateFlow<List<TaskRecord>>(emptyList())

    private val snapshots = CopyOnWriteArrayList<DeviceStateSnapshot>()
    private val knowledgeDocs = ConcurrentHashMap<String, KnowledgeDocEntity>()
    private val logs = CopyOnWriteArrayList<ExecutionLog>()
    private val logsFlow = MutableStateFlow<List<ExecutionLog>>(emptyList())

    override fun getConversations(): Flow<List<ConversationMessage>> = conversationsFlow.asStateFlow()

    override suspend fun getRecentConversations(limit: Int): List<ConversationMessage> {
        return conversations.takeLast(limit)
    }

    override suspend fun saveMessage(message: ConversationMessage): Long {
        conversations.add(message)
        conversationsFlow.value = conversations.toList()
        return conversations.size.toLong()
    }

    override suspend fun clearConversations() {
        conversations.clear()
        conversationsFlow.value = emptyList()
    }

    override fun getAllFacts(): Flow<List<MemoryFact>> = factsFlow.asStateFlow()

    override suspend fun getFact(key: String): MemoryFact? = facts[key]

    override suspend fun searchFacts(query: String): List<MemoryFact> {
        return facts.values.filter {
            it.key.contains(query, ignoreCase = true) ||
            it.value.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
        }
    }

    override suspend fun saveFact(fact: MemoryFact) {
        facts[fact.key] = fact
        factsFlow.value = facts.values.toList()
    }

    override suspend fun deleteFact(key: String): Int {
        val removed = facts.remove(key) != null
        if (removed) factsFlow.value = facts.values.toList()
        return if (removed) 1 else 0
    }

    override suspend fun clearFacts() {
        facts.clear()
        factsFlow.value = emptyList()
    }

    override fun getAllPreferences(): Flow<List<UserPreference>> = preferencesFlow.asStateFlow()

    override suspend fun getPreference(key: String): UserPreference? = preferences[key]

    override suspend fun savePreference(preference: UserPreference) {
        preferences[preference.key] = preference
        preferencesFlow.value = preferences.values.toList()
    }

    override suspend fun deletePreference(key: String) {
        preferences.remove(key)
        preferencesFlow.value = preferences.values.toList()
    }

    override fun getAllTasks(): Flow<List<TaskRecord>> = tasksFlow.asStateFlow()

    override suspend fun getTasksByStatus(status: TaskLifecycleStatus): List<TaskRecord> {
        return tasks.values.filter { it.status == status }
    }

    override suspend fun getTaskById(taskId: String): TaskRecord? = tasks[taskId]

    override suspend fun saveTask(task: TaskRecord) {
        tasks[task.id] = task
        tasksFlow.value = tasks.values.toList()
    }

    override suspend fun updateTaskStatus(
        taskId: String,
        status: TaskLifecycleStatus,
        completedAt: Long?,
        error: String?
    ) {
        val existing = tasks[taskId]
        if (existing != null) {
            val updated = existing.copy(
                status = status,
                completedAt = completedAt ?: existing.completedAt,
                errorDetails = error ?: existing.errorDetails
            )
            tasks[taskId] = updated
            tasksFlow.value = tasks.values.toList()
        }
    }

    override suspend fun deleteTask(taskId: String) {
        tasks.remove(taskId)
        tasksFlow.value = tasks.values.toList()
    }

    override suspend fun recordDeviceSnapshot(snapshot: DeviceStateSnapshot): Long {
        snapshots.add(snapshot)
        return snapshots.size.toLong()
    }

    override suspend fun getLatestSnapshot(): DeviceStateSnapshot? = snapshots.lastOrNull()

    override suspend fun getRecentSnapshots(limit: Int): List<DeviceStateSnapshot> = snapshots.takeLast(limit)

    override suspend fun getAllKnowledgeDocs(): List<KnowledgeDocEntity> = knowledgeDocs.values.toList()

    override suspend fun searchKnowledgeDocs(query: String): List<KnowledgeDocEntity> {
        return knowledgeDocs.values.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.content.contains(query, ignoreCase = true) ||
            it.tags.contains(query, ignoreCase = true)
        }
    }

    override suspend fun saveKnowledgeDoc(doc: KnowledgeDocEntity) {
        knowledgeDocs[doc.docId] = doc
    }

    override suspend fun deleteKnowledgeDoc(docId: String) {
        knowledgeDocs.remove(docId)
    }

    override fun getRecentLogs(limit: Int): Flow<List<ExecutionLog>> = logsFlow.asStateFlow()

    override suspend fun saveExecutionLog(log: ExecutionLog): Long {
        logs.add(log)
        logsFlow.value = logs.toList()
        return logs.size.toLong()
    }

    override suspend fun clearLogs() {
        logs.clear()
        logsFlow.value = emptyList()
    }
}
