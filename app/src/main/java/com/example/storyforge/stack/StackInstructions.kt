// file: com/example/storyforge/stack/StackInstructions.kt
package com.example.storyforge.stack

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Parses and validates the narrator stack configuration embedded in a PromptCard.
 * Allows for rich customization of how memory and world state are shaped into the narrator prompt.
 */
object StackInstructionsLoader {
    fun fromJson(raw: String): StackInstructions {
        val clean = sanitizeJson(raw)
        return try {
            Json.decodeFromString(StackInstructions.serializer(), clean)
        } catch (e: Exception) {
            e.printStackTrace()
            StackInstructions() // fallback to defaults
        }
    }

    private fun sanitizeJson(raw: String): String {
        return raw.lineSequence()
            .map { it.trim() }
            .filterNot { it.startsWith("//") || it.startsWith("#") }
            .joinToString("\n")
    }
}

@Serializable
data class StackInstructions(
    val narratorProseEmission: ProsePolicy = ProsePolicy(),
    val digestPolicy: DigestFilterPolicy = DigestFilterPolicy(),
    val digestEmission: Map<Int, EmissionRule> = defaultDigestEmission(),

    val expressionLogPolicy: ProsePolicy = ProsePolicy(mode = StackMode.ALWAYS),
    val expressionLinesPerCharacter: Int = 3,
    val emotionWeighting: Boolean = true,

    val worldStatePolicy: ProsePolicy = ProsePolicy(mode = StackMode.FILTERED),
    val knownEntitiesPolicy: ProsePolicy = ProsePolicy(mode = StackMode.FIRST_N, n = 2, filtering = FilterMode.TAGGED),

    val outputFormat: String = "prose_digest_emit",

    val tokenPolicy: TokenPolicy = TokenPolicy()
)

@Serializable
data class ProsePolicy(
    val mode: StackMode = StackMode.FIRST_N,
    val n: Int = 3,
    val filtering: FilterMode = FilterMode.NONE
)

/**
 * Controls how summaries are selected from the digest buffer based on score.
 * Example: score 5 = always, score 4 = after turn 1, score 3 = only first 6, etc.
 */
@Serializable
data class EmissionRule(
    val mode: StackMode = StackMode.NEVER,
    val n: Int = 0
)

@Serializable
data class DigestFilterPolicy(
    val filtering: FilterMode = FilterMode.TAGGED
)

@Serializable
data class TokenPolicy(
    val minTokens: Int = 1000,
    val maxTokens: Int = 4096,
    val fallbackPlan: List<String> = listOf(
        "drop_known_entities",
        "drop_low_importance_digest",
        "truncate_expression_logs"
    )
)

/**
 * How many of each stack type to include and when.
 */
@Serializable
enum class StackMode {
    @SerialName("always") ALWAYS,
    @SerialName("firstN") FIRST_N,
    @SerialName("afterN") AFTER_N,
    @SerialName("never") NEVER,
    @SerialName("filtered") FILTERED
}

/**
 * Filtering strategies applied to each emission domain.
 */
@Serializable
enum class FilterMode {
    @SerialName("none") NONE,
    @SerialName("sceneOnly") SCENE_ONLY,
    @SerialName("tagged") TAGGED
}

// Helper: default digest emission levels 1–5
private fun defaultDigestEmission(): Map<Int, EmissionRule> = mapOf(
    5 to EmissionRule(StackMode.ALWAYS),
    4 to EmissionRule(StackMode.AFTER_N, n = 1),
    3 to EmissionRule(StackMode.FIRST_N, n = 6),
    2 to EmissionRule(StackMode.FIRST_N, n = 3),
    1 to EmissionRule(StackMode.NEVER)
)
