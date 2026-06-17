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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.mohadev.videosplitterforstatus.ui.viewmodel.HomeViewModel
import java.io.File

data class VideoBranding(
    val uri: Uri,
    var logoUri: Uri? = null,
    var text: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPickerScreen(
    onVideosSelected: (List<VideoBranding>) -> Unit,
    context: Context,
    onNavigateToHistory: () -> Unit = {}
) {
    var selectedBrandingList by remember { mutableStateOf<List<VideoBranding>>(emptyList()) }
    var focusedVideoIndex by remember { mutableIntStateOf(0) }
    
    var showQualitySelectionDialog by remember { mutableStateOf(false) }
    var showProcessingDialog by remember { mutableStateOf(false) }
    var compressionQuality by remember { mutableStateOf("500k") }
    
    // UI Helpers
    var applyToAll by remember { mutableStateOf(false) }
    var compressedResults by remember { mutableStateOf<List<File>>(emptyList()) }
    
    val homeViewModel: HomeViewModel = hiltViewModel()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                selectedBrandingList = uris.map { uri ->
                    try {
                        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (e: Exception) {}
                    VideoBranding(uri)
                }
                focusedVideoIndex = 0
            }
        }
    )

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> 
            if (uri != null) {
                if (applyToAll) {
                    selectedBrandingList = selectedBrandingList.map { it.copy(logoUri = uri) }
                } else {
                    selectedBrandingList = selectedBrandingList.toMutableList().also {
                        it[focusedVideoIndex] = it[focusedVideoIndex].copy(logoUri = uri)
                    }
                }
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.select_video_title), fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedBrandingList.isEmpty()) {
                Spacer(modifier = Modifier.weight(0.5f))
                Box(modifier = Modifier.size(160.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.height(40.dp))
                Text(text = stringResource(R.string.choose_video_instruction), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(48.dp))
                Button(onClick = { mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }, modifier = Modifier.fillMaxWidth().height(64.dp).shadow(12.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp)) {
                    Text(stringResource(R.string.open_gallery), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    items(selectedBrandingList.size) { index ->
                        VideoQueueItemSmall(
                            branding = selectedBrandingList[index],
                            isFocused = focusedVideoIndex == index,
                            onClick = { focusedVideoIndex = index },
                            onRemove = {
                                selectedBrandingList = selectedBrandingList.filterIndexed { i, _ -> i != index }
                                if (focusedVideoIndex >= selectedBrandingList.size) focusedVideoIndex = 0.coerceAtLeast(selectedBrandingList.size - 1)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Watermark Settings", fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Apply to all", style = MaterialTheme.typography.labelSmall)
                                Checkbox(checked = applyToAll, onCheckedChange = { applyToAll = it })
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { logoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                val currentLogo = selectedBrandingList[focusedVideoIndex].logoUri
                                Text(if (currentLogo == null) "Add Logo" else "Change Logo")
                            }
                            
                            if (selectedBrandingList[focusedVideoIndex].logoUri != null) {
                                IconButton(onClick = {
                                    val uri = null
                                    if (applyToAll) {
                                        selectedBrandingList = selectedBrandingList.map { it.copy(logoUri = uri) }
                                    } else {
                                        selectedBrandingList = selectedBrandingList.toMutableList().also { it[focusedVideoIndex] = it[focusedVideoIndex].copy(logoUri = uri) }
                                    }
                                }) { Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = selectedBrandingList[focusedVideoIndex].text,
                            onValueChange = { newText ->
                                if (newText.length <= 20) {
                                    if (applyToAll) {
                                        selectedBrandingList = selectedBrandingList.map { it.copy(text = newText) }
                                    } else {
                                        selectedBrandingList = selectedBrandingList.toMutableList().also { it[focusedVideoIndex] = it[focusedVideoIndex].copy(text = newText) }
                                    }
                                }
                            },
                            placeholder = { Text("Enter branding text") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onVideosSelected(selectedBrandingList) }, modifier = Modifier.weight(1f).height(60.dp), shape = RoundedCornerShape(16.dp)) {
                        Text("Split Original", fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = { showQualitySelectionDialog = true },
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Compress, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Compress & Split", fontWeight = FontWeight.Bold)
                    }
                }
                
                TextButton(onClick = { selectedBrandingList = emptyList() }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showQualitySelectionDialog) {
        AlertDialog(
            onDismissRequest = { showQualitySelectionDialog = false },
            title = { Text(stringResource(R.string.compression_quality), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    QualitySelectionItem(title = "Low (Smallest)", isSelected = compressionQuality == "250k", onClick = { compressionQuality = "250k" })
                    QualitySelectionItem(title = "Medium (Standard)", isSelected = compressionQuality == "500k", onClick = { compressionQuality = "500k" })
                    QualitySelectionItem(title = "High (Best)", isSelected = compressionQuality == "1000k", onClick = { compressionQuality = "1000k" })
                }
            },
            confirmButton = {
                Button(onClick = {
                    showQualitySelectionDialog = false
                    showProcessingDialog = true
                }) { Text("Start Processing", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showQualitySelectionDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showProcessingDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(if (compressedResults.size < selectedBrandingList.size) "Compressing Batch" else "Compression Complete", fontWeight = FontWeight.Bold) },
            text = {
                if (compressedResults.size < selectedBrandingList.size) {
                    LaunchedEffect(Unit) {
                        val results = mutableListOf<File>()
                        selectedBrandingList.forEach { branding ->
                            val res = homeViewModel.compressVideoUseCase(context, branding.uri.toString(), compressionQuality)
                            if (res != null) results.add(res)
                        }
                        compressedResults = results
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Processing ${compressedResults.size + 1}/${selectedBrandingList.size}...")
                    }
                } else {
                    Text("Batch compression finished. ${compressedResults.size} videos ready for splitting.")
                }
            },
            confirmButton = {
                if (compressedResults.size == selectedBrandingList.size) {
                    Button(onClick = {
                        val newList = selectedBrandingList.mapIndexed { index, branding ->
                            branding.copy(uri = Uri.fromFile(compressedResults[index]))
                        }
                        onVideosSelected(newList)
                        showProcessingDialog = false
                        compressedResults = emptyList()
                    }) { Text("Continue to Split") }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showProcessingDialog = false
                    compressedResults = emptyList()
                }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
fun VideoQueueItemSmall(branding: VideoBranding, isFocused: Boolean, onClick: () -> Unit, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).border(if (isFocused) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)).clickable { onClick() }) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(branding.uri).decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }.videoFrameMillis(1000).build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (branding.logoUri != null || branding.text.isNotEmpty()) {
            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp).align(Alignment.BottomEnd).padding(2.dp), tint = Color.Green)
        }
        IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).size(20.dp).background(Color.Black.copy(0.5f), CircleShape)) {
            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun QualitySelectionItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
            RadioButton(selected = isSelected, onClick = onClick)
        }
    }
}
