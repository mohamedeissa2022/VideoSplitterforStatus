package com.mohadev.videosplitterforstatus.data.repository

import com.mohadev.videosplitterforstatus.data.local.HistoryDao
import com.mohadev.videosplitterforstatus.data.local.SplitHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    fun getAllHistory(): Flow<List<SplitHistory>> = historyDao.getAllHistory()

    suspend fun insertHistory(history: SplitHistory) = historyDao.insert(history)

    suspend fun deleteHistory(id: Int) = historyDao.deleteById(id)
}
