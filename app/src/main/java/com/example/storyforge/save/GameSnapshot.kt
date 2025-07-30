package com.example.storyforge.save

import com.example.storyforge.prompt.PromptCard
import com.example.storyforge.logging.TurnLogEntry
import com.example.storyforge.model.ConversationTurn
import com.example.storyforge.model.DigestLine
import com.example.storyforge.model.GameState
import com.example.storyforge.model.StackLogEntry
import kotlinx.serialization.Serializable


@Serializable
data class GameSnapshot(
    val promptCard: PromptCard,
    val gameState: GameState,
    val turns: List<ConversationTurn>,
    val digestLines: List<DigestLine>,
    val worldDeltas: List<DeltaLogEntry>,
    val turnLogs: List<TurnLogEntry>,
    val stackLogs: List<StackLogEntry>,
    val sceneState: SceneState,
    val narratorUiState: NarratorUiState,
    val timestamp: String
)
