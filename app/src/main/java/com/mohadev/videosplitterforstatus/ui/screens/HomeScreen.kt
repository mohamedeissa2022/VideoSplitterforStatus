package com.mohadev.videosplitterforstatus.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// 1. Define our Preset Data Model
data class SplitPreset(val platform: String, val durationSeconds: Int)

val presets = listOf(
    SplitPreset("WhatsApp", 30),
    SplitPreset("Instagram", 60),
    SplitPreset("Facebook", 60),
    SplitPreset("Snapchat", 60)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPresetSelected: (SplitPreset) -> Unit,
    onCustomSplitSelected: (Int) -> Unit,
    onNavigateToSettings: () -> Unit
)
{
    Scaffold(

        topBar = {
            TopAppBar(
                title = { Text("Video Splitter", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Quick Split", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Grid for Platform Presets
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(presets) { preset ->
                    PresetCard(preset, onClick = { onPresetSelected(preset) })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Custom Duration Section
            Text("Custom Split", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            CustomSplitCard(onCustomSplitSelected)
        }
    }


}

@Composable
fun PresetCard(preset: SplitPreset, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(preset.platform, fontWeight = FontWeight.Bold)
            Text("${preset.durationSeconds} Seconds", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun CustomSplitCard(onCustomSplitSelected: (Int) -> Unit) {
    var customSeconds by remember { mutableStateOf("") }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = customSeconds,
                onValueChange = { customSeconds = it },
                label = { Text("Seconds") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(
                onClick = {
                    val seconds = customSeconds.toIntOrNull() ?: 0
                    if (seconds > 0) onCustomSplitSelected(seconds)
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Split")
            }
        }
    }
}