package com.example.jarvis.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.jarvis.core.JarvisConstants
import com.example.jarvis.data.local.dao.ConversationDao
import com.example.jarvis.data.local.dao.ExecutionLogDao
import com.example.jarvis.data.local.dao.MemoryDao
import com.example.jarvis.data.local.entity.ConversationEntity
import com.example.jarvis.data.local.entity.ExecutionLogEntity
import com.example.jarvis.data.local.entity.MemoryEntity

@Database(
    entities = [
        ConversationEntity::class,
        MemoryEntity::class,
        ExecutionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao
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
