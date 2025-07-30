package com.example.storyforge.logging

import com.example.storyforge.model.DeltaInstruction
import com.example.storyforge.model.DigestLine
import com.example.storyforge.model.TokenSummary
import com.example.storyforge.prompt.AiSettings

/**
 * Canonical structured log for a single turn.
 */
@kotlinx.serialization.Serializable
data class TurnLogEntry(
    val turnNumber: Int,                     // Turn index in session
    val timestamp: String,                   // ISO timestamp of creation

    val userInput: String,                   // Raw user message sent
    val narratorOutput: String,              // Raw narrator reply received

    val digest: DigestLine? = null,          // First digest line (optional)
    val deltas: Map<String, DeltaInstruction>? = null,  // Structured world state changes

    val contextSnapshot: String? = null,     // Stack content sent to narrator
    val tokenUsage: TokenSummary? = null,    // Token usage stats (input/output/total)
    val apiRequestBody: String? = null,
    val apiResponseBody: String? = null,
    val apiUrl: String? = null,
    val latencyMs: Long? = null,


    val aiSettings: AiSettings? = null,      // AI model config at time of generation
    val errorFlags: List<LogErrorFlag> = emptyList(), // Validation issues (if any)
    val modelSlugUsed: String = ""

)
