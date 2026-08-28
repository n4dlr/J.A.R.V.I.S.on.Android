package com.example.jarvis.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jarvis.data.local.entity.ConversationEntity
import com.example.jarvis.data.local.entity.ExecutionLogEntity
import com.example.jarvis.data.local.entity.MemoryEntity
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFact(fact: MemoryEntity)

    @Query("DELETE FROM memory_facts WHERE factKey = :key")
    suspend fun deleteFact(key: String)

    @Query("DELETE FROM memory_facts")
    suspend fun clearMemory()
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
