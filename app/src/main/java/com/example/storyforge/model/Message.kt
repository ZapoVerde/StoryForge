package com.example.storyforge.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val role: String,     // "system", "user", "assistant"
    val content: String
)
