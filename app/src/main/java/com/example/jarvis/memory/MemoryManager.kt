package com.example.jarvis.memory

import com.example.jarvis.core.LowRamManager
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.DeviceStateSnapshot
import com.example.jarvis.domain.model.ExecutionLog
import com.example.jarvis.domain.model.MemoryFact
import com.example.jarvis.domain.model.TaskLifecycleStatus
import com.example.jarvis.domain.model.TaskRecord
import com.example.jarvis.domain.model.UserPreference
import com.example.jarvis.domain.repository.JarvisRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class MemoryManager(
    private val repository: JarvisRepository,
    private val lowRamManager: LowRamManager
) {
    // In-memory bounded cache for instantaneous conversational context
    private val shortTermContext = CopyOnWriteArrayList<ConversationMessage>()

    // ── Conversations ────────────────────────────────────────────────────────

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

    // ── Facts & Knowledge ("Bunu yadda saxla" / "Bunu unut") ─────────────────

    suspend fun saveFact(key: String, value: String, category: String = "general") {
        repository.saveFact(MemoryFact(key = key, value = value, category = category))
    }

    suspend fun rememberFact(rawFact: String, category: String = "facts"): String {
        val clean = rawFact.trim()
        val key = if (clean.length > 50) clean.take(47) + "..." else clean
        repository.saveFact(MemoryFact(key = key, value = clean, category = category))
        return clean
    }

    suspend fun forgetFact(query: String): Boolean {
        val deletedCount = repository.deleteFact(query.trim())
        return deletedCount > 0
    }

    suspend fun getFact(key: String): String? {
        return repository.getFact(key)?.value
    }

    suspend fun searchFacts(query: String): List<MemoryFact> {
        return repository.searchFacts(query.trim())
    }

    fun getAllFacts(): Flow<List<MemoryFact>> {
        return repository.getAllFacts()
    }

    suspend fun clearFacts() {
        repository.clearFacts()
    }

    // ── User Preferences ─────────────────────────────────────────────────────

    suspend fun setPreference(key: String, value: String) {
        repository.savePreference(UserPreference(key = key, value = value))
    }

    suspend fun getPreference(key: String): String? {
        return repository.getPreference(key)?.value
    }

    fun getAllPreferences(): Flow<List<UserPreference>> {
        return repository.getAllPreferences()
    }

    // ── Tasks & Lifecycle ────────────────────────────────────────────────────

    suspend fun createTask(title: String, description: String = ""): TaskRecord {
        val task = TaskRecord(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            status = TaskLifecycleStatus.PENDING
        )
        repository.saveTask(task)
        return task
    }

    suspend fun updateTaskStatus(
        taskId: String,
        status: TaskLifecycleStatus,
        error: String? = null
    ) {
        val completedAt = if (status == TaskLifecycleStatus.COMPLETED || status == TaskLifecycleStatus.FAILED || status == TaskLifecycleStatus.CANCELLED) {
            System.currentTimeMillis()
        } else null
        repository.updateTaskStatus(taskId, status, completedAt, error)
    }

    suspend fun getTasksByStatus(status: TaskLifecycleStatus): List<TaskRecord> {
        return repository.getTasksByStatus(status)
    }

    fun getAllTasks(): Flow<List<TaskRecord>> {
        return repository.getAllTasks()
    }

    // ── Device State Snapshots ───────────────────────────────────────────────

    suspend fun recordDeviceSnapshot(
        batteryPct: Int,
        isCharging: Boolean,
        ramUsedPercent: Int,
        storageFreeGb: Double,
        networkType: String
    ): Long {
        return repository.recordDeviceSnapshot(
            DeviceStateSnapshot(
                batteryPct = batteryPct,
                isCharging = isCharging,
                ramUsedPercent = ramUsedPercent,
                storageFreeGb = storageFreeGb,
                networkType = networkType
            )
        )
    }

    suspend fun getLatestDeviceSnapshot(): DeviceStateSnapshot? {
        return repository.getLatestSnapshot()
    }

    suspend fun getRecentDeviceSnapshots(limit: Int = 10): List<DeviceStateSnapshot> {
        return repository.getRecentSnapshots(limit)
    }

    // ── Audit Logs ───────────────────────────────────────────────────────────

    suspend fun logExecution(log: ExecutionLog) {
        repository.saveExecutionLog(log)
    }

    fun getRecentLogs(limit: Int = 30): Flow<List<ExecutionLog>> {
        return repository.getRecentLogs(limit)
    }
}
