package com.example.storyforge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.storyforge.StoryForgeViewModel
import com.example.storyforge.prompt.PromptCard
import com.example.storyforge.ui.screens.*
import androidx.compose.ui.platform.LocalContext

import java.util.UUID

@Composable
fun AppNavigation(
    viewModel: StoryForgeViewModel,
    startDestination: String = "narrator"
) {
    val navController = rememberNavController()
    var showNavOverlay by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable("narrator") {
                NarratorScreen(viewModel) { showNavOverlay = true }
            }
            composable("prompt_cards") {
                val settings = viewModel.settings
                PromptCardScreen(
                    viewModel = viewModel,
                    onNavToggle = { showNavOverlay = true },
                    navController = navController,
                    currentSettings = settings
                )
            }
            composable("world_state") {
                WorldStateScreen(viewModel) { showNavOverlay = true }
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavToggle = { showNavOverlay = true }
                )
            }
            composable("saves") {
                SaveManagerScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onNavToggle = { showNavOverlay = true }
                )
            }
            composable("library") {
                PromptCardLibraryScreen(
                    viewModel = viewModel,
                    cards = viewModel.promptCards.collectAsState().value,
                    onCancel = { navController.popBackStack() }
                )
            }
            composable("logs") {
                val context = LocalContext.current
                LogViewerScreen(
                    viewModel = viewModel, // or whatever variable you're using
                    onNavToggle = { showNavOverlay = true }
                )
            }


        }

        if (showNavOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
            ) {
                NavigationOverlay(
                    navController = navController,
                    onClose = { showNavOverlay = false }
                )
            }
        }
    }
}

@Composable
fun NavigationOverlay(
    navController: NavHostController,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Navigation", style = MaterialTheme.typography.titleLarge)

        Button(onClick = {
            navController.navigate("narrator") { popUpTo(0) }
            onClose()
        }) { Text("Narrator") }

        Button(onClick = {
            navController.navigate("prompt_cards") { popUpTo(0) }
            onClose()
        }) { Text("Prompt Cards") }

        Button(onClick = {
            navController.navigate("world_state") { popUpTo(0) }
            onClose()
        }) { Text("World State") }

        Button(onClick = {
            navController.navigate("settings") { popUpTo(0) }
            onClose()
        }) { Text("Settings") }

        Button(onClick = {
            navController.navigate("saves") { popUpTo(0) }
            onClose()
        }) { Text("Manage Saves") }

        Button(onClick = {
            navController.navigate("logs") { popUpTo(0) }
            onClose()
        }) { Text("Log Viewer") }


        Spacer(Modifier.weight(1f))

        Button(onClick = onClose) {
            Text("Close")
        }
    }
}
