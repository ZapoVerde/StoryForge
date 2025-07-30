package com.example.storyforge.logging

/**
 * Represents recoverable or inspectable issues detected during a turn's processing.
 */
enum class LogErrorFlag {
    JSON_PARSE_FAILED,      // Failed to extract JSON blocks from narrator output
    DELTA_APPLY_FAILED,     // World state could not be updated with emitted deltas
    DIGEST_MISSING,         // Narrator response lacked any digest lines
    CONTEXT_TOO_LONG,       // Stack exceeded max token limit and was truncated
    MODEL_TIMEOUT,          // AI model timed out or returned no response
    TRUNCATED_RESPONSE,     // Narrator reply was incomplete or cut off
    UNKNOWN_ERROR           // Fallback for unclassified failures
}
