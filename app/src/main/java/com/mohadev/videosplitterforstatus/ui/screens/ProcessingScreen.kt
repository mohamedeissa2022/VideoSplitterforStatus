package com.mohadev.videosplitterforstatus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo

@Composable
fun ProcessingScreen(
    viewModel: HomeViewModel,
    onProcessComplete: (String) -> Unit, // Pass the output folder path
    onProcessCancelled: () -> Unit
) {
    // 1. Observe the WorkManager progress
    val workInfo by viewModel.getWorkInfoFlow().collectAsStateWithLifecycle(initialValue = null)

    // 2. Extract current state and progress
    val progress = workInfo?.progress?.getInt("PROGRESS", 0) ?: 0
    val state = workInfo?.state

    // 3. Navigate away when finished
    LaunchedEffect(state) {
        if (state == WorkInfo.State.SUCCEEDED) {
            val outputDir = workInfo?.outputData?.getString("OUTPUT_DIR") ?: ""
            onProcessComplete(outputDir)
        } else if (state == WorkInfo.State.CANCELLED) {
            onProcessCancelled()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Splitting Video...",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. The Progress Bar
            CircularProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.size(120.dp),
                strokeWidth = 8.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "$progress%",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Please keep the app open",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 5. Cancel Button
            OutlinedButton(
                onClick = {
                    viewModel.cancelCurrentWork()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}