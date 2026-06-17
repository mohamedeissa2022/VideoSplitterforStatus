package com.mohadev.videosplitterforstatus.domain.usecase

import com.mohadev.videosplitterforstatus.data.local.SplitHistory
import com.mohadev.videosplitterforstatus.data.repository.HistoryRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetHistoryUseCaseTest {

    private val repository: HistoryRepository = mockk()
    private val getHistoryUseCase = GetHistoryUseCase(repository)

    @Test
    fun `invoke should return history from repository`() = runTest {
        // Arrange
        val mockHistory = listOf(
            SplitHistory(1, "uri1", "name1", 1000L, "path1", 30),
            SplitHistory(2, "uri2", "name2", 2000L, "path2", 60)
        )
        every { repository.getAllHistory() } returns flowOf(mockHistory)

        // Act
        val result = getHistoryUseCase().first()

        // Assert
        assertEquals(mockHistory, result)
    }
}
