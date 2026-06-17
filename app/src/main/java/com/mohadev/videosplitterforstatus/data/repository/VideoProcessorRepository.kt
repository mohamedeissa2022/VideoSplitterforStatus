package com.mohadev.videosplitterforstatus.data.repository

import android.content.Context
import android.net.Uri
import com.mohadev.videosplitterforstatus.data.service.VideoCompressor
import com.mohadev.videosplitterforstatus.data.service.splitVideoWithProgress
import com.mohadev.videosplitterforstatus.domain.DeviceSpecs
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoProcessorRepository @Inject constructor() {

    suspend fun compress(context: Context, uri: String, quality: String): File? {
        return VideoCompressor.compressVideo(context, uri, quality)
    }

    fun split(context: Context, uri: Uri, outputDir: File, duration: Int, prefix: String, onProgress: (Int) -> Unit) {
        splitVideoWithProgress(context, uri, outputDir, duration, prefix, onProgress)
    }
    
    fun getOptimalThreads(context: Context): String {
        val profile = DeviceSpecs.getProfile(context)
        return DeviceSpecs.getEncodingParams(profile).threads
    }
}
