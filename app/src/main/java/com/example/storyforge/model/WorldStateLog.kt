package com.example.storyforge.model

import android.content.Context
import android.util.Log
import com.example.storyforge.save.DeltaLogEntry
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer
import java.io.File
import java.time.Instant
import com.example.storyforge.save.readJsonLogLines

object WorldStateLog {

    private const val LOG_FILENAME = "worldstate_log.jsonl"

    fun readAll(context: Context): List<DeltaLogEntry> {
        return readJsonLogLines(context, LOG_FILENAME)
    }

    fun setAll(all: List<DeltaLogEntry>) {
        // no-op: deltas are log-only, not memory restored
    }

    fun append(context: Context, turnNumber: Int?, deltas: Map<String, DeltaInstruction>) {
        try {
            val logEntry = buildLogEntry(turnNumber, deltas)
            val file = File(context.filesDir, LOG_FILENAME)
            file.appendText(logEntry + "\n")
        } catch (e: Exception) {
            Log.e("WorldStateLog", "Failed to append to log", e)
        }
    }

    private fun buildLogEntry(turnNumber: Int?, deltas: Map<String, DeltaInstruction>): String {
        val meta = buildJsonObject {
            put("timestamp", JsonPrimitive(Instant.now().toString()))
            turnNumber?.let { put("turn", JsonPrimitive(it)) }
        }

        val deltaPayload = buildJsonObject {
            for ((key, instruction) in deltas) {
                val jsonValue: JsonElement = when (val v = instruction.toLogValue()) {
                    is Map<*, *> -> {
                        val map = v.entries
                            .mapNotNull { (k, valElem) ->
                                if (k is String && valElem is JsonElement) {
                                    k to valElem
                                } else null
                            }
                            .toMap()
                        JsonObject(map)
                    }
                    is String -> JsonPrimitive(v)
                    is Char -> JsonPrimitive(v.toString())
                    else -> JsonPrimitive(v.toString())
                }


                put(key, jsonValue)

            }
        }

        val entry = buildJsonObject {
            put("meta", meta)
            put("delta", deltaPayload)
        }

        return Json.encodeToString(entry)
    }




}
