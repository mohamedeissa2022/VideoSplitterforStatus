package com.mohadev.videosplitterforstatus.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.work.*
import com.mohadev.videosplitterforstatus.domain.usecase.CompressVideoUseCase
import com.mohadev.videosplitterforstatus.ui.screens.VideoBranding
import com.mohadev.videosplitterforstatus.workers.VideoSplitWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workManager: WorkManager,
    val compressVideoUseCase: CompressVideoUseCase
) : ViewModel() {

    companion object {
        const val WORK_TAG = "DYNAMIC_BATCH_WORK"
        const val UNIQUE_WORK_NAME = "DynamicVideoSplitter"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllActiveWorkFlow(): Flow<List<WorkInfo>> {
        return workManager.getWorkInfosByTagLiveData(WORK_TAG).asFlow()
            .map { list -> list.filter { !it.state.isFinished || it.state == WorkInfo.State.SUCCEEDED } }
    }

    fun startBatchSplitWithBranding(
        brandingList: List<VideoBranding>, 
        durationInSeconds: Int
    ) {
        Log.d("DynamicSplit", "Queueing ${brandingList.size} workers")

        var continuation = workManager.beginUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            createWorkerRequest(brandingList.first(), durationInSeconds)
        )

        for (i in 1 until brandingList.size) {
            continuation = continuation.then(createWorkerRequest(brandingList[i], durationInSeconds))
        }

        continuation.enqueue()
    }

    private fun createWorkerRequest(branding: VideoBranding, duration: Int): OneTimeWorkRequest {
        val inputData = Data.Builder()
            .putString("INPUT_PATH", branding.uri.toString())
            .putString("LOGO_PATH", branding.logoUri?.toString())
            .putString("BRANDING_TEXT", branding.text)
            .putInt("DURATION", duration)
            .build()

        return OneTimeWorkRequestBuilder<VideoSplitWorker>()
            .setInputData(inputData)
            .addTag(WORK_TAG)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    }

    fun cancelAllWork() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
