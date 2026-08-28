package com.example.jarvis.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.jarvis.core.JarvisConstants
import com.example.jarvis.data.local.dao.ConversationDao
import com.example.jarvis.data.local.dao.DeviceStateDao
import com.example.jarvis.data.local.dao.ExecutionLogDao
import com.example.jarvis.data.local.dao.KnowledgeDocDao
import com.example.jarvis.data.local.dao.MemoryDao
import com.example.jarvis.data.local.dao.PreferenceDao
import com.example.jarvis.data.local.dao.TaskDao
import com.example.jarvis.data.local.entity.ConversationEntity
import com.example.jarvis.data.local.entity.DeviceStateEntity
import com.example.jarvis.data.local.entity.ExecutionLogEntity
import com.example.jarvis.data.local.entity.KnowledgeDocEntity
import com.example.jarvis.data.local.entity.MemoryEntity
import com.example.jarvis.data.local.entity.PreferenceEntity
import com.example.jarvis.data.local.entity.TaskEntity

@Database(
    entities = [
        ConversationEntity::class,
        MemoryEntity::class,
        PreferenceEntity::class,
        TaskEntity::class,
        DeviceStateEntity::class,
        KnowledgeDocEntity::class,
        ExecutionLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun taskDao(): TaskDao
    abstract fun deviceStateDao(): DeviceStateDao
    abstract fun knowledgeDocDao(): KnowledgeDocDao
    abstract fun executionLogDao(): ExecutionLogDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    JarvisConstants.DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
