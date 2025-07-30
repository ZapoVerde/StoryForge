package com.example.storyforge.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.storyforge.StoryForgeViewModel
import com.example.storyforge.model.GameStateSlotStorage
import java.io.File

@Composable
fun SaveManagerScreen(
    viewModel: StoryForgeViewModel,
    navController: NavController,
    onNavToggle: () -> Unit
) {
    val context = viewModel.appContext
    val saveFiles by remember { mutableStateOf(GameStateSlotStorage.listSaves(context)) }

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
            Text("Saved Games", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onNavToggle) { Text("Menu") }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(saveFiles) { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val snapshot = GameStateSlotStorage.loadSnapshot(file)
                            if (snapshot != null) {
                                viewModel.loadSnapshot(snapshot)
                                navController.navigate("narrator")
                            }
                        }
                    ,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(file.nameWithoutExtension, style = MaterialTheme.typography.titleMedium)
                        Text("Tap to load", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
