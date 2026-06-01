package com.mohadev.videosplitterforstatus

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.activity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mohadev.videosplitterforstatus.ads.AdHelper
import com.mohadev.videosplitterforstatus.ads.AdManager
import com.mohadev.videosplitterforstatus.ui.screens.*
import dagger.hilt.android.AndroidEntryPoint

// Make sure you have @AndroidEntryPoint to enable Hilt DI
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Setup Jetpack Compose Navigation
            val navController = rememberNavController()
            val homeViewModel: HomeViewModel = hiltViewModel()

            // State to hold the picked video path before we start processing
            var selectedVideoPath by remember { mutableStateOf("") }
            var selectedDuration by remember { mutableIntStateOf(30) }
            val adManager = remember { AdManager(this) }
            val adHelper = remember { AdHelper(this) }
            NavHost(navController = navController, startDestination = "picker") {

                // 1. Picker Screen
                composable("picker") {
                    VideoPickerScreen(
                        onVideoSelected = { uri ->
                            // Persist access to the URI for background processing
                            try {
                                contentResolver.takePersistableUriPermission(
                                    uri,
                                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Failed to take persistable URI permission", e)
                            }
                            // Pass the URI to navigation
                            selectedVideoPath = uri.toString()
                            navController.navigate("home")
                        }
                    ,this@MainActivity)
                }

                // 2. Home Screen (Select Duration)
                composable("home") {
                    HomeScreen(
                        onPresetSelected = { preset ->
                            selectedDuration = preset.durationSeconds
                            homeViewModel.startVideoSplit(selectedVideoPath, "MyVideo", selectedDuration)
                            navController.navigate("processing")
                        },
                        onCustomSplitSelected = { seconds ->
                            selectedDuration = seconds
                            homeViewModel.startVideoSplit(selectedVideoPath, "MyVideo", selectedDuration)
                            navController.navigate("processing")
                        },
                        onNavigateToSettings = { /* TODO */ }
                    )
                }

                // 3. Processing Screen (Loading)
                composable("processing") {
                    ProcessingScreen(
                        viewModel = homeViewModel,
                        onProcessComplete = { outputDirPath ->
                            // 1. Check if we should show an ad
                            if (adManager.shouldShowInterstitial()) {
                                // Logic to trigger AdMob Interstitial
                                adHelper.showInterstitial(this@MainActivity) { OnDesmisedAd() }
                            }else{
                                // 2. Then proceed to Results
                                navController.navigate("results/$outputDirPath") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }


                        },
                        onProcessCancelled = {
                            navController.popBackStack()
                        }
                    )
                }

                // 4. Results Screen
                composable("results/{outputDirPath}") { backStackEntry ->
                    val path = backStackEntry.arguments?.getString("outputDirPath") ?: ""
                    ResultsScreen(
                        outputDirPath = path,
                        onNavigateHome = {
                            navController.navigate("picker") {
                                popUpTo("picker") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
fun OnDesmisedAd(): Unit
{
    Log.d("Ad Interstital","Koko")

}