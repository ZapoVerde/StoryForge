package com.example.storyforge.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TurnLogDao {
    @Insert
    suspend fun insertAll(entities: List<TurnLogEntity>)

    @Query("SELECT * FROM turn_logs WHERE sessionId = :sessionId ORDER BY turnNumber ASC")
    fun getBySession(sessionId: String): Flow<List<TurnLogEntity>>

    @Query("DELETE FROM turn_logs WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}