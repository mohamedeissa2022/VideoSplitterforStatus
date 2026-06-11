package com.mohadev.videosplitterforstatus.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.mohadev.videosplitterforstatus.R
import com.mohadev.videosplitterforstatus.domain.VideoCompressor
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPickerScreen(
    onVideoSelected: (Uri) -> Unit,
    context: Context,
    onNavigateToHistory: () -> Unit = {}
) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileSize by remember { mutableLongStateOf(0L) }
    var showQualitySelectionDialog by remember { mutableStateOf(false) }
    var showProcessingDialog by remember { mutableStateOf(false) }
    var compressionQuality by remember { mutableStateOf("800k") }
    
    // Results after compression
    var compressedResult by remember { mutableStateOf<File?>(null) }
    

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    selectedUri = uri
                    fileSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { 
                        it.length 
                    } ?: 0L
                } catch (e: Exception) {
                    android.util.Log.e("VideoPickerScreen", "Failed to process URI", e)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.select_video_title), 
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
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
//        bottomBar = {
//            Box(modifier = Modifier.padding(bottom = 8.dp)) {
//                BannerAdView()
//            }
//        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedUri == null) {
                Spacer(modifier = Modifier.weight(0.5f))
                
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = stringResource(R.string.choose_video_instruction),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.supports_formats),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        stringResource(R.string.open_gallery), 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .shadow(20.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(selectedUri)
                                .decoderFactory { result, options, _ ->
                                    VideoFrameDecoder(result.source, options)
                                }
                                .videoFrameMillis(1000)
                                .build(),
                            contentDescription = "Video Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = Formatter.formatFileSize(context, fileSize),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow, 
                                    contentDescription = null, 
                                    modifier = Modifier.padding(16.dp).size(40.dp), 
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onVideoSelected(selectedUri!!) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.split_original), 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            showQualitySelectionDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 2.dp)
                    ) {
                        Icon(Icons.Default.Compress, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.compress_split), 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    TextButton(
                        onClick = { selectedUri = null },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            stringResource(R.string.pick_another),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // 1. New Quality Selection Dialog
    if (showQualitySelectionDialog) {
        AlertDialog(
            onDismissRequest = { showQualitySelectionDialog = false },
            title = { Text(stringResource(R.string.compression_quality), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    QualitySelectionItem(
                        title = stringResource(R.string.quality_low),
                        isSelected = compressionQuality == "250k",
                        onClick = { compressionQuality = "250k" }
                    )
                    QualitySelectionItem(
                        title = stringResource(R.string.quality_medium),
                        isSelected = compressionQuality == "500k",
                        onClick = { compressionQuality = "500k" }
                    )
                    QualitySelectionItem(
                        title = stringResource(R.string.quality_high),
                        isSelected = compressionQuality == "1000k",
                        onClick = { compressionQuality = "1000k" }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showQualitySelectionDialog = false
                    showProcessingDialog = true
//                    if (rewardedAdManager.isAdLoaded()) {
//                        rewardedAdManager.showAd(context as Activity) {
//                            showProcessingDialog = true
//                        }
//                    } else {
//                        showProcessingDialog = true
//                        rewardedAdManager.loadAd(context as Activity)
//                    }
                }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQualitySelectionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    // 2. Processing & Results Dialog
    if (showProcessingDialog) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss */ },
            title = { 
                Text(
                    if (compressedResult == null) stringResource(R.string.compressing_video_title) 
                    else "Compression Results",
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                if (compressedResult == null) {
                    LaunchedEffect(Unit) {
                        val result = VideoCompressor.compressVideo(context, selectedUri.toString(), compressionQuality) {}
                        compressedResult = result
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
                        Spacer(Modifier.height(24.dp))
                        Text(stringResource(R.string.processing_wait), textAlign = TextAlign.Center)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val originalMB = fileSize / (1024f * 1024f)
                        val compressedMB = compressedResult!!.length() / (1024f * 1024f)
                        val reduction = if (fileSize > 0) ((fileSize - compressedResult!!.length()).toFloat() / fileSize * 100).toInt() else 0
                        val isBetter = compressedResult!!.length() < fileSize
                        
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Original Size:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("%.2f MB".format(originalMB), fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Compressed Size:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("%.2f MB".format(compressedMB), fontWeight = FontWeight.Bold, color = if (isBetter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = if (isBetter) "Total Space Saved: $reduction%" else "Already Optimized!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isBetter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                if (compressedResult != null) {
                    Button(onClick = {
                        onVideoSelected(Uri.fromFile(compressedResult!!))
                        showProcessingDialog = false
                        compressedResult = null
                    }) {
                        Text("Continue to Split", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showProcessingDialog = false 
                    compressedResult = null
                }) { 
                    Text(if (compressedResult == null) stringResource(R.string.cancel) else "Close") 
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
fun QualitySelectionItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
            RadioButton(selected = isSelected, onClick = onClick)
        }
    }
}
