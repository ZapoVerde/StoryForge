// file: com/example/storyforge/core/DummyNarrator.kt

package com.example.storyforge.core

import android.content.Context
import com.example.storyforge.model.DeltaInstruction
import com.example.storyforge.model.Message
import com.example.storyforge.prompt.AiSettings
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import kotlin.Result

class DummyNarrator : Narrator {

    override suspend fun generate(
        messages: List<Message>,
        settings: AiSettings,
        modelName: String,
        turnId: Int,
        context: Context
    ): Result<Pair<String, Map<String, DeltaInstruction>>> {
        delay(300)

        val lastAction = messages.lastOrNull { it.role == "user" }?.content?.trim()?.lowercase()

        val (prose, delta) = when (lastAction) {
            "1" -> Pair(
                "You sift through the rubble of the old ruins. Dust clings to your fingers, but you find a stash of scrap.",
                mapOf(
                    "!enemies.goblin_1" to DeltaInstruction.Declare("enemies.goblin_1", JsonObject(mapOf(
                        "hp" to JsonPrimitive(6),
                        "status" to JsonPrimitive("hostile")
                    ))),
                    "!enemies.goblin_2" to DeltaInstruction.Declare("enemies.goblin_2", JsonObject(mapOf(
                        "hp" to JsonPrimitive(4),
                        "status" to JsonPrimitive("fleeing")
                    ))),
                    "!enemies.goblin_3" to DeltaInstruction.Declare("enemies.goblin_3", JsonObject(mapOf(
                        "hp" to JsonPrimitive(8),
                        "status" to JsonPrimitive("angry")
                    )))
                )
            )

            "2" -> Pair(
                "You press deeper into the forest. Twisting roots slow your path, but signs of an old camp emerge.",
                mapOf(
                    "=world.location" to DeltaInstruction.Assign("world.location", JsonPrimitive("deep_forest")),
                    "!world.flags.enteredForest" to DeltaInstruction.Declare("world.flags.enteredForest", JsonPrimitive(true)),
                    "+inventory.torches" to DeltaInstruction.Add("inventory.torches", JsonPrimitive(1))
                )
            )

            "3" -> Pair(
                "You make a simple camp and rest. The fire crackles as you settle in for the night.",
                mapOf(
                    "!player.status.rested" to DeltaInstruction.Declare("player.status.rested", JsonPrimitive(true)),
                    "!world.flags.restedHere" to DeltaInstruction.Declare("world.flags.restedHere", JsonPrimitive(true))
                )
            )

            "4" -> Pair(
                "You wander without clear direction. The terrain shifts around you, unfamiliar and vast.",
                mapOf(
                    "=world.location" to DeltaInstruction.Assign("world.location", JsonPrimitive("unknown")),
                    "!world.flags.exploring" to DeltaInstruction.Declare("world.flags.exploring", JsonPrimitive(true))
                )
            )

            else -> Pair(
                "You hesitate, unsure what to do next.",
                mapOf(
                    "!world.flags.idle" to DeltaInstruction.Declare("world.flags.idle", JsonPrimitive(true))
                )
            )
        }

        return Result.success(prose to delta)
    }

    override suspend fun generateFull(
        messages: List<Message>,
        settings: AiSettings,
        modelName: String,
        turnId: Int,
        context: Context
    ): Result<NarratorResult> {
        return generate(messages, settings, modelName, turnId, context).map { (prose, deltas) ->
            NarratorResult(
                narration = prose,
                deltas = deltas,
                apiRequestBody = "(dummy)",
                apiResponseBody = "(dummy)",
                apiUrl = "dummy://narrator.local",
                latencyMs = 0L,
                modelSlugUsed = modelName  // fallback to name, since Dummy doesn't use slugs
            )
        }

    }
}
