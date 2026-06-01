package com.mohadev.videosplitterforstatus.workers


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VideoSplitWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("VideoSplitWorker", "Worker Started")
        // 1. Get the String URI passed from the ViewModel
        val inputUriString = inputData.getString("INPUT_PATH") ?: return@withContext Result.failure()
        val duration = inputData.getInt("DURATION", 30)
        val videoName = inputData.getString("VIDEO_NAME") ?: "video"

        // 2. Convert the String back to a Uri
        val inputUri = Uri.parse(inputUriString)

        // 3. THE MAGIC FIX: Convert the secure URI to an FFmpeg-readable path
        // The "r" means we are opening it in Read-Only mode.
        Log.d(
            "VideoSplitWorker",
            FFmpegKitConfig.getFFmpegVersion()
        )
        val safeInputPath =
            FFmpegKitConfig.getSafParameter(
                applicationContext,
                inputUri,
                "r"
            )
        Log.d("VideoSplitWorker", "SAFE PATH = $safeInputPath")
        val outputDir = File(applicationContext.getExternalFilesDir(null), "VideoSplitter")
        if (!outputDir.exists()) outputDir.mkdirs()

        // Sanitize video name for filename
        val sanitizedName =
            videoName.substringBeforeLast(".")
                .replace(Regex("[^\\w-]"), "_")
        val outputPattern = "${outputDir.absolutePath}/${sanitizedName}_part%03d.mp4"

        // 5. Use safeInputPath WITHOUT quotes for SAF paths
        // Try with -c copy first for speed, fallback to re-encoding if needed in logs
        val command = """
-i $safeInputPath
-map 0
-c copy
-f segment
-segment_time $duration
-reset_timestamps 1
"$outputPattern"
""".trimIndent().replace("\n", " ")
        android.util.Log.d("VideoSplitWorker", "Executing FFmpeg command: $command")

        try {
            android.util.Log.d("VideoSplitWorker", "Setting progress to 5%")
            setProgress(workDataOf("PROGRESS" to 5)) // Mark that we've started

            // FFmpegKit.execute is synchronous. Let's use it for now but handle failure better.
            val session = FFmpegKit.execute(command)
            val returnCode = session.returnCode
            val logs = session.allLogsAsString
            android.util.Log.d("VideoSplitWorker", "FFmpeg logs: $logs")

            if (ReturnCode.isSuccess(returnCode)) {
                android.util.Log.d("VideoSplitWorker", "FFmpeg Success, setting progress to 100%")
                setProgress(workDataOf("PROGRESS" to 100))
                val outputData = workDataOf("OUTPUT_DIR" to outputDir.absolutePath)
                return@withContext Result.success(outputData)
            } else {
                // Check if it's a specific codec error that might need re-encoding
                if (logs.contains("codec frame size is not set") || logs.contains("could not find codec parameters") || logs.contains("Operation not permitted")) {
                     android.util.Log.w("VideoSplitWorker", "Operation failed or copy failed, attempting with re-encoding and alternate path...")
                     
                     // Try writing to a different location if it's a permission issue with internal storage (unlikely but possible)
                     val altOutputDir = File(applicationContext.cacheDir, "VideoSplitter_Temp")
                     altOutputDir.mkdirs()
                     val altOutputPattern = "${altOutputDir.absolutePath}/${sanitizedName}_part%03d.mp4"
                     
                     val retryCommand = "-i $safeInputPath -c:v libx264 -preset superfast -crf 23 -c:a aac -map 0 -segment_time $duration -f segment -reset_timestamps 1 \"$altOutputPattern\""
                     val retrySession = FFmpegKit.execute(retryCommand)
                     if (ReturnCode.isSuccess(retrySession.returnCode)) {
                         setProgress(workDataOf("PROGRESS" to 100))
                         return@withContext Result.success(workDataOf("OUTPUT_DIR" to altOutputDir.absolutePath))
                     }
                }

                android.util.Log.e("VideoSplitWorker", "FFmpeg Failed with return code $returnCode: $logs")
                return@withContext Result.failure()
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoSplitWorker", "Exception during FFmpeg execution", e)
            return@withContext Result.failure()
        }
    }
}