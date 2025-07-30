package com.example.storyforge.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.storyforge.StoryForgeViewModel
import com.example.storyforge.core.AINarrator
import com.example.storyforge.core.DummyNarrator
import com.example.storyforge.network.testConnection
import com.example.storyforge.settings.AiConnection
import com.example.storyforge.settings.SettingsManager
import com.example.storyforge.settings.ModelRegistry
import com.example.storyforge.utils.RestartAppHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StoryForgeViewModel,
    onNavToggle: () -> Unit
) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(SettingsManager.load(context)) }
    var selectedConnection by remember { mutableStateOf<AiConnection?>(null) }
    var editingConnection by remember { mutableStateOf<AiConnection?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onNavToggle) { Text("Menu") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val useDummy by viewModel.useDummyNarrator.collectAsState()

        SettingSwitch(
            label = "Use Dummy Narrator",
            checked = useDummy,
            onToggle = { enabled ->
                viewModel.setUseDummyNarrator(enabled)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (enabled) "Dummy Narrator enabled" else "Dummy Narrator disabled"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text("AI Connections", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 280.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                settings.aiConnections.forEach { conn ->
                    val isSelected = conn.id == selectedConnection?.id
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        tonalElevation = if (isSelected) 3.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedConnection = conn }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(conn.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(conn.modelName, style = MaterialTheme.typography.bodySmall)
                            Text(conn.apiUrl, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    editingConnection = AiConnection(
                        id = UUID.randomUUID().toString(),
                        displayName = "",
                        apiUrl = "",
                        apiToken = "",
                        functionCallingEnabled = false,
                        modelName = "",
                        modelSlug = "",
                        dateAdded = System.currentTimeMillis(),
                        lastUpdated = System.currentTimeMillis()
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Add")
            }

            Button(
                onClick = { editingConnection = selectedConnection },
                enabled = selectedConnection != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Edit")
            }

            Button(
                onClick = {
                    selectedConnection?.let { conn ->
                        val updated = settings.copy(
                            aiConnections = settings.aiConnections.filterNot { it.id == conn.id }
                        )
                        settings = updated
                        selectedConnection = null
                        SettingsManager.update(context, updated)
                    }
                },
                enabled = selectedConnection != null,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        }

        editingConnection?.let { conn ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                    .padding(12.dp)
            ) {
                var name by remember { mutableStateOf(conn.displayName) }
                var url by remember { mutableStateOf(conn.apiUrl) }
                var token by remember { mutableStateOf(conn.apiToken) }
                var userAgent by remember { mutableStateOf(conn.userAgent) }

                var functionEnabled by remember { mutableStateOf(conn.functionCallingEnabled) }
                var modelName by remember { mutableStateOf(conn.modelName) }
                var modelSlug by remember { mutableStateOf(conn.modelSlug) }
                var testStatus by remember { mutableStateOf<String?>(null) }

                var modelExpanded by remember { mutableStateOf(false) }

                Text("Connection Name", style = MaterialTheme.typography.labelSmall)
                CompactTextField(name, onValueChange = { name = it }, placeholder = "Connection name")

                Text("API URL", style = MaterialTheme.typography.labelSmall)
                CompactTextField(url, onValueChange = { url = it }, placeholder = "https://api.deepseek.com/v1")

                Text("API Token", style = MaterialTheme.typography.labelSmall)
                CompactTextField(token, onValueChange = { token = it }, placeholder = "sk-...")

                Text("User-Agent", style = MaterialTheme.typography.labelSmall)
                CompactTextField(userAgent, onValueChange = { userAgent = it }, placeholder = "StoryForge/1.0")


                Text("Model Name", style = MaterialTheme.typography.labelSmall)
                CompactTextField(modelName, onValueChange = { modelName = it }, placeholder = "gpt-4o")

                Text("Model Slug (used in API payload)", style = MaterialTheme.typography.labelSmall)
                CompactTextField(modelSlug, onValueChange = { modelSlug = it }, placeholder = "deepseek-coder-v3")

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Function Calling")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = functionEnabled,
                        onCheckedChange = { functionEnabled = it }
                    )
                }

                val testable = url.isNotBlank() && token.isNotBlank()

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Button(
                        onClick = {
                            val updated = conn.copy(
                                displayName = name,
                                apiUrl = url,
                                apiToken = token,
                                userAgent = userAgent,
                                functionCallingEnabled = functionEnabled,
                                modelName = modelName,
                                modelSlug = modelSlug,
                                lastUpdated = System.currentTimeMillis()
                            )

                            val updatedList = (settings.aiConnections
                                .filterNot { it.id == updated.id } + updated)
                                .sortedByDescending { it.lastUpdated }

                            val updatedSettings = settings.copy(aiConnections = updatedList)

                            SettingsManager.update(context, updatedSettings)
                            settings = updatedSettings
                            selectedConnection = updated
                            editingConnection = null

                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }

                    Button(
                        onClick = { editingConnection = null },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val testConn = conn.copy(apiUrl = url.trim(), apiToken = token.trim())
                                testStatus = "⏳ Testing..."
                                val ok = testConnection(testConn)

                                testStatus = if (ok) "✅ Success" else "❌ Failed"
                            }

                        },
                        enabled = testable,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test")
                    }
                }
                if (testStatus != null) {
                    Text(
                        text = "Test Status: $testStatus",
                        color = when (testStatus) {
                            "✅ Success" -> MaterialTheme.colorScheme.primary
                            "❌ Failed" -> MaterialTheme.colorScheme.error
                            "⏳ Testing..." -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
