package com.example.storyforge.model

import android.content.Context
import com.example.storyforge.logging.TurnLogEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import com.example.storyforge.save.readJsonLogLines

/**
 * TurnLog persistently stores each turn's action and narration in a JSONL file.
 * Useful for replay, auditing, or debugging.
 */
object TurnLog {
    private const val LOG_FILENAME = "turn_log.jsonl"

    @Serializable
    data class TurnEntry(
        val turn: Int,
        val timestamp: String = Instant.now().toString(),
        val action: String,
        val narration: String
    )

    fun append(context: Context, turn: Int, action: String, narration: String) {
        try {
            val entry = TurnEntry(turn = turn, action = action, narration = narration)
            val file = File(context.filesDir, LOG_FILENAME)
            file.appendText(Json.encodeToString(entry) + "\n")
        } catch (e: Exception) {
            e.printStackTrace() // Optionally log using Log.e
        }
    }

    fun readAll(context: Context): List<TurnLogEntry> {
        return readJsonLogLines(context, LOG_FILENAME)
    }

}
