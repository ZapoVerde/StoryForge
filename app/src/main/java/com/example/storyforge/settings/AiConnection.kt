package com.example.storyforge.settings

import kotlinx.serialization.Serializable

@Serializable
data class AiConnection(
    val id: String,
    val displayName: String,
    val description: String? = null,
    val apiUrl: String,
    val apiToken: String,
    val userAgent: String = "StoryForge/1.0",
    val functionCallingEnabled: Boolean = false,
    val modelName: String, // used in UI
    val modelSlug: String, // used in API call
    val dateAdded: Long,
    val lastUpdated: Long
)
