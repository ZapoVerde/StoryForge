// file: com/example/storyforge/stack/StackAssembler.kt
package com.example.storyforge.stack

import com.example.storyforge.model.Message

/**
 * StackAssembler orchestrates the construction of narrator input messages
 * based on policies defined in the PromptCard's stackInstructions.
 */
object StackAssembler {
    fun assemble(input: StackAssemblerInput): List<Message> {
        val messages = mutableListOf<Message>()

        // 1. First turn intro (if any)
        if (input.turnNumber == 0 && input.firstTurnOnlyBlock.isNotBlank()) {
            messages += Message(role = "system", content = input.firstTurnOnlyBlock.trim())
        }

        // 2. Static blocks
        messages += Message(role = "system", content = input.aiPrompt.trim())
        if (input.emitSkeleton.isNotBlank()) {
            messages += Message(role = "system", content = input.emitSkeleton.trim())
        }
        if (input.gameRules.isNotBlank()) {
            messages += Message(role = "system", content = input.gameRules.trim())
        }

        // 3. Dynamic layers
        messages += StackLayers.buildNarratorProse(input.stackInstructions.narratorProseEmission, input.turnHistory, input.sceneTags)
        messages += StackLayers.buildDigestLines(input.stackInstructions, input.digestLines, input.turnNumber)
        messages += StackLayers.buildExpressionMemory(input.stackInstructions, input.expressionLog)
        messages += StackLayers.buildWorldStateBlock(input.stackInstructions.worldStatePolicy, input.worldState, input.sceneTags)
        messages += StackLayers.buildKnownEntitiesBlock(input.stackInstructions.knownEntitiesPolicy, input.worldState, input.sceneTags)

        // 4. Output format contract
        if (input.stackInstructions.outputFormat.isNotBlank()) {
            messages += Message(role = "system", content = "Output format: ${input.stackInstructions.outputFormat}")
        }

        // 5. Player action
        messages += Message(role = "user", content = input.userMessage.trim())

        return messages
    }
}
