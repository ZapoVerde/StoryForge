package com.example.storyforge

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storyforge.core.Narrator
import com.example.storyforge.prompt.PromptCardStorage
import com.example.storyforge.settings.Settings

class StoryForgeViewModelFactory(
    private val narratorFactory: () -> Narrator,
    private val settings: Settings,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoryForgeViewModel::class.java)) {
            val promptCardStorage = PromptCardStorage(appContext)
            return StoryForgeViewModel(
                //aiNarrator = narratorFactory(), // instantiate on demand
                narratorFactory = narratorFactory,
                _settings = settings,
                promptCardStorage = promptCardStorage,
                appContext = appContext
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
