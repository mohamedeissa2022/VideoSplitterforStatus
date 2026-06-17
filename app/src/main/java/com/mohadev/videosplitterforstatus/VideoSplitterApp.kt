package com.mohadev.videosplitterforstatus

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VideoSplitterApp : Application() {

    @Inject lateinit var remoteConfig: RemoteConfigManager

    override fun onCreate() {
        super.onCreate()
        
        // Enable Crashlytics for production debugging
        try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            FirebaseCrashlytics.getInstance().setCustomKey("app_version", "Pro_1.0")
        } catch (e: Exception) {
            // Firebase might fail if json is missing, but app should continue
        }
        
        remoteConfig.fetchAndActivate()
    }
}
