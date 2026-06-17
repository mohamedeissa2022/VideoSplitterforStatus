package com.mohadev.videosplitterforstatus.workers


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mohadev.videosplitterforstatus.domain.splitVideoWithProgress


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VideoSplitWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {

        val inputUri = Uri.parse(inputData.getString("INPUT_PATH"))
        val duration = inputData.getInt("DURATION", 30)

        val outputDir =
            File(applicationContext.getExternalFilesDir(null), "VideoSplitter")

        try {
            if (outputDir.exists()) {
                outputDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e("Worker", "Failed to clear output directory", e)
        }

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

            Result.success(
                workDataOf("OUTPUT_DIR" to outputDir.absolutePath)
            )

        } catch (e: Exception) {
            Log.e("Worker", "Error splitting video", e)
            Result.failure()
        }
    }
}