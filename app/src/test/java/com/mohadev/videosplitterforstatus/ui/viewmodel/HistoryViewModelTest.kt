package com.mohadev.videosplitterforstatus.ui.viewmodel

import com.mohadev.videosplitterforstatus.data.local.SplitHistory
import com.mohadev.videosplitterforstatus.domain.usecase.DeleteHistoryUseCase
import com.mohadev.videosplitterforstatus.domain.usecase.GetHistoryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val getHistoryUseCase: GetHistoryUseCase = mockk()
    private val deleteHistoryUseCase: DeleteHistoryUseCase = mockk()
    private lateinit var viewModel: HistoryViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getHistoryUseCase() } returns flowOf(emptyList())
        viewModel = HistoryViewModel(getHistoryUseCase, deleteHistoryUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `historyItems should emit values from GetHistoryUseCase`() = runTest {
        // Arrange
        val mockHistory = listOf(
            SplitHistory(1, "uri1", "name1", 1000L, "path1", 30)
        )
        every { getHistoryUseCase() } returns flowOf(mockHistory)
        
        // Re-init to capture the new mock behavior if needed or just use current
        val vm = HistoryViewModel(getHistoryUseCase, deleteHistoryUseCase)

        // Act
        val result = vm.historyItems.first()

        // Assert
        assertEquals(mockHistory, result)
    }

    @Test
    fun `deleteHistory should call DeleteHistoryUseCase`() = runTest {
        // Arrange
        val history = SplitHistory(1, "uri1", "name1", 1000L, "path1", 30)
        coEvery { deleteHistoryUseCase(any()) } returns Unit

        // Act
        viewModel.deleteHistory(history)

        // Assert
        coVerify { deleteHistoryUseCase(1) }
    }
}
