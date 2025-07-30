package com.example.storyforge.logging

import android.content.Context
import com.example.storyforge.model.Message
import com.example.storyforge.model.StackLogEntry
import com.example.storyforge.model.TokenSummary
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

object NarratorStackLog {
    private const val LOG_FILENAME = "stack_trace_log.jsonl"

    fun append(
        context: Context,
        turn: Int,
        model: String,
        messages: List<Message>,
        latencyMs: Long,
        inputTokens: Int,
        outputTokens: Int
    ) {
        val entry = StackLogEntry(
            turn = turn,
            timestamp = Instant.now().toString(),
            model = model,
            stack = messages,
            latency_ms = latencyMs,
            token_summary = TokenSummary(
                input = inputTokens,
                output = outputTokens,
                total = inputTokens + outputTokens
            )
        )

        val file = File(context.filesDir, LOG_FILENAME)
        file.appendText(Json.encodeToString(entry) + "\n")
    }
}
