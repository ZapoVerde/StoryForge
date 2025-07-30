package com.example.storyforge.prompt

import kotlinx.serialization.Serializable

/**
 * A self-contained prompt card that can be submitted to the AI to configure tone, rules, or scenario.
 *
 * @param id A stable unique identifier for saving and referencing.
 * @param title Human-readable label shown in UI.
 * @param description Optional explanation for what this card does.
 * @param prompt The actual prompt content to send to the AI.
 * @param emitSkeleton Optional JSON emit/tag structure the AI should follow.
 * @param worldStateInit Optional JSON for initializing world state.
 * @param gameRules Optional text defining in-world rules and mechanics.
 * @param aiSettings Optional text or JSON for AI config like temperature or style.
 * @param tags Optional list of tags for filtering or future use.
 * @param isExample True if this card is bundled with the app by default.
 */
@Serializable
data class PromptCard(
    val id: String,
    val title: String,
    val description: String? = null,
    val prompt: String,
    val firstTurnOnlyBlock: String = "",
    val stackInstructions: String = "",

    val emitSkeleton: String = "",
    val worldStateInit: String = "",
    val gameRules: String = "",
    val aiSettings: AiSettings = AiSettings(),              // ✅ Primary AI config
    val helperAiSettings: AiSettings = AiSettings(),        // ✅ Secondary AI config
    val tags: List<String> = emptyList(),
    val isExample: Boolean = false,
    val functionDefs: String = ""
)
