package com.mohadev.videosplitterforstatus.domain.usecase

import com.mohadev.videosplitterforstatus.data.repository.HistoryRepository
import javax.inject.Inject

class DeleteHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {
    suspend operator fun invoke(id: Int) = repository.deleteHistory(id)
}
