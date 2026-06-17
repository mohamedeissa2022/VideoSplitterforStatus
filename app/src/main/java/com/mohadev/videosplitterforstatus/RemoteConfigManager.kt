package com.mohadev.videosplitterforstatus

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor() {

    private val remoteConfig: FirebaseRemoteConfig? by lazy { 
        try {
            Firebase.remoteConfig
        } catch (e: Exception) {
            Log.e("RemoteConfig", "Firebase not initialized or missing google-services.json")
            null
        }
    }

    init {
        setupDefaults()
    }

    private fun setupDefaults() {
        try {
            val config = remoteConfig ?: return
            
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600 
            }
            config.setConfigSettingsAsync(configSettings)
            
            val defaults = mapOf(
                "app_update_version" to 1L,
                "app_update_desc" to "A new version of Video Splitter is available with better performance and fixes.",
                "app_update_link" to "https://play.google.com/store/apps/details?id=com.mohadev.videosplitterforstatus",
                "force_update" to false
            )
            config.setDefaultsAsync(defaults)
        } catch (e: Exception) {
            Log.e("RemoteConfig", "Error setting defaults", e)
        }
    }

    fun fetchAndActivate(onComplete: () -> Unit = {}) {
        val config = remoteConfig
        if (config == null) {
            onComplete()
            return
        }
        
        try {
            config.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("RemoteConfig", "Config params updated")
                    } else {
                        Log.e("RemoteConfig", "Config fetch failed")
                    }
                    onComplete()
                }
        } catch (e: Exception) {
            onComplete()
        }
    }

    fun getUpdateVersion(): Long = try { remoteConfig?.getLong("app_update_version") ?: 0L } catch(e: Exception) { 0L }
    fun getUpdateDesc(): String = try { remoteConfig?.getString("app_update_desc") ?: "" } catch(e: Exception) { "" }
    fun getUpdateLink(): String = try { remoteConfig?.getString("app_update_link") ?: "" } catch(e: Exception) { "" }
    fun isUpdateForced(): Boolean = try { remoteConfig?.getBoolean("force_update") ?: false } catch(e: Exception) { false }
}
