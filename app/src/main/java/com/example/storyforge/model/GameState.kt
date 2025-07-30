package com.example.storyforge.model

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

private const val TAG = "GameState"

@Serializable
data class GameState(
    var hp: Int = 100,
    var gold: Int = 50,
    var narration: String = "Welcome!",
    var worldState: JsonObject = JsonObject(mapOf())
) {
    fun applyDeltas(deltas: Map<String, DeltaInstruction>) {
        Log.d(TAG, "Applying structured deltas...")

        val updatedWorld = worldState.toMutableMap()

        deltas.forEach { (fullKey, instruction) ->
            val parts = fullKey.split(".", limit = 2)
            if (parts.size < 2) return@forEach
            val target = parts[0]
            val variable = parts[1]

            if (target == "player") {
                applyToCoreFields(variable, instruction)
            } else {
                val current = updatedWorld[target]?.jsonObject?.toMutableMap() ?: mutableMapOf()
                when (instruction) {
                    is DeltaInstruction.Add -> {
                        val prev = current[variable]?.jsonPrimitive?.intOrNull ?: 0
                        val addValue = instruction.value.jsonPrimitive.intOrNull ?: 0
                        current[variable] = JsonPrimitive(prev + addValue)
                    }
                    is DeltaInstruction.Assign -> {
                        current[variable] = instruction.value
                    }
                    is DeltaInstruction.Declare -> {
                        if (!current.containsKey(variable)) {
                            current[variable] = instruction.value
                        }
                    }
                    is DeltaInstruction.Delete -> {
                        current.remove(variable)
                    }
                }
                updatedWorld[target] = JsonObject(current)
                Log.d(TAG, "Updated $target.$variable with ${instruction.toLogValue()}")
            }
        }

        worldState = JsonObject(updatedWorld)
        val flattened = flattenJsonObject(worldState)
        val worldSummary = if (flattened.isEmpty()) "none" else flattened.keys.joinToString(", ")
        Log.d(TAG, "New state - Narration: '$narration', World keys: $worldSummary")
    }

    private fun applyToCoreFields(variable: String, instruction: DeltaInstruction) {
        val value = when (instruction) {
            is DeltaInstruction.Add -> instruction.value
            is DeltaInstruction.Assign -> instruction.value
            is DeltaInstruction.Declare -> instruction.value
            is DeltaInstruction.Delete -> null
        }

        when (instruction) {
            is DeltaInstruction.Add -> {
                when (variable) {
                    "hp" -> hp += value?.jsonPrimitive?.intOrNull ?: 0
                    "gold" -> gold += value?.jsonPrimitive?.intOrNull ?: 0
                }
            }
            is DeltaInstruction.Assign -> {
                when (variable) {
                    "hp" -> hp = value?.jsonPrimitive?.intOrNull ?: hp
                    "gold" -> gold = value?.jsonPrimitive?.intOrNull ?: gold
                    "narration" -> narration = value?.jsonPrimitive?.content ?: narration
                }
            }
            is DeltaInstruction.Declare -> {
                applyToCoreFields(variable, DeltaInstruction.Assign(variable, instruction.value))
            }
            is DeltaInstruction.Delete -> {
                when (variable) {
                    "hp" -> hp = 0
                    "gold" -> gold = 0
                    "narration" -> narration = ""
                }
            }
        }

        Log.d(TAG, "Applied to core field player.$variable -> ${instruction.toLogValue()}")
    }
}

private fun flattenJsonObject(obj: JsonObject, prefix: String = ""): Map<String, JsonElement> {
    val result = mutableMapOf<String, JsonElement>()
    for ((key, value) in obj) {
        val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
        if (value is JsonObject) {
            result.putAll(flattenJsonObject(value, fullKey))
        } else {
            result[fullKey] = value
        }
    }
    return result
}
