package com.example.storyforge.settings

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val useDummyNarrator: Boolean = true,
    val apiUrl: String = "https://api.example.com",
    val debugMode: Boolean = false,
    val apiToken: String? = null,
    val aiConnections: List<AiConnection> = emptyList()

)
