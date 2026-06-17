package com.mohadev.videosplitterforstatus.domain.usecase

import com.mohadev.videosplitterforstatus.data.local.SplitHistory
import com.mohadev.videosplitterforstatus.data.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {
    operator fun invoke(): Flow<List<SplitHistory>> = repository.getAllHistory()
}
