package com.example.storyforge.model

import kotlinx.serialization.Serializable

@Serializable
data class StackLogEntry(
    val turn: Int,
    val timestamp: String,
    val model: String,
    val stack: List<Message>,
    val token_summary: TokenSummary,
    val latency_ms: Long
)

@Serializable
data class TokenSummary(
    val input: Int,
    val output: Int,
    val total: Int
)
