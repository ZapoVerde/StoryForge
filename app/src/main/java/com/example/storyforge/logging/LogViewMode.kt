package com.example.storyforge.logging

/**
 * Declares available panel types in the Log Review carousel.
 */
enum class LogViewMode {
    RAW,        // Original user + narrator text
    DIGEST,     // Summary digest line
    DELTAS,     // World state changes
    CONTEXT,    // Full prompt stack sent to narrator
    TOKENS,     // Token usage stats (input/output)
    SETTINGS,   // AI model settings for this turn
    ERRORS,     // Detected issues during processing
    API         // Pure API call/response (no context or game state)
}
