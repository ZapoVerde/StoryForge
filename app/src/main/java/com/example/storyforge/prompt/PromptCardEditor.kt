package com.example.storyforge.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialogDefaults.containerColor
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.storyforge.prompt.PromptCard
import com.example.storyforge.settings.AiConnection
import java.util.*
import androidx.compose.ui.Alignment
import com.example.storyforge.prompt.AiSettings
import com.example.storyforge.prompt.PromptCardDefaults


@Composable
fun PromptCardEditor(
    card: PromptCard,
    onDirtyChange: (Boolean) -> Unit,
    onCardChange: (PromptCard) -> Unit,
    availableConnections: List<AiConnection>, // ✅ ADD THIS
    selectedConnectionId: String?,
    onConnectionSelected: (String) -> Unit,
    selectedHelperConnectionId: String?,      // ✅ ADD THIS
    onHelperConnectionSelected: (String) -> Unit // ✅ ADD THIS
) {
    var title by remember { mutableStateOf(card.title) }
    var description by remember { mutableStateOf(card.description ?: "") }
    var prompt by remember { mutableStateOf(card.prompt) }
    var worldState by remember { mutableStateOf(card.worldStateInit) }
    var gameRules by remember { mutableStateOf(card.gameRules) }
    var functionDefs by remember { mutableStateOf(card.functionDefs) }
    var aiSettings by remember { mutableStateOf(card.aiSettings) }
    var helperAiSettings by remember { mutableStateOf(card.helperAiSettings) }
    var firstTurnOnlyBlock by remember {
        mutableStateOf(
            if (card.firstTurnOnlyBlock.isBlank()) PromptCardDefaults.DEFAULT_FIRST_TURN else card.firstTurnOnlyBlock
        )
    }

    var stackInstructions by remember {
        mutableStateOf(
            if (card.stackInstructions.isBlank()) PromptCardDefaults.DEFAULT_STACK_INSTRUCTIONS else card.stackInstructions
        )
    }

    var emitSkeleton by remember {
        mutableStateOf(
            if (card.emitSkeleton.isBlank()) PromptCardDefaults.DEFAULT_EMIT_SKELETON else card.emitSkeleton
        )
    }



    val currentCard = card.copy(
        title = title,
        description = description.ifBlank { null },
        prompt = prompt,
        firstTurnOnlyBlock = firstTurnOnlyBlock,
        stackInstructions = stackInstructions,
        emitSkeleton = emitSkeleton,
        worldStateInit = worldState,
        gameRules = gameRules,
        functionDefs = functionDefs,
        aiSettings = aiSettings.copy(selectedConnectionId = selectedConnectionId ?: ""),
        helperAiSettings = helperAiSettings.copy(selectedConnectionId = selectedHelperConnectionId ?: "")
    )


    LaunchedEffect(currentCard) {
        onDirtyChange(currentCard != card)
        onCardChange(currentCard)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        TitleSection(title, description, onTitleChange = { title = it }, onDescriptionChange = { description = it })
        AiPromptSection(prompt, onPromptChange = { prompt = it })
        FirstTurnSceneSection(firstTurnOnlyBlock) { firstTurnOnlyBlock = it }
        EmitRulesSection(emitSkeleton, onEmitChange = { emitSkeleton = it })
        WorldStateSection(worldState, onWorldStateChange = { worldState = it })
        GameRulesSection(gameRules, onRulesChange = { gameRules = it })


        AiSettingsSection(
            label = "Primary AI Settings",
            settings = aiSettings,
            onSettingsChange = { updated ->
                aiSettings = updated
                onDirtyChange(true)
            },
            availableConnections = availableConnections,
            selectedConnectionId = selectedConnectionId,
            onConnectionSelected = onConnectionSelected
        )

        AiSettingsSection(
            label = "Helper AI Settings",
            settings = helperAiSettings,
            onSettingsChange = { updated ->
                helperAiSettings = updated
                onDirtyChange(true)
            },
            availableConnections = availableConnections,
            selectedConnectionId = selectedHelperConnectionId,
            onConnectionSelected = onHelperConnectionSelected
        )


        FunctionDefinitionsSection(functionDefs) { functionDefs = it }
        StackInstructionsSection(stackInstructions) { stackInstructions = it }


        Spacer(Modifier.height(12.dp))
    }
}


