// file: com/example/storyforge/stack/StackAssemblerInput.kt
package com.example.storyforge.stack

import com.example.storyforge.model.ConversationTurn
import com.example.storyforge.model.DigestLine
import kotlinx.serialization.json.JsonObject

/**
 * Inputs required to assemble the narrator stack.
 */
data class StackAssemblerInput(
    val turnNumber: Int,
    val userMessage: String,
    val stackInstructions: StackInstructions,
    val aiPrompt: String,
    val emitSkeleton: String,
    val gameRules: String,
    val firstTurnOnlyBlock: String,

    val turnHistory: List<ConversationTurn>,
    val digestLines: List<DigestLine>,
    val expressionLog: Map<String, List<String>>,
    val worldState: JsonObject,
    val sceneTags: Set<String>
)
