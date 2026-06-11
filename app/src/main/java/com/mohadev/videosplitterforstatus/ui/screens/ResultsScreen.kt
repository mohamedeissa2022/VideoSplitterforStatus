package com.mohadev.videosplitterforstatus.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.shadow
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
import com.mohadev.videosplitterforstatus.VideoItem

import com.mohadev.videosplitterforstatus.domain.loadVideosFromDir
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    outputDirPath: String,
    context: Context = LocalContext.current,
    onNavigateHome: () -> Unit
) {
    val videos = remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showPlayerDialog by remember { mutableStateOf<VideoItem?>(null) }

    LaunchedEffect(outputDirPath) {
        val loadedVideos = loadVideosFromDir(outputDirPath, context)
        videos.value = loadedVideos
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
                },
                actions = {
                    TextButton(onClick = {
                        if (selectedIndices.size == videos.value.size) {
                            selectedIndices.clear()
                        } else {
                            selectedIndices.clear()
                            selectedIndices.addAll(videos.value.indices)
                        }
                    }) {
                        Text(
                            if (selectedIndices.size == videos.value.size) 
                                stringResource(R.string.deselect_all) 
                            else 
                                stringResource(R.string.select_all),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                if (selectedIndices.isNotEmpty()) {
                    val selectedVideos = remember(selectedIndices, videos.value) {
                        selectedIndices.sorted().map { videos.value[it] }
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { shareMultipleVideos(context, selectedVideos) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.share_selected, selectedIndices.size),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Social Media Quick Share Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SocialShareButton(
                                icon = Icons.Default.Share,
                                label = "WhatsApp",
                                color = Color(0xFF25D366),
                                modifier = Modifier.weight(1f),
                                onClick = { shareToSocial(context, selectedVideos, "com.whatsapp") }
                            )
                            SocialShareButton(
                                icon = Icons.Default.Share,
                                label = "Instagram",
                                color = Color(0xFFE4405F),
                                modifier = Modifier.weight(1f),
                                onClick = { shareToSocial(context, selectedVideos, "com.instagram.android") }
                            )
                            SocialShareButton(
                                icon = Icons.Default.Share,
                                label = "Facebook",
                                color = Color(0xFF1877F2),
                                modifier = Modifier.weight(1f),
                                onClick = { shareToSocial(context, selectedVideos, "com.facebook.katana") }
                            )
                        }
                    }
                }
                //BannerAdView()
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.split_videos_label),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            if (videos.value.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_files_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(videos.value) { index, video ->
                        VideoItemCard(
                            video = video,
                            isSelected = selectedIndices.contains(index),
                            onToggleSelection = {
                                if (selectedIndices.contains(index)) {
                                    selectedIndices.remove(index)
                                } else {
                                    selectedIndices.add(index)
                                }
                            },
                            onPreview = { showPlayerDialog = video }
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
                
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    //NativeAdViewComposable()
                }
            }
        }
    }

    showPlayerDialog?.let { video ->
        VideoPlayerDialog(video = video, onDismiss = { showPlayerDialog = null })
    }
}

@Composable
fun VideoItemCard(
    video: VideoItem,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onPreview: () -> Unit
) {
    val context = LocalContext.current
    val fileSize = remember(video.uri) {
        try {
            File(video.uri.path ?: "").length()
        } catch (e: Exception) { 0L }
    }

    Card(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .shadow(if (isSelected) 16.dp else 4.dp, RoundedCornerShape(24.dp)),
        onClick = onPreview,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.uri)
                        .decoderFactory { result, options, _ ->
                            VideoFrameDecoder(result.source, options)
                        }
                        .videoFrameMillis(1000)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Play Icon Overlay
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = video.name,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = Formatter.formatFileSize(context, fileSize),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { shareVideo(context, File(video.uri.path ?: "")) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        Icons.Default.Share, 
                        contentDescription = stringResource(R.string.share_content_description),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
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
        onDispose {
            exoPlayer.release()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f/16f),
            color = Color.Black
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun SocialShareButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
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
        if (uris.size > 1) {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        } else {
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
        setPackage(packageName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        context.startActivity(Intent.createChooser(intent, "Share to $packageName"))
    }
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
