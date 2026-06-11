package com.mohadev.videosplitterforstatus.ui.screens

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.mohadev.videosplitterforstatus.workers.VideoSplitWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workManager: WorkManager
) : ViewModel() {

    // Store the ID of the current running job
    private var _currentWorkId = mutableStateOf<UUID?>(null)
    val currentWorkId: UUID? get() = _currentWorkId.value

    fun startVideoSplit(videoPath: String, videoName: String, durationInSeconds: Int) {
        Log.d("VideoSplit", "startVideoSplit called")
        val inputData = Data.Builder()
            .putString("INPUT_PATH", videoPath)
            .putString("VIDEO_NAME", videoName)
            .putInt("DURATION", durationInSeconds)
            .build()

        val splitWorkRequest = OneTimeWorkRequestBuilder<VideoSplitWorker>()
            .setInputData(inputData)
            .build()

        // Save the ID before enqueuing
        _currentWorkId.value = splitWorkRequest.id
        workManager.enqueue(splitWorkRequest)
    }

    // Expose a Flow of the WorkInfo so Compose can listen to it
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getWorkInfoFlow(): Flow<WorkInfo> {
        return snapshotFlow { _currentWorkId.value }
            .flatMapLatest { id ->
                if (id == null) {
                    kotlinx.coroutines.flow.emptyFlow()
                } else {
                    workManager.getWorkInfoByIdLiveData(id).asFlow().mapNotNull { it }
                }
            }
    }

    // Cancel the job if the user taps "Cancel"
    fun cancelCurrentWork() {
        currentWorkId?.let { id ->
            workManager.cancelWorkById(id)
        }
    }
}