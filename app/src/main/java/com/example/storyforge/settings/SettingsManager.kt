package com.example.storyforge.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

object SettingsManager {
    private const val FILE_NAME = "settings.json"
    private val _settingsState = MutableStateFlow<Settings?>(null)
    val settingsState: StateFlow<Settings?> = _settingsState.asStateFlow()

    fun load(context: Context): Settings {
        val file = File(context.filesDir, FILE_NAME)
        val settings: Settings = if (!file.exists()) {
            val now = System.currentTimeMillis()
            val default = Settings(
                aiConnections = listOf(
                    AiConnection(
                        id = UUID.randomUUID().toString(),
                        displayName = "DeepSeek Coder",
                        apiUrl = "https://api.deepseek.com/v1",
                        apiToken = "",
                        functionCallingEnabled = true,
                        modelSlug = "deepseek-coder",
                        modelName = "deepseek-coder",
                        dateAdded = now,
                        lastUpdated = now
                    ),
                    AiConnection(
                        id = UUID.randomUUID().toString(),
                        displayName = "OpenAI GPT-4o",
                        apiUrl = "https://api.openai.com/v1",
                        apiToken = "",
                        functionCallingEnabled = true,
                        modelSlug = "gpt-4o",
                        modelName = "gpt-4o",
                        dateAdded = now,
                        lastUpdated = now
                    ),
                    AiConnection(
                        id = UUID.randomUUID().toString(),
                        displayName = "Anthropic Claude 3 Haiku",
                        apiUrl = "https://api.anthropic.com/v1",
                        apiToken = "",
                        functionCallingEnabled = false,
                        modelSlug = "claude-3-haiku",
                        modelName = "claude-3-haiku",
                        dateAdded = now,
                        lastUpdated = now
                    )
                )
            )

            saveSettings(context, default)
            default
        } else {
            val raw = file.readText()
            Json.decodeFromString(Settings.serializer(), raw)
        }

        _settingsState.value = settings
        return settings
    }

    fun update(context: Context, newSettings: Settings) {
        saveSettings(context, newSettings)
    }

    private fun saveSettings(context: Context, settings: Settings) {
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(Json.encodeToString(Settings.serializer(), settings))
        _settingsState.value = settings
    }
}
