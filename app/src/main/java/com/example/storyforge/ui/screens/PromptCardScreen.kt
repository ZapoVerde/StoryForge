package com.example.storyforge.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.storyforge.StoryForgeViewModel
import com.example.storyforge.prompt.PromptCard
import com.example.storyforge.settings.AiConnection
import com.example.storyforge.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Composable
fun PromptCardScreen(
    viewModel: StoryForgeViewModel,
    onNavToggle: () -> Unit,
    navController: NavController,
    currentSettings: Settings
) {
    val activeCard by viewModel.activePromptCard.collectAsState()
    var editedCard by remember(activeCard) {
        mutableStateOf(activeCard?.copy())
    }
    val savedCards by viewModel.promptCards.collectAsState()
    val matchingSavedCard = savedCards.find { it.id == activeCard?.id }
    val isUnapplied = activeCard != null && editedCard != null &&
            Json.encodeToString(activeCard) != Json.encodeToString(editedCard)
    val isUnsaved = matchingSavedCard != null && editedCard != null &&
            Json.encodeToString(matchingSavedCard) != Json.encodeToString(editedCard)
    var showSaveDialog by remember { mutableStateOf(false) }
    val availableConnections by viewModel.aiConnections.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Prompt Cards", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onNavToggle) { Text("Menu") }
        }

        // Game Library access
        Button(onClick = { navController.navigate("library") }) {
            Text("Open Game Library")
        }

        // Active card panel
        if (activeCard != null && editedCard != null) {
            val selectedHelperConnectionId = editedCard!!.helperAiSettings.selectedConnectionId
            val onHelperConnectionSelected = { newId: String ->
                val old = editedCard!!.helperAiSettings
                editedCard = editedCard!!.copy(helperAiSettings = old.copy(selectedConnectionId = newId))
            }

            PromptCardEditor(
                card = editedCard!!,
                onDirtyChange = {},
                onCardChange = { updated -> editedCard = updated },
                availableConnections = availableConnections,
                selectedConnectionId = editedCard!!.aiSettings.selectedConnectionId,
                onConnectionSelected = { newId ->
                    val old = editedCard!!.aiSettings
                    editedCard = editedCard!!.copy(aiSettings = old.copy(selectedConnectionId = newId))
                },
                selectedHelperConnectionId = selectedHelperConnectionId,
                onHelperConnectionSelected = onHelperConnectionSelected
            )




        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        editedCard = activeCard!!.copy()
                    },
                    enabled = isUnsaved,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Revert to Saved")
                }

                Button(
                    onClick = { showSaveDialog = true },
                    enabled = isUnsaved,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }

                Button(
                    onClick = {
                        try {
                            viewModel.setActivePromptCard(editedCard!!)
                        } catch (e: Exception) {
                            Log.e("PromptCard", "Failed to push to live", e)
                        }
                    },
                    enabled = isUnapplied,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Push to Live")
                }
            }
        }
    }

    if (showSaveDialog && editedCard != null) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Prompt Card") },
            text = { Text("How would you like to save this prompt card?") },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val copy = editedCard!!.copy(
                            id = UUID.randomUUID().toString(),
                            title = editedCard!!.title + " (Copy)"
                        )
                        viewModel.addPromptCard(copy)
                    } catch (e: Exception) {
                        Log.e("PromptCard", "Failed to save copy", e)
                    }
                    showSaveDialog = false
                }) {
                    Text("Save as New")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        try {
                            viewModel.addPromptCard(editedCard!!)
                        } catch (e: Exception) {
                            Log.e("PromptCard", "Failed to update original", e)
                        }
                        showSaveDialog = false
                    }) {
                        Text("Update Original")
                    }
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
