package com.example.storyforge.persistence

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [GameSessionEntity::class, TurnLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StoryForgeDatabase : RoomDatabase() {
    abstract fun gameSessionDao(): GameSessionDao
    abstract fun turnLogDao(): TurnLogDao

    companion object {
        @Volatile
        private var INSTANCE: StoryForgeDatabase? = null

        fun getInstance(context: Context): StoryForgeDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StoryForgeDatabase::class.java,
                    "storyforge.db"
                ).build().also { INSTANCE = it }
            }
    }
}