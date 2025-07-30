package com.example.storyforge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storyforge.StoryForgeViewModel
import com.example.storyforge.logging.LogViewMode
import com.example.storyforge.logging.renderLogEntry
import kotlinx.coroutines.launch

@Composable
fun LogViewerScreen(
    viewModel: StoryForgeViewModel,
    onNavToggle: () -> Unit
) {
    val entries by viewModel.turnLogEntries.collectAsStateWithLifecycle()
    val selectedModes by viewModel.selectedLogViews.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { selectedModes.size })
    val coroutineScope = rememberCoroutineScope()

    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Top Row: Header + Menu Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val currentTurn = entries.getOrNull(pagerState.currentPage)?.turnNumber ?: pagerState.currentPage
            Text("Log Review", style = MaterialTheme.typography.headlineSmall)


            TextButton(onClick = onNavToggle) { Text("Menu") }
        }

        Spacer(Modifier.height(12.dp))

        // Dropdown for selecting views
        Box {
            Button(onClick = { dropdownExpanded = true }) {
                Text("Log Views ▼")
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                LogViewMode.values().forEach { mode ->
                    val isChecked = selectedModes.contains(mode)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(mode.name)
                            }
                        },
                        onClick = {
                            val newList = if (isChecked)
                                selectedModes - mode
                            else
                                selectedModes + mode

                            viewModel.setSelectedLogViews(newList)
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Centered mode name
        if (selectedModes.isNotEmpty()) {
            Text(
                text = "←  ${selectedModes[pagerState.currentPage].name}  →",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))
        }

        // Carousel
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val mode = selectedModes.getOrNull(page) ?: return@HorizontalPager

            LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
                items(entries) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        renderLogEntry(mode, entry)
                    }
                }
            }
        }
    }
}
