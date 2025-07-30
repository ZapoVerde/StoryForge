package com.example.storyforge.logging

import com.example.storyforge.model.DeltaInstruction
import com.example.storyforge.model.DigestLine
import com.example.storyforge.model.TokenSummary
import com.example.storyforge.prompt.AiSettings
import java.time.Instant

object TurnLogAssembler {

    /**
     * Build a structured TurnLogEntry from available turn data.
     * Any missing components will be stubbed or set to null.
     */
    fun assemble(
        turnNumber: Int,
        userInput: String,
        rawNarratorOutput: String,
        parsedDigest: List<DigestLine>?,
        parsedDeltas: Map<String, DeltaInstruction>?,
        contextSnapshot: String? = null,
        tokenUsage: TokenSummary? = null,
        aiSettings: AiSettings? = null,
        errorFlags: List<LogErrorFlag> = emptyList(),
        apiRequestBody: String = "",
        apiResponseBody: String = "",
        apiUrl: String = "",
        latencyMs: Long = 0,
        modelSlugUsed: String = ""        // <-- ADD THIS
    ): TurnLogEntry {
        val digestLine = parsedDigest?.firstOrNull()

        return TurnLogEntry(
            turnNumber = turnNumber,
            timestamp = Instant.now().toString(),
            userInput = userInput,
            narratorOutput = rawNarratorOutput,
            digest = digestLine,
            deltas = parsedDeltas,
            contextSnapshot = contextSnapshot,
            tokenUsage = tokenUsage,
            aiSettings = aiSettings,
            errorFlags = errorFlags,
            apiRequestBody = apiRequestBody,
            apiResponseBody = apiResponseBody,
            apiUrl = apiUrl,
            latencyMs = latencyMs,
            modelSlugUsed = modelSlugUsed    // <-- AND PASS IT INTO THE ENTRY
        )
    }


}
