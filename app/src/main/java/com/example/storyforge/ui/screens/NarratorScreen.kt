package com.example.storyforge.ui.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.storyforge.StoryForgeViewModel
import com.example.storyforge.save.NarratorUiState
import com.example.storyforge.utils.DiceRoller
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NarratorScreen(
    viewModel: StoryForgeViewModel,
    onNavToggle: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val turnList by viewModel.turns.collectAsState()
    val worldChangeMessage by viewModel.worldChangeMessage.collectAsState()
    val narratorUiState by viewModel.narratorUiState.collectAsState()

    var inputText by rememberSaveable { mutableStateOf(narratorUiState.inputText) }
    var currentRollFormula by remember { mutableStateOf("2d6") }
    var showRollDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Restore scroll position
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            listState.scrollToItem(narratorUiState.scrollPosition)
        }
    }

    // Track scroll position for snapshot
    LaunchedEffect(listState.firstVisibleItemIndex) {
        viewModel.updateNarratorUiState(
            narratorUiState.copy(scrollPosition = listState.firstVisibleItemIndex)
        )
    }

    // Auto-scroll on new turn
    LaunchedEffect(turnList) {
        if (turnList.isNotEmpty()) {
            listState.animateScrollToItem(turnList.size - 1)
        }
    }

    // Trigger snackbar when worldChangeMessage appears
    LaunchedEffect(worldChangeMessage) {
        if (!worldChangeMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(worldChangeMessage!!)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Narrator", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onNavToggle) { Text("Menu") }
            }

            PinnedItemsSection(viewModel)
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(turnList) { turn ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(text = "You: ${turn.user}", style = MaterialTheme.typography.bodyMedium)
                            if (turn.narrator.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "AI: ${turn.narrator}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .combinedClickable(
                            onClick = {
                                val result = DiceRoller.roll(currentRollFormula)
                                val summary = DiceRoller.format(result)
                                viewModel.processAction("Roll: $currentRollFormula\n$summary")
                            },
                            onLongClick = { showRollDialog = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Casino, contentDescription = "Roll Dice")
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    viewModel.updateNarratorUiState(narratorUiState.copy(inputText = it))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                placeholder = { Text("What do you do?") },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.processAction(inputText)
                                inputText = ""
                                coroutineScope.launch {
                                    listState.animateScrollToItem(turnList.size)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )

        if (showRollDialog) {
            var input by remember { mutableStateOf(currentRollFormula) }
            AlertDialog(
                onDismissRequest = { showRollDialog = false },
                title = { Text("Set Dice Formula") },
                text = {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("e.g. 1d20+3 or 3d6") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        currentRollFormula = input
                        showRollDialog = false
                    }) { Text("Set") }
                },
                dismissButton = {
                    TextButton(onClick = { showRollDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun PinnedItemsSection(viewModel: StoryForgeViewModel) {
    val gameState by viewModel.gameState.collectAsState()
    val pinnedKeys by viewModel.pinnedKeys.collectAsState()

    val flatWorld = flattenJsonObject(gameState.worldState)
    val pinnedItems = pinnedKeys.mapNotNull { key -> flatWorld[key]?.let { key to it } }

    if (pinnedItems.isNotEmpty()) {
        val grouped = pinnedItems
            .groupBy { it.first.substringBeforeLast(".") }
            .mapValues { entry ->
                entry.value.associate { it.first.substringAfterLast(".") to it.second }
            }

        FlowRow(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            grouped.forEach { (entityPath, attributes) ->
                val allKeys = attributes.keys.map { "$entityPath.$it" }

                Surface(
                    modifier = Modifier
                        .wrapContentSize()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                viewModel.togglePin(entityPath)
                            }
                        ),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .wrapContentWidth()
                    ) {
                        Text(
                            text = entityPath.split(".").last().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            attributes.forEach { (label, value) ->
                                val fullKey = "$entityPath.$label"
                                MiniAttrRow(label = label, value = value) {
                                    viewModel.togglePin(fullKey)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun MiniAttrRow(label: String, value: JsonElement, onLongPress: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {},
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 1.dp), // ✅ minimal spacing
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(1.dp)) // ✅ smaller gap
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
