// file: com/example/storyforge/logging/NarrationLogger.kt
package com.example.storyforge.logging

import com.example.storyforge.model.DigestLine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object NarrationLogger {

    private val sessionId: String = generateSessionId()
    private val logDir = File("logs").also { it.mkdirs() }

    private fun generateSessionId(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        return "s" + now.format(formatter)
    }

    /**
     * Save a parsed digest line with extracted tags and turn index.
     */
    fun logDigestLine(line: DigestLine, turn: Int) {
        val entry = mapOf(
            "turn" to turn,
            "score" to line.score,
            "tags" to extractTags(line.text),
            "text" to line.text
        )
        val jsonLine = Json.encodeToString(entry)
        File(logDir, "${sessionId}_parsed_digest.jsonl").appendText(jsonLine + "\n")
    }

    /**
     * Extract symbolic tags like #fox or @deepwood from a digest line.
     */
    private fun extractTags(text: String): List<String> {
        val tagPattern = Regex("[#@\\$][a-zA-Z0-9_]+")
        return tagPattern.findAll(text).map { it.value }.toSet().toList()
    }
}
