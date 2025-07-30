package com.example.storyforge.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.storyforge.StoryForgeViewModel
import com.example.storyforge.prompt.AiSettings
import com.example.storyforge.prompt.PromptCard
import java.util.UUID

@Composable
fun PromptCardLibraryScreen(
    viewModel: StoryForgeViewModel,
    cards: List<PromptCard>,
    onCancel: () -> Unit
) {
    var selectedCard by remember { mutableStateOf<PromptCard?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingCard by remember { mutableStateOf<PromptCard?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }


    fun fireCardNow(card: PromptCard) {
        viewModel.setActivePromptCard(card)
        viewModel.resetSession()
        viewModel.processAction(card.prompt)
        try {
            viewModel.saveToSlot(card.title)
        } catch (e: Exception) {
            // Log or handle
        }
        onCancel()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Game Library", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onCancel) { Text("Close") }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cards) { card ->
                val isSelected = selectedCard?.id == card.id
                val bgColor = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCard = card },
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(card.title, style = MaterialTheme.typography.titleMedium)
                        card.description?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        if (isSelected) {
                            Spacer(Modifier.height(8.dp))
                            Text("Selected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

        }
//connectionId
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {
                val newCard = PromptCard(
                    id = UUID.randomUUID().toString(),
                    title = "",
                    prompt = "",
                    aiSettings = AiSettings(
                        selectedConnectionId = viewModel.settings.aiConnections.firstOrNull()?.id ?: ""
                    )
                )
                viewModel.addPromptCard(newCard)
                fireCardNow(newCard)
            }, modifier = Modifier.weight(1f)) {
                Text("Create New")
            }

            Button(
                onClick = {
                    selectedCard?.let {
                        if (viewModel.turns.value.isNotEmpty()) {
                            pendingCard = it
                            showConfirmDialog = true
                        } else fireCardNow(it)
                    }
                },
                enabled = selectedCard != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Load")
            }

            Button(
                onClick = {
                    selectedCard?.let {
                        val clone = it.copy(
                            id = UUID.randomUUID().toString(),
                            title = it.title + " (Copy)"
                        )
                        viewModel.addPromptCard(clone)
                        if (viewModel.turns.value.isNotEmpty()) {
                            pendingCard = clone
                            showConfirmDialog = true
                        } else fireCardNow(clone)
                    }
                },
                enabled = selectedCard != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Clone")
            }

            Button(
                onClick = {
                    showDeleteDialog = true
                },
                enabled = selectedCard != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.onError)
            }
        }
    }

    if (showConfirmDialog && pendingCard != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Start New Game?") },
            text = {
                Text("You have an existing game in progress. Starting a new game will reset the current session. Your progress is autosaved.")
            },
            confirmButton = {
                TextButton(onClick = {
                    fireCardNow(pendingCard!!)
                    pendingCard = null
                    showConfirmDialog = false
                }) {
                    Text("Start New Game")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    pendingCard = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showDeleteDialog && selectedCard != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Prompt Card?") },
            text = {
                Text("Are you sure you want to permanently delete “${selectedCard!!.title.ifBlank { "(Untitled)" }}”? This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePromptCard(selectedCard!!.id)
                    selectedCard = null
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

}
