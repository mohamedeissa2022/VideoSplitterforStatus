package com.mohadev.videosplitterforstatus.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    outputDirPath: String,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current

    // 1. Scan the folder for the generated MP4 files
    val generatedFiles = remember(outputDirPath) {
        val dir = File(outputDirPath)
        if (dir.exists()) {
            dir.listFiles { file -> file.extension == "mp4" }
                ?.sortedBy { it.name }
                ?: emptyList()
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Split Complete!", fontWeight = FontWeight.Bold) })
        },
        bottomBar = {
            Button(
                onClick = onNavigateHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
            ) {
                Text("Split Another Video")
            }
        }
    ) { paddingValues ->
        if (generatedFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No files found in directory.")
            }
        } else {
            // 2. Display the files in a list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(generatedFiles) { file ->
                    FileItemCard(file, context)
                }
            }
        }
    }
}

@Composable
fun FileItemCard(file: File, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${(file.length() / (1024 * 1024))} MB", style = MaterialTheme.typography.bodySmall)
            }

            Row {
                // Share Button
                IconButton(onClick = { shareVideo(context, file) }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                // Play Button (Requires external player for now)
                IconButton(onClick = { /* TODO: Launch Video Player Intent */ }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                }
            }
        }
    }
}

// Helper function to share the video via Android Native Share Sheet
fun shareVideo(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Video via"))
}