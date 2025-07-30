package com.example.storyforge.core

import android.content.Context
import android.util.Log
import com.example.storyforge.model.DeltaInstruction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.time.Instant
import com.example.storyforge.save.SceneState


/**
 * SceneManager tracks the current scene location and present characters/entities.
 * It is updated via @scene block or fallback heuristics from deltas.
 * Scene state is also logged with timestamp and turn number.
 */
object SceneManager {

    private const val LOG_FILENAME = "scene_log.jsonl"
    private var sceneLocation: String? = null
    private var scenePresent: List<String> = emptyList()

    @Serializable
    data class SceneLogEntry(
        val turn: Int,
        val timestamp: String,
        val location: String?,
        val present: List<String>
    )

    fun onSceneBlock(scene: JsonObject, context: Context, turn: Int) {
        sceneLocation = scene["location"]?.jsonPrimitive?.contentOrNull
        scenePresent = scene["present"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: emptyList()
        logScene(context, turn)
    }

    fun onDeltas(deltas: Map<String, DeltaInstruction>, context: Context, turn: Int) {
        if (scenePresent.isNotEmpty() && sceneLocation != null) return

        val present = mutableListOf<String>()
        for ((key, instruction) in deltas) {
            if (instruction !is DeltaInstruction.Declare) continue

            if (key == "world.location") {
                sceneLocation = instruction.value.jsonPrimitive.contentOrNull
                continue
            }

            val parts = key.split(".")
            if (parts.size < 2) continue

            val category = parts[0]
            val entity = parts[1]

            val obj = instruction.value as? JsonObject ?: continue
            val tag = obj["tag"]?.jsonPrimitive?.contentOrNull ?: continue
            if (tag in listOf("character", "location")) {
                present += "$category.$entity"
            }
        }

        scenePresent = present.distinct()
        logScene(context, turn)
    }

    fun getSceneTags(worldState: JsonObject): List<String> {
        val tags = mutableSetOf<String>()

        for (path in scenePresent) {
            val parts = path.split(".")
            if (parts.size < 2) continue
            val category = parts[0]
            val entity = parts[1]
            val tag = worldState[category]
                ?.jsonObject
                ?.get(entity)
                ?.jsonObject
                ?.get("tag")
                ?.jsonPrimitive
                ?.contentOrNull

            if (tag != null && (tag.startsWith("#") || tag.startsWith("@"))) {
                tags += tag
            }
        }

        // Scene location is already stored as symbolic tag (e.g. "@tavern")
        sceneLocation?.let {
            if (it.startsWith("@")) tags += it
        }

        return tags.toList()
    }


    fun reset() {
        sceneLocation = null
        scenePresent = emptyList()
    }

    fun debugState(): String {
        return "SceneManager(location=$sceneLocation, present=$scenePresent)"
    }

    fun getSnapshot(): SceneState {
        return SceneState(
            location = sceneLocation,
            present = scenePresent
        )
    }

    fun setFromSnapshot(state: SceneState) {
        sceneLocation = state.location
        scenePresent = state.present
    }


    fun restoreFromSnapshot(snapshot: SceneState) {
        sceneLocation = snapshot.location
        scenePresent = snapshot.present
        Log.d("SceneManager", "Restored scene: location=$sceneLocation present=$scenePresent")
    }



    private fun logScene(context: Context, turn: Int) {
        try {
            val entry = SceneLogEntry(
                turn = turn,
                timestamp = Instant.now().toString(),
                location = sceneLocation,
                present = scenePresent
            )
            val file = File(context.filesDir, LOG_FILENAME)
            file.appendText(Json.encodeToString(entry) + "\n")
        } catch (e: Exception) {
            Log.e("SceneManager", "Failed to log scene state", e)
        }
    }
}
