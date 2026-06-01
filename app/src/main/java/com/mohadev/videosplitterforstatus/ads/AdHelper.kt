package com.mohadev.videosplitterforstatus.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdHelper(private val context: Context) {

    private var mInterstitialAd: InterstitialAd? = null
    private val TAG = "AdHelper"

    // Call this as soon as the app starts (or after a job completes)
    fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            "ca-app-pub-3940256099942544/1033173712", // Replace with your REAL ID
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.toString())
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                }
            }
        )
    }

    // This is the function you call in your MainActivity
    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(activity)

            // Reload for the next time
            mInterstitialAd = null
            loadInterstitialAd()
        } else {
            Log.d(TAG, "The interstitial wasn't ready yet.")
            onAdDismissed() // Skip and go to next screen
        }
    }
}