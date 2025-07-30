package com.example.storyforge.settings

object ModelRegistry {
    val knownModels: Map<String, List<String>> = mapOf(
        "OpenAI" to listOf("gpt-4", "gpt-4o", "gpt-3.5-turbo"),
        "DeepSeek" to listOf("deepseek-chat", "deepseek-coder", "deepseek-coder-v3"),
        "Anthropic" to listOf("claude-3-haiku", "claude-3-sonnet", "claude-3-opus-20240229"),
        "Mistral" to listOf("mistral-tiny", "mistral-small", "mistral-medium"),
        "Google" to listOf("gemini-1.0-pro", "gemini-1.5-pro"),
        "Together.ai" to listOf(
            "mistralai/Mistral-7B-Instruct-v0.2",
            "meta-llama/Llama-3-70b-chat-hf",
            "codellama/CodeLlama-34b-Instruct-hf"
        )
    )

    val allModels: List<String> = knownModels.values.flatten().distinct()
}
