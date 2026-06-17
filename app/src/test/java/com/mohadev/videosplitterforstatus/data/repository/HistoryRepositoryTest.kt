package com.mohadev.videosplitterforstatus.data.repository

import com.mohadev.videosplitterforstatus.data.local.HistoryDao
import com.mohadev.videosplitterforstatus.data.local.SplitHistory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryRepositoryTest {

    private val dao: HistoryDao = mockk()
    private val repository = HistoryRepository(dao)

    @Test
    fun `getAllHistory should return flow from dao`() = runTest {
        // Arrange
        val mockHistory = listOf(mockk<SplitHistory>())
        every { dao.getAllHistory() } returns flowOf(mockHistory)

        // Act
        val result = repository.getAllHistory().first()

        // Assert
        assertEquals(mockHistory, result)
    }

    @Test
    fun `insertHistory should call insert on dao`() = runTest {
        // Arrange
        val history = mockk<SplitHistory>()
        coEvery { dao.insert(history) } returns 1L

        // Act
        repository.insertHistory(history)

        // Assert
        coVerify { dao.insert(history) }
    }

    @Test
    fun `deleteHistory should call deleteById on dao`() = runTest {
        // Arrange
        coEvery { dao.deleteById(1) } returns Unit

        // Act
        repository.deleteHistory(1)

        // Assert
        coVerify { dao.deleteById(1) }
    }
}
