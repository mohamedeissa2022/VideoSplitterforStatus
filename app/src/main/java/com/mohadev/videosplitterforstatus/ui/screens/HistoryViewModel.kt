package com.mohadev.videosplitterforstatus.ui.screens

import androidx.lifecycle.ViewModel
import com.mohadev.videosplitterforstatus.domain.HistoryDao
import com.mohadev.videosplitterforstatus.domain.SplitHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyDao: HistoryDao
) : ViewModel() {
    val historyItems: Flow<List<SplitHistory>> = historyDao.getAllHistory()

    fun deleteHistory(history: SplitHistory) {
        viewModelScope.launch {
            // 1. Delete the files from storage
            try {
                val dir = File(history.outputFolderPath)
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Failed to delete files", e)
            }

            // 2. Delete the record from database
            historyDao.deleteById(history.id)
        }
    }
}
