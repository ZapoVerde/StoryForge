package com.example.storyforge.persistence

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GameSessionEntity): Long

    @Query("SELECT * FROM game_sessions WHERE id = :id")
    suspend fun getById(id: String): GameSessionEntity?

    @Query("SELECT * FROM game_sessions ORDER BY lastPlayed DESC")
    fun getAll(): Flow<List<GameSessionEntity>>

    @Query("SELECT * FROM game_sessions WHERE isActive = 1")
    fun getActive(): Flow<GameSessionEntity?>

    @Delete
    suspend fun delete(entity: GameSessionEntity)

    @Query("DELETE FROM game_sessions WHERE id = :id")
    suspend fun deleteById(id: String)
}