package com.mohadev.videosplitterforstatus.ui.viewmodel

import androidx.work.WorkManager
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class HomeViewModelTest {

    private val workManager: WorkManager = mockk(relaxed = true)
    private val viewModel = HomeViewModel(workManager)

    @Test
    fun `cancelAllWork should call cancelUniqueWork on workManager`() {
        // Act
        viewModel.cancelAllWork()

        // Assert
        verify { workManager.cancelUniqueWork(HomeViewModel.UNIQUE_WORK_NAME) }
    }
}
