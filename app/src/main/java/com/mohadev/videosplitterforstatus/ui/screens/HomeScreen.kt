package com.mohadev.videosplitterforstatus.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohadev.videosplitterforstatus.R


// 1. Define our Preset Data Model
data class SplitPreset(val platform: String, val durationSeconds: Int, val iconColor: Color)

val presets = listOf(
    SplitPreset("WhatsApp", 30, Color(0xFF25D366)),
    SplitPreset("Instagram", 60, Color(0xFFE4405F)),
    SplitPreset("Facebook", 60, Color(0xFF1877F2)),
    SplitPreset("Snapchat", 60, Color(0xFFFFFC00))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPresetSelected: (SplitPreset) -> Unit,
    onCustomSplitSelected: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit
)
{
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.home_title), 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ) 
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.History, 
                            contentDescription = stringResource(R.string.history_content_description),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings, 
                            contentDescription = stringResource(R.string.settings_content_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
//        bottomBar = {
//            Box(modifier = Modifier.padding(bottom = 8.dp)) {
//
//            }
//        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                stringResource(R.string.quick_split), 
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Custom Duration Section
            Text(
                stringResource(R.string.custom_split), 
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            CustomSplitCard(onCustomSplitSelected)
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PresetCard(preset: SplitPreset, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(preset.iconColor)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                preset.platform, 
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.seconds_unit, preset.durationSeconds), 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CustomSplitCard(onCustomSplitSelected: (Int) -> Unit) {
    var customSeconds by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = customSeconds,
                onValueChange = { if (it.length <= 3) customSeconds = it },
                label = { Text(stringResource(R.string.seconds_label)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            
            Button(
                onClick = {
                    val seconds = customSeconds.toIntOrNull() ?: 0
                    if (seconds > 0) onCustomSplitSelected(seconds)
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Text(
                    stringResource(R.string.split_button),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
