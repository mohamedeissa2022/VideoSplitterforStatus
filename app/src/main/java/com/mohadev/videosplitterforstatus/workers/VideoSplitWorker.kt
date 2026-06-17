package com.mohadev.videosplitterforstatus.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mohadev.videosplitterforstatus.MainActivity
import com.mohadev.videosplitterforstatus.R
import com.mohadev.videosplitterforstatus.domain.AppDatabase
import com.mohadev.videosplitterforstatus.domain.DeviceSpecs
import com.mohadev.videosplitterforstatus.domain.SplitHistory
import com.mohadev.videosplitterforstatus.domain.splitVideoWithProgress
import java.io.File
import androidx.room.Room
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

/**
 * Dynamic Dynamic Worker: Processes ONE video per instance.
 * Highly scalable for large batches.
 */
class VideoSplitWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val notificationId = (System.currentTimeMillis() % 10000).toInt()
    private val channelId = "dynamic_processing_channel"

    override suspend fun doWork(): Result {
        // 1. Get Dynamic Input Data
        val inputUriString = inputData.getString("INPUT_PATH") ?: return Result.failure()
        val inputUri = Uri.parse(inputUriString)
        val duration = inputData.getInt("DURATION", 30)
        val logoUriString = inputData.getString("LOGO_PATH")
        val brandingText = inputData.getString("BRANDING_TEXT") ?: ""
        val videoName = inputUri.lastPathSegment ?: "Split Video"

        // 2. Promote to Foreground (Dynamic Priority)
        // This ensures the worker isn't killed by the system during long tasks
        setForeground(createForegroundInfo(videoName))

        val timestamp = System.currentTimeMillis()
        val outputDir = File(applicationContext.getExternalFilesDir(null), "VideoSplitter/Queue_$timestamp")
        if (!outputDir.exists()) outputDir.mkdirs()

        return try {
            // 🛠️ Dynamic Specs
            val profile = DeviceSpecs.getProfile(applicationContext)
            val params = DeviceSpecs.getEncodingParams(profile)

            Log.d("DynamicWorker", "Processing: $videoName with profile: $profile")

            if (logoUriString != null || brandingText.isNotEmpty()) {
                splitWithBranding(
                    inputUri, outputDir, duration, 
                    logoUriString?.let { Uri.parse(it) }, 
                    brandingText, params
                )
            } else {
                splitVideoWithProgress(
                    applicationContext, inputUri, outputDir, duration, "dyn"
                ) { progress ->
                    setProgressAsync(workDataOf("PROGRESS" to progress, "VIDEO_NAME" to videoName))
                }
            }

            // Save to history
            saveHistory(inputUriString, videoName, outputDir.absolutePath, duration)

            Result.success(workDataOf("OUTPUT_DIR" to outputDir.absolutePath))
        } catch (e: Exception) {
            Log.e("DynamicWorker", "Failed processing $videoName", e)
            Result.failure()
        }
    }

    private suspend fun saveHistory(uri: String, name: String, path: String, dur: Int) {
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "video_splitter_db").build()
        db.historyDao().insert(SplitHistory(originalVideoUri = uri, videoName = name, timestamp = System.currentTimeMillis(), outputFolderPath = path, segmentDuration = dur))
        db.close()
    }

    private fun splitWithBranding(inputUri: Uri, outputDir: File, segmentSeconds: Int, logoUri: Uri?, text: String, params: com.mohadev.videosplitterforstatus.domain.EncodingParams) {
        val context = applicationContext
        val resolvedFile = getFileFromUri(context, inputUri, "split") ?: return
        val logoFile = logoUri?.let { getFileFromUri(context, it, "logo") }
        val videoName = inputUri.lastPathSegment ?: "Processing"
        
        try {
            val infoSession = FFmpegKit.execute("-i \"${resolvedFile.absolutePath}\"")
            val match = "Duration: (\\d{2}):(\\d{2}):(\\d{2})\\.".toRegex().find(infoSession.allLogsAsString)
            val totalSeconds = if (match != null) {
                val (h, m, s) = match.destructured
                h.toInt() * 3600 + m.toInt() * 60 + s.toInt()
            } else 300 
            
            val parts = (totalSeconds / segmentSeconds) + 1
            for (i in 0 until parts) {
                val start = i * segmentSeconds
                if (start >= totalSeconds && i > 0) break
                val outputFile = File(outputDir, "part_$i.mp4")
                
                val complexFilter = when {
                    logoFile != null && text.isNotEmpty() -> "[1:v]scale=100:-1[logo];[0:v][logo]overlay=W-w-20:H-h-50[bg];[bg]drawtext=text='$text':x=W-tw-20:y=H-th-15:fontsize=24:fontcolor=white:box=1:boxcolor=black@0.4,scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p"
                    logoFile != null -> "[1:v]scale=100:-1[logo];[0:v][logo]overlay=W-w-20:H-h-20,scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p"
                    else -> "drawtext=text='$text':x=W-tw-20:y=H-th-20:fontsize=24:fontcolor=white:box=1:boxcolor=black@0.4,scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p"
                }

                val inputArgs = if (logoFile != null) "-i \"${resolvedFile.absolutePath}\" -i \"${logoFile.absolutePath}\"" else "-i \"${resolvedFile.absolutePath}\""
                val command = "-y -threads ${params.threads} -ss $start -t $segmentSeconds $inputArgs -filter_complex \"$complexFilter\" -vcodec h264_mediacodec -b:v ${params.videoBitrate} -acodec aac -b:a ${params.audioBitrate} \"${outputFile.absolutePath}\""
                
                FFmpegKit.execute(command)
                setProgressAsync(workDataOf("PROGRESS" to ((i + 1).toFloat() / parts * 100).toInt(), "VIDEO_NAME" to videoName))
            }
        } finally {
            resolvedFile.delete()
            logoFile?.delete()
        }
    }

    private fun createForegroundInfo(videoName: String): ForegroundInfo {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(channelId, "Active Production", NotificationManager.IMPORTANCE_LOW))
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Splitting: $videoName")
            .setTicker("Processing Video")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        return ForegroundInfo(notificationId, notification)
    }

    private fun getFileFromUri(context: Context, uri: Uri, prefix: String): File? {
        if (uri.scheme == "file") return uri.path?.let { File(it) }
        return try {
            val tempFile = File(context.cacheDir, "${prefix}_temp_${System.currentTimeMillis()}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
            if (tempFile.exists() && tempFile.length() > 0) tempFile else null
        } catch (e: Exception) { null }
    }
}
