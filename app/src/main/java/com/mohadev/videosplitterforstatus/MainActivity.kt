package com.mohadev.videosplitterforstatus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mohadev.videosplitterforstatus.ui.screens.*
import com.mohadev.videosplitterforstatus.ui.theme.VideoSplitterForStatusTheme
import dagger.hilt.android.AndroidEntryPoint

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Request Notification Permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            VideoSplitterForStatusTheme {
                val navController = rememberNavController()
                val homeViewModel: HomeViewModel = hiltViewModel()

                var selectedVideoPath by remember { mutableStateOf("") }
                var selectedVideoName by remember { mutableStateOf("") }
                var selectedDuration by remember { mutableIntStateOf(30) }
                val remoteConfig: RemoteConfigManager= RemoteConfigManager()
                LaunchedEffect(intent) {
                    intent.getStringExtra("TARGET_ROUTE")?.let { route ->
                        navController.navigate(route)
                    }
                }
                
                var showUpdateDialog by remember { mutableStateOf(false) }
                var showRateDialog by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {

                    // Check for updates
                    val currentVersion = packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
                    val _updateVersion=remoteConfig.getUpdateVersion()
                    Log.d("RemoteConfig_App_Update:","CurrVersion:$currentVersion\tUpdate Version:$_updateVersion")
                    if (_updateVersion > currentVersion) {
                        showUpdateDialog = true
                    }
                }

                NavHost(navController = navController, startDestination = "picker") {
                    composable("picker") {
                        VideoPickerScreen(
                            onVideoSelected = { uri ->
                                try {
                                    contentResolver.takePersistableUriPermission(
                                        uri,
                                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to take permission", e)
                                }
                                selectedVideoPath = uri.toString()
                                selectedVideoName = uri.lastPathSegment ?: "Split Video"
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
                                homeViewModel.startVideoSplit(selectedVideoPath, selectedVideoName, selectedDuration)
                                navController.navigate("processing")
                            },
                            onCustomSplitSelected = { seconds ->
                                selectedDuration = seconds
                                homeViewModel.startVideoSplit(selectedVideoPath, selectedVideoName, selectedDuration)
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
                    RateAppDialog(
                        onDismiss = { showRateDialog = false }
                    )
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
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.update_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
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
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                }
                onDismiss()
            }) {
                Text(stringResource(R.string.rate_now), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.remind_later))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
