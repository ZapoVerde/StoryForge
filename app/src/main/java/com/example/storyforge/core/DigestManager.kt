package com.example.storyforge.core

import android.content.Context
import android.util.Log
import com.example.storyforge.model.DeltaInstruction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import com.example.storyforge.model.DigestLine


/**
 * DigestManager maintains a persistent and memory-resident list of digest summaries
 * extracted from narrator output. It supports selective retention and is extensible
 * for future tagging and disposal policies.
 */
object DigestManager {

    private const val LOG_FILENAME = "digest_log.jsonl"
    private const val MAX_MEMORY_LINES = 30


    /** Toggle to control whether we persist logs to disk */
    var enableLogging: Boolean = true

    private val digestLines = mutableListOf<DigestLine>()



    /**
     * Add a new digest line to memory and optionally log to file.
     */
    fun addLine(context: Context, turn: Int, score: Int, text: String) {
        if (score !in 1..5 || text.isBlank()) return

        val entry = DigestLine(
            turn = turn,
            tags = extractTags(text),
            score = score,
            text = text.trim()
        )

        digestLines += entry

        if (enableLogging) {
            try {
                val file = File(context.filesDir, LOG_FILENAME)
                file.appendText(Json.encodeToString(entry) + "\n")
            } catch (e: Exception) {
                Log.e("DigestManager", "Failed to log digest line", e)
            }
        }

        pruneOldLines()
    }

    /**
     * Returns the top memory block for prompt generation, sorted by turn.
     */
    fun getContextBlock(maxLines: Int = 12): List<String> {
        return digestLines
            .sortedWith(compareByDescending<DigestLine> { it.score }.thenBy { it.turn })
            .take(maxLines)
            .sortedBy { it.turn }
            .map { it.text }
    }

    /**
     * Limit in-memory digest to top-N important items.
     */
    fun pruneOldLines(maxStored: Int = MAX_MEMORY_LINES) {
        if (digestLines.size <= maxStored) return
        digestLines.sortWith(compareByDescending<DigestLine> { it.score }.thenByDescending { it.turn })
        while (digestLines.size > maxStored) {
            digestLines.removeAt(digestLines.lastIndex)

        }
    }

    fun getAllLines(): List<DigestLine> {
        return digestLines.toList()
    }

    private fun extractTags(text: String): List<String> {
        val tagPattern = Regex("[#@\\$][a-zA-Z0-9_]+")
        return tagPattern.findAll(text).map { it.value }.toSet().toList()
    }

    fun recordDigestLine(line: DigestLine) {
        digestLines += line
    }



    fun clearMemory() {
        digestLines.clear()
    }

    fun deleteLogFile(context: Context) {
        val file = File(context.filesDir, LOG_FILENAME)
        if (file.exists()) file.delete()
    }

    fun loadFromLog(context: Context) {
        val file = File(context.filesDir, LOG_FILENAME)
        if (!file.exists()) return

        try {
            file.readLines()
                .mapNotNull { line ->
                    try {
                        Json.decodeFromString<DigestLine>(line)
                    } catch (_: Exception) {
                        null
                    }
                }.let {
                    digestLines.clear()
                    digestLines.addAll(it)
                }
        } catch (e: Exception) {
            Log.e("DigestManager", "Failed to load digest log", e)
        }
    }

    fun addParsedLines(context: Context, turn: Int, prose: String, deltas: Map<String, DeltaInstruction>) {
        if (deltas.isEmpty()) return

        for ((key, instruction) in deltas) {
            val score = when {
                key.startsWith("=player.") || key.startsWith("=world.") -> 5
                key.contains(".flags.") -> 4
                key.contains(".status") -> 3
                key.startsWith("+") || key.startsWith("!") -> 2
                else -> 1
            }

            val tag = (instruction as? DeltaInstruction.Declare)?.value
                ?.let { it as? JsonObject }
                ?.get("tag")?.jsonPrimitive?.contentOrNull

            val keyParts = key.split(".")
            val taggableKey = if (tag in listOf("character", "location") && keyParts.size >= 2) {
                "@${keyParts[1].lowercase()}"
            } else null

            val summary = when (instruction) {
                is DeltaInstruction.Assign -> "Set $key = ${instruction.value}"
                is DeltaInstruction.Add -> "Added to $key: ${instruction.value}"
                is DeltaInstruction.Declare -> "Declared ${taggableKey ?: key} as ${instruction.value}"
                is DeltaInstruction.Delete -> "Removed $key"
            }

            addLine(context, turn, score, summary)
        }

        // Optional prose line extraction
        prose.trim().split(Regex("[.!?]")).firstOrNull()?.let { firstLine ->
            if (firstLine.length > 10) {
                addLine(context, turn, score = 3, text = firstLine.trim())
            }
        }
    }

    fun setFromSnapshot(lines: List<DigestLine>) {
        digestLines.clear()
        digestLines.addAll(lines)
    }


    fun exportAll(): List<DigestLine> {
        return digestLines.toList()
    }

    fun restoreFromSnapshot(lines: List<DigestLine>) {
        digestLines.clear()
        digestLines.addAll(lines)
        Log.d("DigestManager", "Restored ${lines.size} digest lines from snapshot")
    }


}
