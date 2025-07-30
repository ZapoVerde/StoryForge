package com.example.storyforge.model

import kotlinx.serialization.Serializable

@Serializable
data class ConversationTurn(
    val user: String,
    val narrator: String
)
