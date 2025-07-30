// file: com/example/storyforge/core/NarrationParser.kt
package com.example.storyforge.core

import com.example.storyforge.model.DeltaInstruction
import com.example.storyforge.model.DigestLine
import kotlinx.serialization.json.*
import java.lang.Exception
import java.time.Instant

object NarrationParser {

    data class ParsedNarration(
        val prose: String,
        val emitDelta: Map<String, DeltaInstruction>,
        val digestLines: List<DigestLine>,
        val sceneBlock: JsonObject? // ← NEW
    )

    fun extractJsonAndCleanNarration(raw: String): ParsedNarration {
        val deltaMarker = "@delta"
        val digestMarker = "@digest"
        val sceneMarker = "@scene"

        val lines = raw.lines()

        val deltaIndex = lines.indexOfFirst { it.trim() == deltaMarker }
        val digestIndex = lines.indexOfFirst { it.trim() == digestMarker }
        val sceneIndex = lines.indexOfFirst { it.trim() == sceneMarker }

        val proseEnd = listOfNotNull(
            if (deltaIndex >= 0) deltaIndex else null,
            if (digestIndex >= 0) digestIndex else null,
            if (sceneIndex >= 0) sceneIndex else null
        ).minOrNull() ?: lines.size

        val prose = lines.subList(0, proseEnd).joinToString("\n").trim()

        val deltaJson = if (deltaIndex != -1 && deltaIndex + 1 < lines.size) {
            extractJsonObject(lines.subList(deltaIndex + 1, digestIndex.takeIf { it > deltaIndex }
                ?: sceneIndex.takeIf { it > deltaIndex } ?: lines.size))
        } else JsonObject(emptyMap())

        val digestJson = if (digestIndex != -1 && digestIndex + 1 < lines.size) {
            extractJsonArray(lines.subList(digestIndex + 1, sceneIndex.takeIf { it > digestIndex }
                ?: lines.size))
        } else JsonArray(emptyList())

        val sceneJson = if (sceneIndex != -1 && sceneIndex + 1 < lines.size) {
            extractJsonObject(lines.subList(sceneIndex + 1, lines.size))
        } else null

        return ParsedNarration(
            prose = prose,
            emitDelta = parseDelta(deltaJson),
            digestLines = parseDigestLines(digestJson),
            sceneBlock = sceneJson?.let { normalizeSceneBlock(it) }
        )
    }

    private fun extractJsonObject(lines: List<String>): JsonObject {
        val text = lines.joinToString("\n").trim()
        return try {
            Json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }
    }

    private fun extractJsonArray(lines: List<String>): JsonArray {
        val text = lines.joinToString("\n").trim()
        return try {
            Json.parseToJsonElement(text).jsonArray
        } catch (e: Exception) {
            JsonArray(emptyList())
        }
    }

    private fun parseDelta(json: JsonObject): Map<String, DeltaInstruction> {
        return json.mapNotNull { (key, value) ->
            DeltaInstruction.fromJsonElement(key, value)?.let { key to it }
        }.toMap()
    }

    private fun parseDigestLines(json: JsonArray): List<DigestLine> {
        return json.mapIndexedNotNull { index, element ->
            try {
                val obj = element.jsonObject
                val text = obj["text"]?.jsonPrimitive?.contentOrNull ?: return@mapIndexedNotNull null
                val score = obj["importance"]?.jsonPrimitive?.intOrNull ?: 3
                val tags = extractTags(text)
                DigestLine(turn = index, tags = tags, score = score, text = text)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun extractTags(text: String): List<String> {
        val tagPattern = Regex("[#@\\$][a-zA-Z0-9_]+")
        return tagPattern.findAll(text).map { it.value }.toSet().toList()
    }

    fun buildLogEntry(
        turnNumber: Int?,
        deltas: Map<String, DeltaInstruction>
    ): String {
        val meta = buildJsonObject {
            put("timestamp", JsonPrimitive(Instant.now().toString()))
            turnNumber?.let { put("turn", JsonPrimitive(it)) }
        }
        val deltaPayload = deltas.mapValues { it.value.toLogJsonElement() }
        val entry = JsonObject(
            mapOf(
                "meta" to meta,
                "delta" to JsonObject(deltaPayload)
            )
        )
        return Json.encodeToString(entry)
    }

    private fun normalizeSceneBlock(raw: JsonObject): JsonObject {
        val location = raw["location"]?.jsonPrimitive?.contentOrNull
        val presentArray = raw["present"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        val filteredPresent = presentArray.filter { it.startsWith("#") || it.startsWith("@") }
        return buildJsonObject {
            location?.let { put("location", JsonPrimitive(it)) }
            put("present", JsonArray(filteredPresent.map { JsonPrimitive(it) }))
        }
    }
}
