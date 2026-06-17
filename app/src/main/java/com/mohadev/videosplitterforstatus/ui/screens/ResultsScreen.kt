package com.mohadev.videosplitterforstatus.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.mohadev.videosplitterforstatus.VideoItem
import com.mohadev.videosplitterforstatus.domain.loadVideosFromDir
import java.io.File

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ResultsScreen(
//    outputDirPath: String,
//    onNavigateHome: () -> Unit
//) {
//    val context = LocalContext.current
//
//    // 1. Scan the folder for the generated MP4 files
//    val generatedFiles = remember(outputDirPath) {
//        val dir = File(outputDirPath)
//        if (dir.exists()) {
//            dir.listFiles { file -> file.extension == "mp4" }
//                ?.sortedBy { it.name }
//                ?: emptyList()
//        } else {
//            emptyList()
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(title = { Text("Split Complete!", fontWeight = FontWeight.Bold) })
//        },
//        bottomBar = {
//            Button(
//                onClick = onNavigateHome,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//                    .height(56.dp)
//            ) {
//                Text("Split Another Video")
//            }
//        }
//    ) { paddingValues ->
//        if (generatedFiles.isEmpty()) {
//            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                Text("No files found in directory.")
//            }
//        } else {
//            // 2. Display the files in a list
//            LazyColumn(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(paddingValues)
//                    .padding(horizontal = 16.dp),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                items(generatedFiles) { file ->
//                    FileItemCard(file, context)
//                }
//            }
//        }
//    }
//}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    outputDirPath: String,
    context: Context = LocalContext.current,
    onNavigateHome: () -> Unit
) {
    val videos = remember { mutableStateOf<List<VideoItem>>(emptyList()) }

    LaunchedEffect(outputDirPath) {
        videos.value = loadVideosFromDir(outputDirPath, context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split Complete!", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Split Videos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            if (videos.value.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No files found in directory.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f)
                ) {
                    items(videos.value) { video ->
                        VideoItemCard(video)
                    }
                }
            }
        }
    }
}
@Composable
fun VideoItemCard(video: VideoItem) {

    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {

        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = video.name)
            }

            IconButton(onClick = {
                try {
                    val file = File(video.uri.path ?: "")
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/mp4"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                } catch (e: Exception) {
                    android.util.Log.e("ResultsScreen", "Failed to share video", e)
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Video"
                )
            }

            Spacer(Modifier.width(8.dp))

            Button(onClick = {
                try {
                    val file = File(video.uri.path ?: "")
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, "video/mp4")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("ResultsScreen", "Failed to play video", e)
                }
            }) {
                Text("Play")
            }
        }
    }
}