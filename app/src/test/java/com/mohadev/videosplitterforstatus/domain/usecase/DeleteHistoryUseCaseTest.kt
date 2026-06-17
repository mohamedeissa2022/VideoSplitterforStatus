package com.mohadev.videosplitterforstatus.domain.usecase

import com.mohadev.videosplitterforstatus.data.repository.HistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteHistoryUseCaseTest {

    private val repository: HistoryRepository = mockk()
    private val deleteHistoryUseCase = DeleteHistoryUseCase(repository)

    @Test
    fun `invoke should call deleteHistory on repository`() = runTest {
        // Arrange
        coEvery { repository.deleteHistory(1) } returns Unit

        // Act
        deleteHistoryUseCase(1)

        // Assert
        coVerify { repository.deleteHistory(1) }
    }
}
