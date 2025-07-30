package com.example.storyforge.core

import android.content.Context
import com.example.storyforge.model.DeltaInstruction
import com.example.storyforge.model.GameState
import com.example.storyforge.model.Message
import com.example.storyforge.prompt.AiSettings

interface Narrator {
    suspend fun generate(
        messages: List<Message>,
        settings: AiSettings,
        modelName: String,
        turnId: Int,
        context: Context
    ): Result<Pair<String, Map<String, DeltaInstruction>>>

    suspend fun generateFull(
        messages: List<Message>,
        settings: AiSettings,
        modelName: String,
        turnId: Int,
        context: Context
    ): Result<NarratorResult>
}

