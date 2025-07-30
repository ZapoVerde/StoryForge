package com.example.storyforge.model

import kotlinx.serialization.Serializable


@Serializable
data class DigestLine(
    val turn: Int,               // Corresponds to which turn this line was emitted from
    val tags: List<String>,      // Inline tags like #dingo, #inn
    val score: Int,              // Importance score from 1 (least) to 5 (most)
    val text: String             // The natural-language digest line
)
