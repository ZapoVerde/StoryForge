// ui/App.kt
package com.example.storyforge.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.storyforge.StoryForgeViewModel
import com.example.storyforge.ui.screens.NarratorScreen
import com.example.storyforge.ui.screens.SettingsScreen
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryForgeApp(viewModel: StoryForgeViewModel) {
    AppNavigation(
        viewModel = viewModel,
        startDestination = "narrator" // or your preferred default
    )
}