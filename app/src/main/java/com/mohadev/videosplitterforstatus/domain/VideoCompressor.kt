package com.mohadev.videosplitterforstatus.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.mohadev.videosplitterforstatus.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VideoCompressor {

    suspend fun compressVideo(
        context: Context,
        inputUriString: String,
        targetBitrate: String = "800k",
        onProgress: (Float) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        
        Log.d("VideoCompressor", "Process Start: $inputUriString with bitrate $targetBitrate")

        // 🛠️ Dynamic Tuning for Compression (80% Rule)
        val profile = DeviceSpecs.getProfile(context)
        val params = DeviceSpecs.getEncodingParams(profile)
        val threads = params.threads

        val resolvedFile = getFileFromUri(context, Uri.parse(inputUriString))
        if (resolvedFile == null || !resolvedFile.exists()) {
            Log.e("VideoCompressor", "Error: Input file missing or copy failed")
            return@withContext null
        }

        val outputDir = File(context.getExternalFilesDir(null), "Compressed")
        if (!outputDir.exists()) outputDir.mkdirs()

        val outputFile = File(outputDir, "compressed_${System.currentTimeMillis()}.mp4")
        
        val command = "-y -threads $threads -i \"${resolvedFile.absolutePath}\" -vcodec h264_mediacodec -b:v $targetBitrate -maxrate $targetBitrate -bufsize 1000k -vf \"scale=trunc(iw/2)*2:trunc(ih/2)*2,scale=-2:480,format=yuv420p\" -acodec aac -b:a 48k \"${outputFile.absolutePath}\""

        Log.d("VideoCompressor", "Executing Smart Compression: $command")
        
        val session = FFmpegKit.execute(command)

        val result = if (ReturnCode.isSuccess(session.returnCode)) {
            val originalSize = resolvedFile.length()
            val compressedSize = outputFile.length()
            
            if (compressedSize >= originalSize && originalSize > 0) {
                Log.d("VideoCompressor", "Compressed file ($compressedSize) is larger than original ($originalSize). Keeping original.")
                resolvedFile 
            } else {
                Log.d("VideoCompressor", "SUCCESS: saved to ${outputFile.absolutePath} ($compressedSize bytes)")
                showCompletionNotification(context)
                outputFile
            }
        } else {
            Log.e("VideoCompressor", "FAILURE code: ${session.returnCode}")
            null
        }

        try {
            if (resolvedFile.exists() && result != resolvedFile && resolvedFile.absolutePath.contains("compress_temp")) {
                resolvedFile.delete()
            }
        } catch (e: Exception) {
            Log.e("VideoCompressor", "Cleanup error", e)
        }

        result
    }

    private fun showCompletionNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "video_processing_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_compress_complete_title))
            .setContentText(context.getString(R.string.notif_compress_complete_msg))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
            
        try {
            notificationManager.notify(2, notification)
        } catch (e: SecurityException) {
            Log.e("VideoCompressor", "Notification permission missing", e)
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        if (uri.scheme == "file") return uri.path?.let { File(it) }
        return try {
            val tempFile = File(context.cacheDir, "compress_temp_${System.currentTimeMillis()}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) tempFile else null
        } catch (e: Exception) { 
            Log.e("VideoCompressor", "getFileFromUri error", e)
            null
        }
    }
}
