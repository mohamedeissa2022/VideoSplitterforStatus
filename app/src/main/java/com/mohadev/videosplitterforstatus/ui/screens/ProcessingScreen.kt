package com.mohadev.videosplitterforstatus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import com.mohadev.videosplitterforstatus.R

@Composable
fun ProcessingScreen(
    viewModel: HomeViewModel,
    onProcessComplete: (String) -> Unit,
    onProcessCancelled: () -> Unit
) {
    // 🛠️ Observing Dynamic Queue
    val workList by viewModel.getAllActiveWorkFlow().collectAsStateWithLifecycle(initialValue = emptyList())

    val isAllFinished = workList.isNotEmpty() && workList.all { it.state.isFinished }
    val isAnyFailed = workList.any { it.state == WorkInfo.State.FAILED || it.state == WorkInfo.State.CANCELLED }

    var navigated by remember { mutableStateOf(false) }

    LaunchedEffect(isAllFinished, isAnyFailed) {
        if (isAllFinished && !navigated) {
            navigated = true
            // In dynamic mode, we use the output of the LAST worker
            val lastOutput = workList.lastOrNull { it.state == WorkInfo.State.SUCCEEDED }?.outputData?.getString("OUTPUT_DIR")
            onProcessComplete(lastOutput ?: "")
        } else if (isAnyFailed && workList.isEmpty() && !navigated) {
            navigated = true
            onProcessCancelled()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp)
        ) {
            Text("Dynamic Production Monitor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("Real-time background production queue", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Live Queue (${workList.size} Tasks)", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(workList) { workInfo ->
                            DynamicTaskItem(workInfo)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { viewModel.cancelAllWork() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DynamicTaskItem(workInfo: WorkInfo) {
    val progress = workInfo.progress.getInt("PROGRESS", 0)
    val videoName = workInfo.progress.getString("VIDEO_NAME") ?: "Pending Video..."
    val state = workInfo.state
    
    val isActive = state == WorkInfo.State.RUNNING
    val isCompleted = state == WorkInfo.State.SUCCEEDED
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Pending,
            contentDescription = null,
            tint = if (isCompleted) Color(0xFF10B981) else if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = videoName, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium)
            
            if (isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
        
        if (isActive) {
            Text(text = "$progress%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        } else if (isCompleted) {
            Text("Done", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981))
        }
    }
}
