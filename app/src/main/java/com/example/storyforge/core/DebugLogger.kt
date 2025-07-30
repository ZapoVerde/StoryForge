package com.example.storyforge.core

import android.content.Context
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object DebugLogger {

    private fun getTimestamp(): String {
        return "# Logged at: ${ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}"
    }

    fun writeText(context: Context, filename: String, content: String) {
        try {
            val dir = File(context.filesDir, "debug")
            dir.mkdirs()
            val file = File(dir, filename)
            file.writeText("${getTimestamp()}\n$content")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun writeTokens(context: Context, turnId: Int, model: String, inputTokens: Int, outputTokens: Int) {
        val text = buildString {
            appendLine("Turn: $turnId")
            appendLine("Model: $model")
            appendLine("Input tokens: $inputTokens")
            appendLine("Output tokens: $outputTokens")
            appendLine("Total: ${inputTokens + outputTokens}")
        }
        writeText(context, "turn_%04d_tokens.txt".format(turnId), text)
    }

    fun writeLatency(context: Context, turnId: Int, millis: Long) {
        writeText(context, "turn_%04d_latency.txt".format(turnId), "$millis ms")
    }
}
