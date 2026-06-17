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
import com.mohadev.videosplitterforstatus.data.local.AppDatabase
import com.mohadev.videosplitterforstatus.data.local.SplitHistory
import com.mohadev.videosplitterforstatus.data.repository.HistoryRepository
import com.mohadev.videosplitterforstatus.data.repository.VideoProcessorRepository
import com.mohadev.videosplitterforstatus.domain.DeviceSpecs
import com.mohadev.videosplitterforstatus.domain.usecase.SplitVideoUseCase
import java.io.File
import androidx.room.Room
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

class VideoSplitWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private fun getDb(): AppDatabase {
        return Room.databaseBuilder(applicationContext, AppDatabase::class.java, "video_splitter_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    override suspend fun doWork(): Result {
        return try {
            val batchDataString = inputData.getString("BATCH_DATA") ?: return Result.failure()
            val duration = inputData.getInt("DURATION", 30)

            val batchList = batchDataString.split(";").mapNotNull { item ->
                try {
                    val parts = item.split("|")
                    if (parts.isEmpty() || parts[0].isEmpty()) return@mapNotNull null
                    val uri = Uri.parse(parts[0])
                    val logo = if (parts.size > 1 && parts[1].isNotEmpty()) Uri.parse(parts[1]) else null
                    val text = if (parts.size > 2) parts[2] else ""
                    Triple(uri, logo, text)
                } catch (e: Exception) { null }
            }

            if (batchList.isEmpty()) return Result.failure()

            val timestamp = System.currentTimeMillis()
            val outputDir = File(applicationContext.getExternalFilesDir(null), "VideoSplitter/Batch_$timestamp")
            if (!outputDir.exists()) outputDir.mkdirs()

            var overallSuccess = true

            batchList.forEachIndexed { index, (inputUri, logoUri, brandingText) ->
                try {
                    val videoName = inputUri.lastPathSegment ?: "Video_${index + 1}"
                    updateProgress(0, index, batchList.size, videoName)

                    if (logoUri != null || brandingText.isNotEmpty()) {
                        splitWithBranding(inputUri, outputDir, duration, logoUri, brandingText, index, batchList.size)
                    } else {
                        // Use split logic from repository (abstracted)
                        VideoProcessorRepository().split(applicationContext, inputUri, outputDir, duration, "v$index") { progress ->
                            updateProgress(progress, index, batchList.size, videoName)
                        }
                    }

                    val database = getDb()
                    database.historyDao().insert(SplitHistory(
                        originalVideoUri = inputUri.toString(), 
                        videoName = videoName, 
                        timestamp = timestamp, 
                        outputFolderPath = outputDir.absolutePath, 
                        segmentDuration = duration
                    ))
                    database.close()

                } catch (e: Exception) {
                    Log.e("Worker", "Batch item $index crashed", e)
                    overallSuccess = false
                }
            }

            if (overallSuccess) {
                showCompletionNotification(outputDir.absolutePath)
                Result.success(workDataOf("OUTPUT_DIR" to outputDir.absolutePath))
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("Worker", "Worker global crash", e)
            Result.failure()
        }
    }

    private fun updateProgress(progress: Int, current: Int, total: Int, name: String) {
        setProgressAsync(workDataOf("PROGRESS" to progress, "CURRENT_INDEX" to current, "TOTAL_VIDEOS" to total, "CURRENT_VIDEO_NAME" to name))
    }

    private fun splitWithBranding(inputUri: Uri, outputDir: File, segmentSeconds: Int, logoUri: Uri?, text: String, batchIndex: Int, totalInBatch: Int) {
        val context = applicationContext
        val resolvedFile = getFileFromUri(context, inputUri, "split") ?: return
        val logoFile = logoUri?.let { getFileFromUri(context, it, "logo") }
        val videoName = inputUri.lastPathSegment ?: "Video_${batchIndex + 1}"
        
        val profile = DeviceSpecs.getProfile(applicationContext)
        val params = DeviceSpecs.getEncodingParams(profile)

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
                val outputFile = File(outputDir, "v${batchIndex}_part_$i.mp4")
                
                val complexFilter = when {
                    logoFile != null && text.isNotEmpty() -> "[1:v]scale=100:-1[logo];[0:v][logo]overlay=W-w-20:H-h-50[bg];[bg]drawtext=text='$text':x=W-tw-20:y=H-th-15:fontsize=24:fontcolor=white:box=1:boxcolor=black@0.4,scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p"
                    logoFile != null -> "[1:v]scale=100:-1[logo];[0:v][logo]overlay=W-w-20:H-h-20,scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p"
                    else -> "drawtext=text='$text':x=W-tw-20:y=H-th-20:fontsize=24:fontcolor=white:box=1:boxcolor=black@0.4,scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p"
                }

                val inputArgs = if (logoFile != null) "-i \"${resolvedFile.absolutePath}\" -i \"${logoFile.absolutePath}\"" else "-i \"${resolvedFile.absolutePath}\""
                val command = "-y -threads ${params.threads} -ss $start -t $segmentSeconds $inputArgs -filter_complex \"$complexFilter\" -vcodec h264_mediacodec -b:v ${params.videoBitrate} -acodec aac \"${outputFile.absolutePath}\""
                
                if (!ReturnCode.isSuccess(FFmpegKit.execute(command).returnCode)) {
                    FFmpegKit.execute("-y -threads ${params.threads} -ss $start -t $segmentSeconds $inputArgs -filter_complex \"$complexFilter\" -vcodec mpeg4 -b:v ${params.videoBitrate} -acodec aac \"${outputFile.absolutePath}\"" )
                }
                updateProgress(((i + 1).toFloat() / parts * 100).toInt().coerceAtMost(100), batchIndex, totalInBatch, videoName)
            }
        } finally {
            resolvedFile.delete()
            logoFile?.delete()
        }
    }

    private fun showCompletionNotification(outputDir: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "video_processing_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(channelId, applicationContext.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_HIGH))
        }
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TARGET_ROUTE", "results/${Uri.encode(outputDir)}")
        }
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, channelId).setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.notif_split_complete_title))
            .setContentText(applicationContext.getString(R.string.notif_split_complete_msg))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(pendingIntent).build()
        try { notificationManager.notify(1, notification) } catch (e: SecurityException) { }
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
