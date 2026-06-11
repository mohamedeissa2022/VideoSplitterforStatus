package com.mohadev.videosplitterforstatus

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor() {

    private val remoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 1200 // 20 Mintus
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        val defaults = mapOf(
            "app_update_version" to 1L,
            "app_update_desc" to "A new version of Video Splitter is available with better performance and fixes.",
            "app_update_link" to "https://play.google.com/store/apps/details?id=com.mohadev.videosplitterforstatus",
            "force_update" to false
        )
        remoteConfig.setDefaultsAsync(defaults)
    }

    fun fetchAndActivate(onComplete: () -> Unit = {}) {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("RemoteConfig", "Config params updated")
                } else {
                    Log.e("RemoteConfig", "Config fetch failed")
                }
                onComplete()
            }
    }

    fun getBannerId(): String = remoteConfig.getString("ad_banner_id")
    fun getInterstitialId(): String = remoteConfig.getString("ad_interstitial_id")
    fun getRewardedId(): String = remoteConfig.getString("ad_rewarded_id")
    fun getAppOpenId(): String = remoteConfig.getString("ad_app_open_id")
    fun getNativeId(): String = remoteConfig.getString("ad_native_id")
    fun  getAllKeysValues(): MutableMap<String, String>{
        val maps=mutableMapOf<String, String>()
        remoteConfig.all.forEach { key, value -> maps[key] = value.asString() }
        return  maps
    }

    fun getUpdateVersion(): Long = remoteConfig.getLong("app_update_version")
    fun getUpdateDesc(): String = remoteConfig.getString("app_update_desc")
    fun getUpdateLink(): String = remoteConfig.getString("app_update_link")
    fun isUpdateForced(): Boolean = remoteConfig.getBoolean("force_update")
}