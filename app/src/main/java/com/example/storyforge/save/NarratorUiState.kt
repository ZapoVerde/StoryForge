package com.example.storyforge.save

import kotlinx.serialization.Serializable

@Serializable
data class NarratorUiState(
    val inputText: String = "",
    val selectedLogTabs: List<String> = emptyList(),
    val scrollPosition: Int = 0
)
