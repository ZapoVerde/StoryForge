// file: com/example/storyforge/stack/StackLayers.kt
package com.example.storyforge.stack

import com.example.storyforge.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

object StackLayers {

    fun buildNarratorProse(
        policy: ProsePolicy,
        history: List<ConversationTurn>,
        sceneTags: Set<String>
    ): List<Message> {
        val filtered = when (policy.filtering) {
            FilterMode.SCENE_ONLY -> history.filter { turn -> sceneTags.any { tag -> turn.narrator.contains(tag) } }
            FilterMode.TAGGED -> history
            FilterMode.NONE -> history
        }

        val clipped = when (policy.mode) {
            StackMode.ALWAYS -> filtered
            StackMode.FIRST_N -> filtered.take(policy.n)
            StackMode.AFTER_N -> filtered.drop(policy.n)
            StackMode.NEVER -> emptyList()
            else -> filtered
        }

        return clipped.flatMap { turn ->
            listOf(
                Message(role = "user", content = turn.user),
                Message(role = "assistant", content = turn.narrator)
            )
        }
    }

    fun buildDigestLines(
        instructions: StackInstructions,
        allLines: List<DigestLine>,
        turnNumber: Int
    ): List<Message> {
        val filtering = instructions.digestPolicy.filtering
        val lines = allLines.filter { line ->
            val emit = instructions.digestEmission[line.score] ?: EmissionRule(StackMode.NEVER)

            when (emit.mode) {
                StackMode.ALWAYS -> true
                StackMode.FIRST_N -> turnNumber < emit.n
                StackMode.AFTER_N -> turnNumber >= emit.n
                StackMode.NEVER -> false
                else -> false
            }

        }.filter { line ->
            when (filtering) {
                FilterMode.TAGGED -> line.tags.any { it.startsWith("#") || it.startsWith("@") }
                else -> true
            }
        }

        if (lines.isEmpty()) return emptyList()

        val summaryText = buildString {
            appendLine("Memory Summary:")
            for (line in lines) {
                appendLine("- [${line.score}] ${line.text}")
            }
        }

        return listOf(Message(role = "system", content = summaryText.trim()))
    }

    fun buildExpressionMemory(
        instructions: StackInstructions,
        emotionLog: Map<String, List<String>>
    ): List<Message> {
        val policy = instructions.expressionLogPolicy
        if (policy.mode == StackMode.NEVER || emotionLog.isEmpty()) return emptyList()

        val filtered = when (policy.filtering) {
            FilterMode.SCENE_ONLY -> emotionLog.filterKeys { it.startsWith("#") }
            else -> emotionLog
        }

        val trimmed = filtered.mapValues { (_, lines) ->
            lines.take(instructions.expressionLinesPerCharacter)
        }

        if (trimmed.isEmpty()) return emptyList()

        val block = buildString {
            appendLine("Character Emotions:")
            for ((char, lines) in trimmed) {
                appendLine("- $char:")
                lines.forEach { line -> appendLine("    • $line") }
            }
        }

        return listOf(Message(role = "system", content = block.trim()))
    }

    fun buildWorldStateBlock(
        policy: ProsePolicy,
        worldState: JsonObject,
        sceneTags: Set<String>
    ): List<Message> {
        val filtered = when (policy.filtering) {
            FilterMode.SCENE_ONLY -> {
                val relevant = worldState.filter { (cat, obj) ->
                    obj is JsonObject && obj.keys.any { key -> sceneTags.contains("$cat.$key") }
                }
                JsonObject(relevant)
            }
            else -> worldState
        }

        if (filtered.isEmpty()) return emptyList()

        val block = buildString {
            appendLine("World State:")
            appendLine(Json.encodeToString(JsonObject.serializer(), filtered))
        }

        return listOf(Message(role = "system", content = block.trim()))
    }

    fun buildKnownEntitiesBlock(
        policy: ProsePolicy,
        worldState: JsonObject,
        sceneTags: Set<String>
    ): List<Message> {
        val entityList = mutableListOf<String>()

        for ((cat, obj) in worldState) {
            if (obj !is JsonObject) continue
            for ((key, value) in obj) {
                val fullKey = "$cat.$key"
                val tag = value.jsonObject["tag"]?.jsonPrimitive?.contentOrNull ?: continue
                if (policy.filtering == FilterMode.SCENE_ONLY && fullKey !in sceneTags) continue
                entityList += "$tag → $fullKey"
            }
        }

        val clipped = when (policy.mode) {
            StackMode.FIRST_N -> entityList.take(policy.n)
            StackMode.AFTER_N -> entityList.drop(policy.n)
            StackMode.ALWAYS -> entityList
            else -> emptyList()
        }

        if (clipped.isEmpty()) return emptyList()

        val block = buildString {
            appendLine("Known Entities:")
            for (line in clipped) appendLine("- $line")
        }

        return listOf(Message(role = "system", content = block.trim()))
    }
}
