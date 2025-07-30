package com.example.storyforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.storyforge.core.AINarrator
import com.example.storyforge.core.DummyNarrator
import com.example.storyforge.core.Narrator
import com.example.storyforge.core.SecretsLoader
import com.example.storyforge.settings.SettingsManager
import com.example.storyforge.ui.StoryForgeApp
import com.example.storyforge.ui.theme.StoryForgeTheme

class MainActivity : ComponentActivity() {
    private val viewModel: StoryForgeViewModel by viewModels {
        val settings = SettingsManager.load(applicationContext)
        val narratorFactory = {
            if (settings.useDummyNarrator) DummyNarrator()
            else settings.aiConnections.firstOrNull()?.let {
                AINarrator.fromConnection(it)
            } ?: DummyNarrator() // fallback if no AI connections

        }

        StoryForgeViewModelFactory(
            narratorFactory = narratorFactory,
            settings = settings,
            appContext = applicationContext
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StoryForgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StoryForgeApp(viewModel = viewModel)
                }
            }
        }
    }
}
