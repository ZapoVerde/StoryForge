package com.example.storyforge.persistence

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun createSession(name: String, gameStateJson: String): String
    suspend fun getSession(id: String): GameSessionEntity?
    fun getAllSessions(): Flow<List<GameSessionEntity>>
    suspend fun deleteSession(id: String)
    suspend fun updateLastPlayed(id: String)
}