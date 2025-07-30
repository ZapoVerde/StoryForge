package com.example.storyforge.save

import kotlinx.serialization.Serializable

@Serializable
data class SceneState(
    val location: String? = null,
    val present: List<String> = emptyList()
)
