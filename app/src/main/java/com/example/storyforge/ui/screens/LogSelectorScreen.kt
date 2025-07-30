package com.example.storyforge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.storyforge.logging.LogViewMode

@Composable
fun LogSelectorScreen(
    selectedViews: List<LogViewMode>,
    onSelectionChanged: (List<LogViewMode>) -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val allModes = LogViewMode.values().toList()
    val selected = remember { mutableStateListOf(*selectedViews.toTypedArray()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Select Log Views", style = MaterialTheme.typography.headlineSmall)

        for (mode in allModes) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = selected.contains(mode),
                    onCheckedChange = { checked ->
                        if (checked) selected.add(mode) else selected.remove(mode)
                        onSelectionChanged(selected.toList())
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(mode.name)
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = onNavigateToLogs) {
            Text("Open Log Review")
        }
    }
}
