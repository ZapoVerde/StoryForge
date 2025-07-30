package com.example.storyforge.logging

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun renderLogEntry(viewMode: LogViewMode, entry: TurnLogEntry) {
    when (viewMode) {
        LogViewMode.RAW      -> renderRaw(entry)
        LogViewMode.DIGEST   -> renderDigest(entry)
        LogViewMode.DELTAS   -> renderDeltas(entry)
        LogViewMode.CONTEXT  -> renderContext(entry)
        LogViewMode.TOKENS   -> renderTokens(entry)
        LogViewMode.SETTINGS -> renderSettings(entry)
        LogViewMode.ERRORS   -> renderErrorFlags(entry)
        LogViewMode.API      -> renderApiExchange(entry)
    }
}

@Composable
fun LogHeader(entry: TurnLogEntry) {
    Text(
        text = "Turn ${entry.turnNumber} – ${entry.timestamp}",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun renderRaw(entry: TurnLogEntry) {
    Column(Modifier.padding(8.dp)) {
        LogHeader(entry)
        Text("User:", style = MaterialTheme.typography.labelSmall)
        Text(entry.userInput, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text("Narrator:", style = MaterialTheme.typography.labelSmall)
        Text(entry.narratorOutput, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun renderDigest(entry: TurnLogEntry) {
    val digest = entry.digest
    Column(Modifier.padding(8.dp)) {
        LogHeader(entry)
        if (digest != null) {
            Text("Importance: ${digest.score}", style = MaterialTheme.typography.labelSmall)
            Text("Tags: ${digest.tags.joinToString()}", style = MaterialTheme.typography.labelSmall)
            Text(digest.text, style = MaterialTheme.typography.bodyMedium)
        } else {
            Text("(No digest line)", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun renderDeltas(entry: TurnLogEntry) {
    val deltas = entry.deltas
    Column(Modifier.padding(8.dp)) {
        LogHeader(entry)
        if (deltas.isNullOrEmpty()) {
            Text("(No deltas)", style = MaterialTheme.typography.bodySmall)
        } else {
            for ((key, op) in deltas) {
                Text("$key → ${op.toLogValue()}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun renderContext(entry: TurnLogEntry) {
    Column(Modifier.padding(8.dp)) {
        LogHeader(entry)
        if (entry.contextSnapshot.isNullOrBlank()) {
            Text("(No context snapshot)", style = MaterialTheme.typography.bodySmall)
        } else {
            Text(entry.contextSnapshot, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun renderTokens(entry: TurnLogEntry) {
    val tokens = entry.tokenUsage
    Column(Modifier.padding(8.dp)) {
        LogHeader(entry)
        if (tokens == null) {
            Text("(No token data)", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Input: ${tokens.input}", style = MaterialTheme.typography.bodySmall)
            Text("Output: ${tokens.output}", style = MaterialTheme.typography.bodySmall)
            Text("Total: ${tokens.total}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun renderSettings(entry: TurnLogEntry) {
    val settings = entry.aiSettings
    Column(Modifier.padding(8.dp)) {
        LogHeader(entry)
        if (settings == null) {
            Text("(No AI settings)", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Temperature: ${settings.temperature}", style = MaterialTheme.typography.bodySmall)
            Text("Top P: ${settings.topP}", style = MaterialTheme.typography.bodySmall)
            Text("Max Tokens: ${settings.maxTokens}", style = MaterialTheme.typography.bodySmall)
            Text("Presence Penalty: ${settings.presencePenalty}", style = MaterialTheme.typography.bodySmall)
            Text("Frequency Penalty: ${settings.frequencyPenalty}", style = MaterialTheme.typography.bodySmall)
            Text("Function Calling: ${settings.functionCallingEnabled}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun renderErrorFlags(entry: TurnLogEntry) {
    val errors = entry.errorFlags
    Column(Modifier.padding(8.dp)) {
        LogHeader(entry)
        if (errors.isEmpty()) {
            Text("✔ No errors", style = MaterialTheme.typography.bodySmall)
        } else {
            for (flag in errors) {
                Text("⚠ $flag", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun renderApiExchange(entry: TurnLogEntry) {
    Column(Modifier.padding(8.dp)) {
        LogHeader(entry)

        Text("API Endpoint:", style = MaterialTheme.typography.labelSmall)
        Text(entry.apiUrl ?: "(unknown)", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(8.dp))
        Text("Latency: ${entry.latencyMs ?: "?"} ms", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(8.dp))
        Text("Raw Request JSON:", style = MaterialTheme.typography.labelSmall)
        Text(entry.apiRequestBody ?: "(not captured)", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(8.dp))
        Text("Raw Response JSON:", style = MaterialTheme.typography.labelSmall)
        Text(entry.apiResponseBody ?: "(not captured)", style = MaterialTheme.typography.bodySmall)
    }
}


