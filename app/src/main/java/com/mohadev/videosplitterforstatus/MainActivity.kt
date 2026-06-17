package com.mohadev.videosplitterforstatus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mohadev.videosplitterforstatus.ui.screens.*
import com.mohadev.videosplitterforstatus.ui.theme.VideoSplitterForStatusTheme
import com.mohadev.videosplitterforstatus.ui.viewmodel.HomeViewModel
import com.mohadev.videosplitterforstatus.ui.viewmodel.HistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject lateinit var remoteConfig: RemoteConfigManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            VideoSplitterForStatusTheme {
                val navController = rememberNavController()
                val homeViewModel: HomeViewModel = hiltViewModel()

                var selectedBrandingList by remember { mutableStateOf<List<VideoBranding>>(emptyList()) }
                var selectedDuration by remember { mutableIntStateOf(30) }
                
                LaunchedEffect(intent) {
                    intent.getStringExtra("TARGET_ROUTE")?.let { route ->
                        navController.navigate(route)
                    }
                }
                
                var showUpdateDialog by remember { mutableStateOf(false) }
                var showRateDialog by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    try {
                        val currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
                        }
                        val _updateVersion = remoteConfig.getUpdateVersion()
                        if (_updateVersion > currentVersion) {
                            showUpdateDialog = true
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Update check failed", e)
                    }
                }

                NavHost(navController = navController, startDestination = "picker") {
                    composable("picker") {
                        VideoPickerScreen(
                            onVideosSelected = { brandingList ->
                                selectedBrandingList = brandingList
                                navController.navigate("home")
                            },
                            context = this@MainActivity,
                            onNavigateToHistory = { navController.navigate("history") }
                        )
                    }

                    composable("home") {
                        HomeScreen(
                            onPresetSelected = { preset ->
                                selectedDuration = preset.durationSeconds
                                homeViewModel.startBatchSplitWithBranding(selectedBrandingList, selectedDuration)
                                navController.navigate("processing")
                            },
                            onCustomSplitSelected = { seconds ->
                                selectedDuration = seconds
                                homeViewModel.startBatchSplitWithBranding(selectedBrandingList, selectedDuration)
                                navController.navigate("processing")
                            },
                            onNavigateToSettings = { /* TODO */ },
                            onNavigateToHistory = { navController.navigate("history") }
                        )
                    }

                    composable("processing") {
                        ProcessingScreen(
                            viewModel = homeViewModel,
                            onProcessComplete = { outputDirPath ->
                                val encodedPath = Uri.encode(outputDirPath)
                                showRateDialog = true
                                navController.navigate("results/$encodedPath") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onProcessCancelled = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("results/{outputDirPath}") { backStackEntry ->
                        val path = Uri.decode(backStackEntry.arguments?.getString("outputDirPath") ?: "")
                        ResultsScreen(
                            outputDirPath = path,
                            onNavigateHome = {
                                navController.navigate("picker") {
                                    popUpTo("picker") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("history") {
                        HistoryScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                if (showUpdateDialog) {
                    UpdateDialog(
                        desc = remoteConfig.getUpdateDesc(),
                        link = remoteConfig.getUpdateLink(),
                        isForced = remoteConfig.isUpdateForced(),
                        onDismiss = { showUpdateDialog = false }
                    )
                }

                if (showRateDialog) {
                    RateAppDialog(onDismiss = { showRateDialog = false })
                }
            }
        }
    }
}

@Composable
fun UpdateDialog(desc: String, link: String, isForced: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = { if (!isForced) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !isForced, dismissOnClickOutside = !isForced)
    ) {
        Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.update_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text(desc, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(32.dp))
                Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(stringResource(R.string.update_button), fontWeight = FontWeight.Bold)
                }
                if (!isForced) {
                    TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.update_later))
                    }
                }
            }
        }
    }
}

@Composable
fun RateAppDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rate_title), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.rate_message)) },
        confirmButton = {
            Button(onClick = {
                val packageName = context.packageName
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                } catch (e: Exception) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                }
                onDismiss()
            }) {
                Text(stringResource(R.string.rate_now), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.remind_later)) }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
