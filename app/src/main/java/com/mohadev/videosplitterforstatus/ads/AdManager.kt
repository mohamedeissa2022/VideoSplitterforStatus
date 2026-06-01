package com.mohadev.videosplitterforstatus.ads


import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)

    fun shouldShowInterstitial(): Boolean {
        val count = prefs.getInt("split_count", 0) + 1
        val dailyAds = prefs.getInt("daily_ads_count", 0)

        // Rule: Show every 2 jobs, capped at 4 per day
        if (count >= 2 && dailyAds < 4) {
            prefs.edit().putInt("split_count", 0).apply()
            prefs.edit().putInt("daily_ads_count", dailyAds + 1).apply()
            return true
        }

        prefs.edit().putInt("split_count", count).apply()
        return false
    }
}