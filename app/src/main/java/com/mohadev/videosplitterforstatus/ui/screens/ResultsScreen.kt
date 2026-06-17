package com.mohadev.videosplitterforstatus.ui.screens

import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.mohadev.videosplitterforstatus.R
import com.mohadev.videosplitterforstatus.domain.model.VideoItem
import com.mohadev.videosplitterforstatus.data.service.VideoLoader
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    outputDirPath: String,
    context: Context = LocalContext.current,
    onNavigateHome: () -> Unit
) {
    val groupedVideos = remember { mutableStateOf<Map<String, List<VideoItem>>>(emptyMap()) }
    var showPlayerDialog by remember { mutableStateOf<VideoItem?>(null) }

    LaunchedEffect(outputDirPath) {
        groupedVideos.value = VideoLoader.loadVideosGrouped(outputDirPath, context)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.split_complete), 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_content_description)
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (groupedVideos.value.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                val sortedKeys = groupedVideos.value.keys.sortedBy { it.removePrefix("v").toIntOrNull() ?: 0 }
                
                sortedKeys.forEach { videoKey ->
                    val parts = groupedVideos.value[videoKey] ?: emptyList()
                    item {
                        VideoGroupSection(
                            title = "Video ${videoKey.removePrefix("v").toIntOrNull()?.let { it + 1 } ?: videoKey}",
                            parts = parts,
                            onPreview = { showPlayerDialog = it }
                        )
                    }
                }
            }
        }
    }

    showPlayerDialog?.let { video ->
        VideoPlayerDialog(video = video, onDismiss = { showPlayerDialog = null })
    }
}

@Composable
fun VideoGroupSection(
    title: String,
    parts: List<VideoItem>,
    onPreview: (VideoItem) -> Unit
) {
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            
            TextButton(onClick = { shareMultipleVideos(context, parts) }) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share All")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().height(260.dp)
        ) {
            items(parts) { video ->
                VideoItemCardMinimal(
                    video = video,
                    onPreview = { onPreview(video) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun VideoItemCardMinimal(
    video: VideoItem,
    onPreview: () -> Unit
) {
    val context = LocalContext.current
    val fileSize = remember(video.uri) {
        try { File(video.uri.path ?: "").length() } catch (e: Exception) { 0L }
    }

    Card(
        modifier = Modifier.width(180.dp).fillMaxHeight(),
        onClick = onPreview,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.uri)
                        .decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                        .videoFrameMillis(1000)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(shape = CircleShape, color = Color.Black.copy(0.4f), modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(8.dp), tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(video.name, maxLines = 1, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(Formatter.formatFileSize(context, fileSize), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = { shareVideo(context, File(video.uri.path ?: "")) },
                modifier = Modifier.align(Alignment.End),
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.4f))
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun VideoPlayerDialog(video: VideoItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(32.dp), tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth().aspectRatio(9f/16f), color = Color.Black) {
            AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = true } }, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun SocialShareButton(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(50.dp), shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f), contentColor = color, border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

fun shareToSocial(context: Context, videos: List<VideoItem>, packageName: String) {
    val uris = ArrayList(videos.map { video ->
        val file = File(video.uri.path ?: "")
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    })
    val intent = Intent(if (uris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND).apply {
        type = "video/mp4"
        if (uris.size > 1) putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris) else putExtra(Intent.EXTRA_STREAM, uris.first())
        setPackage(packageName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try { context.startActivity(intent) } catch (e: Exception) { context.startActivity(Intent.createChooser(intent, "Share to $packageName")) }
}

fun shareVideo(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
}

fun shareMultipleVideos(context: Context, videos: List<VideoItem>) {
    val uris = ArrayList<android.net.Uri>()
    videos.forEach { video ->
        val file = File(video.uri.path ?: "")
        uris.add(FileProvider.getUriForFile(context, "${context.packageName}.provider", file))
    }
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "video/mp4"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Videos"))
}
