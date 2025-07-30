package com.example.storyforge.save

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class DeltaLogEntry(
    val turn: Int,
    val timestamp: String,
    val deltas: Map<String, JsonElement> // Already flattened via `toLogJsonElement()`
)