@Composable
fun CollapsibleSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsSection(
    label: String,
    settings: AiSettings,
    onSettingsChange: (AiSettings) -> Unit,
    availableConnections: List<AiConnection>,
    selectedConnectionId: String?,
    onConnectionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedConn = availableConnections.find { it.id == selectedConnectionId }

    CollapsibleSection(label, initiallyExpanded = false) {

    // Connection dropdown
        var connDropdownExpanded by remember { mutableStateOf(false) }
        val selectedConn = availableConnections.find { it.id == selectedConnectionId }

        ExposedDropdownMenuBox(
            expanded = connDropdownExpanded,
            onExpandedChange = { connDropdownExpanded = !connDropdownExpanded }
        ) {
            OutlinedTextField(
                value = selectedConn?.let { "${it.displayName} – ${it.modelName}" } ?: "Select Connection",
                onValueChange = {},
                readOnly = true,
                label = { Text("AI Connection") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = connDropdownExpanded)
                },
                modifier = Modifier
                    .menuAnchor() // ✅ absolutely required for click detection
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = connDropdownExpanded,
                onDismissRequest = { connDropdownExpanded = false }
            ) {
                availableConnections.forEach { conn ->
                    DropdownMenuItem(
                        text = { Text("${conn.displayName} – ${conn.modelName}") },
                        onClick = {
                            onConnectionSelected(conn.id)
                            connDropdownExpanded = false
                        }
                    )
                }
            }
        }


        Spacer(Modifier.height(12.dp))

        // Sliders and tuning
        LabeledSlider("Temperature", settings.temperature, 0.0f..1.5f, 0.1f) {
            onSettingsChange(settings.copy(temperature = it))
        }
        Text(
            "Controls randomness. Low = logical, high = creative. RPG-optimal: 0.7–1.0.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LabeledSlider("Top P", settings.topP, 0.0f..1.0f, 0.05f) {
            onSettingsChange(settings.copy(topP = it))
        }
        Text(
            "Controls diversity. Lower = focused, higher = expressive. RPG-optimal: 0.8–1.0.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LabeledSlider("Max Tokens", settings.maxTokens.toFloat(), 256f..8192f, 256f) {
            onSettingsChange(settings.copy(maxTokens = it.toInt()))
        }
        Text(
            "Maximum length of AI reply. Longer = more story depth. RPG-optimal: 1024–4096.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LabeledSlider("Presence Penalty", settings.presencePenalty, -2.0f..2.0f, 0.1f) {
            onSettingsChange(settings.copy(presencePenalty = it))
        }
        Text(
            "Discourages introducing new topics repeatedly. RPG-optimal: 0.0–0.5.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LabeledSlider("Frequency Penalty", settings.frequencyPenalty, -2.0f..2.0f, 0.1f) {
            onSettingsChange(settings.copy(frequencyPenalty = it))
        }
        Text(
            "Discourages repeating phrases. Helps avoid spam. RPG-optimal: 0.2–0.8.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enable Function Calling")
            Spacer(Modifier.weight(1f))
            Switch(
                checked = settings.functionCallingEnabled,
                onCheckedChange = {
                    onSettingsChange(settings.copy(functionCallingEnabled = it))
                }
            )
        }
        Text(
            "Allows AI to call structured functions (if you've defined them in the prompt).",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
@Composable
fun FirstTurnSceneSection(value: String, onChange: (String) -> Unit) {
    CollapsibleSection("First Turn Scene Setup") {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text("Intro scene shown only on turn 1") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
    }
}

@Composable
fun StackInstructionsSection(value: String, onChange: (String) -> Unit) {
    CollapsibleSection("🧠 Stack Instructions (JSON)") {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text("Stack Assembly Policy") },
            placeholder = { Text("{ \"digestPolicy\": { ... } }") },
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
    }
}


@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label: ${"%.2f".format(value)}")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = ((valueRange.endInclusive - valueRange.start) / step).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
@Composable
fun TitleSection(
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    CollapsibleSection("Title & Description", initiallyExpanded = true) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}

@Composable
fun AiPromptSection(prompt: String, onPromptChange: (String) -> Unit) {
    CollapsibleSection("AI Prompt", initiallyExpanded = true) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            label = { Text("AI Prompt") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )
    }
}

@Composable
fun EmitRulesSection(emit: String, onEmitChange: (String) -> Unit) {
    CollapsibleSection("Emit & Tagging Skeleton") {
        OutlinedTextField(
            value = emit,
            onValueChange = onEmitChange,
            label = { Text("Emit/Tagging Rules") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )
    }
}

@Composable
fun WorldStateSection(worldState: String, onWorldStateChange: (String) -> Unit) {
    CollapsibleSection("World State Initialization") {
        OutlinedTextField(
            value = worldState,
            onValueChange = onWorldStateChange,
            label = { Text("Initial World State") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )
    }
}

@Composable
fun GameRulesSection(rules: String, onRulesChange: (String) -> Unit) {
    CollapsibleSection("Game Rules Skeleton") {
        OutlinedTextField(
            value = rules,
            onValueChange = onRulesChange,
            label = { Text("Game Rules") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )
    }
}

@Composable
fun FunctionDefinitionsSection(
    functionDefs: String,
    onFunctionDefsChange: (String) -> Unit
) {
    CollapsibleSection("Function Definitions") {
        OutlinedTextField(
            value = functionDefs,
            onValueChange = onFunctionDefsChange,
            label = { Text("Paste Function Definitions") },
            placeholder = { Text("{ \"name\": \"myFunction\", \"parameters\": { ... } }") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryAiConnectionDropdown(
    availableConnections: List<AiConnection>,
    selectedId: String?,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = availableConnections.find { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.displayName} – ${it.modelName}" } ?: "Select Helper AI",
            onValueChange = {},
            readOnly = true,
            label = { Text("Helper AI Connection") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenu(expanded, { expanded = false }) {
            availableConnections.forEach { conn ->
                DropdownMenuItem(
                    text = { Text("${conn.displayName} – ${conn.modelName}") },
                    onClick = {
                        onSelected(conn.id)
                        expanded = false
                    }
                )
            }
        }
    }

}
