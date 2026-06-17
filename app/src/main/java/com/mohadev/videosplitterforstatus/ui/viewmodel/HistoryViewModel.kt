package com.mohadev.videosplitterforstatus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohadev.videosplitterforstatus.data.local.SplitHistory
import com.mohadev.videosplitterforstatus.domain.usecase.DeleteHistoryUseCase
import com.mohadev.videosplitterforstatus.domain.usecase.GetHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val deleteHistoryUseCase: DeleteHistoryUseCase
) : ViewModel() {
    val historyItems: Flow<List<SplitHistory>> = getHistoryUseCase()

    fun deleteHistory(history: SplitHistory) {
        viewModelScope.launch {
            try {
                val dir = File(history.outputFolderPath)
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Failed to delete files", e)
            }
            deleteHistoryUseCase(history.id)
        }
    }
}
