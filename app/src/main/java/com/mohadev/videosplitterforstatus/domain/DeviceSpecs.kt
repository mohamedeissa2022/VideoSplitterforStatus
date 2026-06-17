package com.mohadev.videosplitterforstatus.domain

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log

enum class DeviceProfile {
    LOW_END,   // < 4GB RAM or < 4 Cores
    MID_RANGE, // 4-8GB RAM
    HIGH_END   // > 8GB RAM and >= 8 Cores
}

object DeviceSpecs {

    fun getProfile(context: Context): DeviceProfile {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRamGb = memoryInfo.totalMem / (1024 * 1024 * 1024)
        val cores = Runtime.getRuntime().availableProcessors()
        
        Log.d("DeviceSpecs", "RAM: ${totalRamGb}GB, Cores: $cores")
        
        return when {
            totalRamGb >= 8 && cores >= 8 -> DeviceProfile.HIGH_END
            totalRamGb >= 4 -> DeviceProfile.MID_RANGE
            else -> DeviceProfile.LOW_END
        }
    }

    /**
     * Returns optimized FFmpeg parameters based on device profile.
     */
    fun getEncodingParams(profile: DeviceProfile): EncodingParams {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        val optimalThreads = (availableProcessors * 0.8).toInt().coerceAtLeast(1)
        Log.d("DeviceSpecs", "Using $optimalThreads threads out of $availableProcessors (80% rule)")

        return when (profile) {
            DeviceProfile.HIGH_END -> EncodingParams(
                videoBitrate = "2000k",
                preset = "medium",
                audioBitrate = "128k",
                threads = optimalThreads.toString()
            )
            DeviceProfile.MID_RANGE -> EncodingParams(
                videoBitrate = "1200k",
                preset = "fast",
                audioBitrate = "96k",
                threads = optimalThreads.toString()
            )
            DeviceProfile.LOW_END -> EncodingParams(
                videoBitrate = "600k",
                preset = "ultrafast",
                audioBitrate = "64k",
                threads = optimalThreads.toString()
            )
        }
    }
}

data class EncodingParams(
    val videoBitrate: String,
    val preset: String,
    val audioBitrate: String,
    val threads: String
)
