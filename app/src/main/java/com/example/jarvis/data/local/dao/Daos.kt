package com.example.jarvis.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jarvis.data.local.entity.ConversationEntity
import com.example.jarvis.data.local.entity.DeviceStateEntity
import com.example.jarvis.data.local.entity.ExecutionLogEntity
import com.example.jarvis.data.local.entity.KnowledgeDocEntity
import com.example.jarvis.data.local.entity.MemoryEntity
import com.example.jarvis.data.local.entity.PreferenceEntity
import com.example.jarvis.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp ASC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentConversations(limit: Int): List<ConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ConversationEntity): Long

    @Query("DELETE FROM conversations")
    suspend fun clearAll()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_facts ORDER BY updatedAt DESC")
    fun getAllFacts(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory_facts WHERE factKey = :key LIMIT 1")
    suspend fun getFactByKey(key: String): MemoryEntity?

    @Query("SELECT * FROM memory_facts WHERE factKey LIKE '%' || :query || '%' OR factValue LIKE '%' || :query || '%'")
    suspend fun searchFacts(query: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFact(fact: MemoryEntity)

    @Query("DELETE FROM memory_facts WHERE factKey = :key OR factKey LIKE '%' || :key || '%'")
    suspend fun deleteFact(key: String): Int

    @Query("DELETE FROM memory_facts")
    suspend fun clearMemory()
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM user_preferences")
    fun getAllPreferences(): Flow<List<PreferenceEntity>>

    @Query("SELECT * FROM user_preferences WHERE prefKey = :key LIMIT 1")
    suspend fun getPreference(key: String): PreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreference(pref: PreferenceEntity)

    @Query("DELETE FROM user_preferences WHERE prefKey = :key")
    suspend fun deletePreference(key: String)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM task_records ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task_records WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getTasksByStatus(status: String): List<TaskEntity>

    @Query("SELECT * FROM task_records WHERE taskId = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("UPDATE task_records SET status = :status, completedAt = :completedAt, errorDetails = :error WHERE taskId = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: String, completedAt: Long?, error: String?)

    @Query("DELETE FROM task_records WHERE taskId = :taskId")
    suspend fun deleteTask(taskId: String)
}

@Dao
interface DeviceStateDao {
    @Query("SELECT * FROM device_snapshots ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSnapshots(limit: Int): List<DeviceStateEntity>

    @Query("SELECT * FROM device_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSnapshot(): DeviceStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: DeviceStateEntity): Long

    @Query("DELETE FROM device_snapshots WHERE timestamp < :olderThanTimestamp")
    suspend fun pruneOldSnapshots(olderThanTimestamp: Long)
}

@Dao
interface KnowledgeDocDao {
    @Query("SELECT * FROM knowledge_documents")
    suspend fun getAllDocuments(): List<KnowledgeDocEntity>

    @Query("SELECT * FROM knowledge_documents WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    suspend fun searchDocuments(query: String): List<KnowledgeDocEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: KnowledgeDocEntity)

    @Query("DELETE FROM knowledge_documents WHERE docId = :docId")
    suspend fun deleteDocument(docId: String)
}

@Dao
interface ExecutionLogDao {
    @Query("SELECT * FROM execution_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<ExecutionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ExecutionLogEntity): Long

    @Query("DELETE FROM execution_logs")
    suspend fun clearLogs()
}
