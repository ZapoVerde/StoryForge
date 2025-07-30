package com.example.storyforge.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.storyforge.StoryForgeViewModel
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.foundation.text.BasicTextField
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun WorldStateScreen(
    viewModel: StoryForgeViewModel,
    onNavToggle: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val worldState = gameState.worldState
    val flattened = remember(worldState) { flattenJsonObject(worldState) }
    val pinnedKeys by viewModel.pinnedKeys.collectAsState()
    var expandedCategories by remember { mutableStateOf(setOf<String>()) }
    var expandedEntities by remember { mutableStateOf(setOf<Pair<String, String>>()) }
    var renamingEntity by remember { mutableStateOf<Pair<String, String>?>(null) }

    val groupedByCategory = remember(flattened) {
        val grouped = mutableMapOf<String, MutableMap<String, MutableMap<String, JsonElement>>>()
        for ((fullKey, value) in flattened) {
            val parts = fullKey.split(".")
            if (parts.size < 3) continue
            val category = parts[0]
            val entity = parts[1]
            val variable = parts.drop(2).joinToString(".")
            val entityMap = grouped.getOrPut(category) { mutableMapOf() }
            val variableMap = entityMap.getOrPut(entity) { mutableMapOf() }
            variableMap[variable] = value
        }
        grouped
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("World State", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onNavToggle) { Text("Menu") }
        }

        Spacer(Modifier.height(8.dp))

        if (flattened.isEmpty()) {
            Text("No world state available.", style = MaterialTheme.typography.bodyMedium)
        } else {
            groupedByCategory.forEach { (category, entities) ->
                val categoryExpanded = category in expandedCategories
                val allCategoryKeys = entities.flatMap { (entity, vars) ->
                    vars.keys.map { "$category.$entity.$it" }
                }
                val categoryPinnedCount = allCategoryKeys.count { it in pinnedKeys }
                val categoryCheckboxState = when {
                    categoryPinnedCount == 0 -> ToggleableState.Off
                    categoryPinnedCount == allCategoryKeys.size -> ToggleableState.On
                    else -> ToggleableState.Indeterminate
                }

                var categoryNameText by remember(category) { mutableStateOf(TextFieldValue(category)) }
                var categoryFocused by remember { mutableStateOf(false) }

                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedCategories = if (categoryExpanded)
                                    expandedCategories - category
                                else
                                    expandedCategories + category
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (categoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        Box(
                            modifier = Modifier
                                .wrapContentWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            BasicTextField(
                                value = categoryNameText,
                                onValueChange = { categoryNameText = it },
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .onFocusChanged {
                                        if (categoryFocused && !it.isFocused) {
                                            if (categoryNameText.text != category) {
                                                viewModel.renameCategory(category, categoryNameText.text)
                                            }
                                        }
                                        categoryFocused = it.isFocused
                                    },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TriStateCheckbox(
                                state = categoryCheckboxState,
                                onClick = {
                                    val shouldPinAll = categoryCheckboxState == ToggleableState.Off
                                    allCategoryKeys.forEach { key ->
                                        val isPinned = key in pinnedKeys
                                        if (shouldPinAll && !isPinned) viewModel.togglePin(key)
                                        if (!shouldPinAll && isPinned) viewModel.togglePin(key)
                                    }
                                }
                            )
                            IconButton(
                                onClick = { viewModel.deleteWorldCategory(category) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete category",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (categoryExpanded) {
                        entities.forEach { (entity, variables) ->
                            val entityKey = category to entity
                            val entityExpanded = entityKey in expandedEntities
                            val entityPath = "$category.$entity"
                            val allEntityKeys = variables.keys.map { "$entityPath.$it" }
                            val entityPinnedCount = allEntityKeys.count { it in pinnedKeys }
                            val entityCheckboxState = when {
                                entityPinnedCount == 0 -> ToggleableState.Off
                                entityPinnedCount == allEntityKeys.size -> ToggleableState.On
                                else -> ToggleableState.Indeterminate
                            }

                            var entityNameText by remember(entity) { mutableStateOf(TextFieldValue(entity)) }
                            var entityFocused by remember { mutableStateOf(false) }

                            Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedEntities = if (entityExpanded)
                                                expandedEntities - entityKey
                                            else
                                                expandedEntities + entityKey
                                        }
                                ) {
                                    Icon(
                                        imageVector = if (entityExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .wrapContentWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        BasicTextField(
                                            value = entityNameText,
                                            onValueChange = { entityNameText = it },
                                            modifier = Modifier
                                                .wrapContentWidth()
                                                .onFocusChanged {
                                                    if (entityFocused && !it.isFocused) {
                                                        if (entityNameText.text != entity) {
                                                            viewModel.renameEntity(category, entity, entityNameText.text)
                                                        }
                                                    }
                                                    entityFocused = it.isFocused
                                                },
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TriStateCheckbox(
                                            state = entityCheckboxState,
                                            onClick = {
                                                val shouldPinAll = entityCheckboxState == ToggleableState.Off
                                                allEntityKeys.forEach { key ->
                                                    val isPinned = key in pinnedKeys
                                                    if (shouldPinAll && !isPinned) viewModel.togglePin(key)
                                                    if (!shouldPinAll && isPinned) viewModel.togglePin(key)
                                                }
                                            }
                                        )
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteEntity(
                                                    category,
                                                    entity,
                                                    level = if (category == "entities") 2 else 1
                                                )
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete entity",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                if (entityExpanded) {
                                    // 💡 Tag editor row
                                    val tagValue = variables["tag"]?.jsonPrimitive?.contentOrNull ?: ""
                                    var tagEdit by remember { mutableStateOf(TextFieldValue(tagValue)) }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, bottom = 6.dp)
                                    ) {
                                        Text("Tag:", modifier = Modifier.padding(end = 8.dp))
                                        OutlinedTextField(
                                            value = tagEdit,
                                            onValueChange = {
                                                val raw = it.text.trim()
                                                // Allow empty, or require prefix for non-empty tags
                                                val isValid = raw.isEmpty() || raw.startsWith("#") || raw.startsWith("@")
                                                if (isValid) {
                                                    tagEdit = it
                                                    viewModel.editWorldKey("$category.$entity.tag", JsonPrimitive(raw))
                                                }
                                            }
                                            ,
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.06f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    ) {
                                        variables.forEach { (varName, value) ->
                                            val fullKey = "$category.$entity.$varName"
                                            WorldStateItemRow(
                                                key = fullKey,
                                                value = value,
                                                viewModel = viewModel,
                                                onDelete = { viewModel.deleteWorldKey(fullKey) },
                                                onEdit = { k, v -> viewModel.editWorldKey(k, v) }
                                            )
                                        }
                                    }
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
fun WorldStateItemRow(
    key: String,
    value: JsonElement,
    viewModel: StoryForgeViewModel,
    onDelete: () -> Unit,
    onEdit: (String, JsonElement) -> Unit
) {
    var editMode by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(TextFieldValue(value.toString())) }
    val displayName = key.substringAfterLast(".")

    // Collect pinned keys as state
    val pinnedKeys by viewModel.pinnedKeys.collectAsState()
    val isPinned = key in pinnedKeys

    // Debug logs
    LaunchedEffect(key) {
        Log.d("PIN_DEBUG", "Rendering $key | Pinned: $isPinned")
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$displayName:",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            Checkbox(
                checked = isPinned,
                onCheckedChange = {
                    Log.d("PIN_DEBUG", "Checkbox clicked for $key")
                    viewModel.togglePin(key)
                },
                modifier = Modifier.size(24.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }


        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp)
        ) {
            if (editMode) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                editMode = false
                                parseJsonPrimitive(editText.text)?.let { parsedValue ->
                                    onEdit(key, parsedValue)  // Now passing both key and value
                                }
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Confirm Edit",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { editMode = true }
                        .padding(vertical = 6.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

fun flattenJsonObject(obj: JsonObject, prefix: String = ""): Map<String, JsonElement> {
    val result = mutableMapOf<String, JsonElement>()

    for ((k, v) in obj) {
        val fullKey = if (prefix.isEmpty()) k else "$prefix.$k"

        when (v) {
            is JsonObject -> {
                val nested = flattenJsonObject(v, fullKey)
                result.putAll(nested)
            }
            else -> {
                result[fullKey] = v
            }
        }
    }

    return result
}



fun parseJsonPrimitive(text: String): JsonElement? {
    val trimmed = text.trim()

    return when {
        trimmed.equals("true", ignoreCase = true) -> JsonPrimitive(true)
        trimmed.equals("false", ignoreCase = true) -> JsonPrimitive(false)
        trimmed.toIntOrNull() != null -> JsonPrimitive(trimmed.toInt())
        trimmed.toDoubleOrNull() != null -> JsonPrimitive(trimmed.toDouble())
        trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2 ->
            JsonPrimitive(trimmed.removeSurrounding("\""))
        // Fallback: if string doesn't parse cleanly, treat as raw string
        else -> JsonPrimitive(trimmed)
    }
}

