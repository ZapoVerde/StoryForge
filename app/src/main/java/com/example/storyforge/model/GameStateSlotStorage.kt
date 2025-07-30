package com.example.storyforge.model

import android.content.Context
import android.util.Log
import com.example.storyforge.save.GameSnapshot
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GameStateSlotStorage {
    private const val DIR_NAME = "saves"

    private fun saveDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { mkdirs() }

    fun listSaves(context: Context): List<File> =
        saveDir(context).listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun saveSlot(context: Context, state: GameState, promptCardName: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
        val safeName = promptCardName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val filename = "$safeName-$timestamp.json"
        val file = File(saveDir(context), filename)
        file.writeText(Json.encodeToString(state))
        return file
    }

    fun loadSlot(file: File): GameState? {
        return try {
            val json = file.readText()
            Json.decodeFromString(json)
        } catch (_: Exception) {
            null
        }
    }

    fun loadSnapshot(file: File): GameSnapshot? {
        return try {
            val text = file.readText()
            Json.decodeFromString<GameSnapshot>(text)
        } catch (e: Exception) {
            Log.e("Save", "Failed to load snapshot from ${file.name}", e)
            null
        }
    }

}
