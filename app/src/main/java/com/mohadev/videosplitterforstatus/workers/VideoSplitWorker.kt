package com.mohadev.videosplitterforstatus.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mohadev.videosplitterforstatus.R
import com.mohadev.videosplitterforstatus.domain.AppDatabase
import com.mohadev.videosplitterforstatus.domain.SplitHistory
import com.mohadev.videosplitterforstatus.domain.splitVideoWithProgress
import java.io.File
import androidx.room.Room
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

class VideoSplitWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {

        val inputUriString = inputData.getString("INPUT_PATH") ?: return Result.failure()
        val inputUri = Uri.parse(inputUriString)
        val duration = inputData.getInt("DURATION", 30)
        val videoName = inputData.getString("VIDEO_NAME") ?: "Split Video"

        val timestamp = System.currentTimeMillis()
        val uniqueDirName = "Split_${timestamp}"
        val outputDir = File(applicationContext.getExternalFilesDir(null), "VideoSplitter/$uniqueDirName")

        if (!outputDir.exists()) outputDir.mkdirs()

        return try {
            
            splitVideoWithProgress(
                applicationContext,
                inputUri,
                outputDir,
                duration
            ) { progress ->
                setProgressAsync(
                    workDataOf("PROGRESS" to progress)
                )
            }

            // Verify if files were created and are not empty
            val files = outputDir.listFiles { f -> f.extension == "mp4" }
            if (files.isNullOrEmpty() || files.all { it.length() == 0L }) {
                Log.e("Worker", "No valid files created")
                return Result.failure()
            }

            // Save to History
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "video_splitter_db"
            ).build()
            
            db.historyDao().insert(
                SplitHistory(
                    originalVideoUri = inputUriString,
                    videoName = videoName,
                    timestamp = timestamp,
                    outputFolderPath = outputDir.absolutePath,
                    segmentDuration = duration
                )
            )

            showCompletionNotification()

            Result.success(
                workDataOf("OUTPUT_DIR" to outputDir.absolutePath)
            )

        } catch (e: Exception) {
            Log.e("Worker", "Error splitting video", e)
            Result.failure()
        }
    }

    private fun showCompletionNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "video_processing_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                applicationContext.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.notif_split_complete_title))
            .setContentText(applicationContext.getString(R.string.notif_split_complete_msg))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(1, notification)
    }
}
