package com.example.storyforge.prompt

import kotlinx.serialization.Serializable

@Serializable
data class AiSettings(
    val selectedConnectionId: String = "",
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val maxTokens: Int = 2048,
    val presencePenalty: Float = 0.0f,
    val frequencyPenalty: Float = 0.0f,
    val functionCallingEnabled: Boolean = false // ✅ REQUIRED

)
