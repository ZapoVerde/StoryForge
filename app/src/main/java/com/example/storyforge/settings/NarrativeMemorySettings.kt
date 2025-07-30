package com.example.storyforge.settings

import kotlinx.serialization.Serializable

@Serializable
data class NarrativeMemorySettings(
    val bufferTurns: Int = 3,           // N: Number of full turns to include in prompt
    val digestPruneX: Int = 20,         // X: Age threshold to start pruning score < 3
    val digestPruneY: Int = 50,         // Y: Age threshold to prune score < 4
    val digestPruneZ: Int = 100         // Z: Final threshold, keep only score = 5
)
