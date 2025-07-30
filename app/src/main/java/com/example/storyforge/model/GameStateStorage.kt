package com.example.storyforge.model

import android.content.Context
import android.util.Log
import com.example.storyforge.save.GameSnapshot
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object GameStateStorage {
    private const val FILE_NAME = "game_state.json"

    fun save(context: Context, state: GameState) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val json = Json.encodeToString(state)
            file.writeText(json)
        } catch (_: Exception) {
            // Fail silently for now; you can log later
        }
    }

    fun load(context: Context): GameState {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return GameState()

        return try {
            val json = file.readText()
            Json.decodeFromString<GameState>(json)
        } catch (_: Exception) {
            GameState()
        }
    }

    fun saveSnapshot(context: Context, snapshot: GameSnapshot) {
        try {
            val file = File(context.filesDir, "latest_snapshot.json")
            file.writeText(Json.encodeToString(snapshot))
        } catch (e: Exception) {
            Log.e("GameStateStorage", "Failed to save snapshot", e)
        }
    }

    fun loadSnapshot(context: Context): GameSnapshot? {
        val file = File(context.filesDir, "last_snapshot.json")
        if (!file.exists()) return null

        return try {
            val json = file.readText()
            Json.decodeFromString(GameSnapshot.serializer(), json)
        } catch (e: Exception) {
            Log.e("GameStateStorage", "Failed to load snapshot", e)
            null
        }
    }





}
