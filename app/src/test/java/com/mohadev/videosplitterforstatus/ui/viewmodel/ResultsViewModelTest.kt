package com.mohadev.videosplitterforstatus.ui.viewmodel

import android.content.Context
import com.mohadev.videosplitterforstatus.domain.model.VideoItem
import com.mohadev.videosplitterforstatus.domain.usecase.GetGroupedVideosUseCase
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultsViewModelTest {

    private val getGroupedVideosUseCase: GetGroupedVideosUseCase = mockk()
    private val viewModel = ResultsViewModel(getGroupedVideosUseCase)
    private val context: Context = mockk()

    @Test
    fun `load should update groupedVideos state`() {
        // Arrange
        val mockGroupedVideos = mapOf(
            "v0" to listOf(VideoItem(mockk(), "name1"))
        )
        every { getGroupedVideosUseCase("path", context) } returns mockGroupedVideos

        // Act
        viewModel.load("path", context)

        // Assert
        assertEquals(mockGroupedVideos, viewModel.groupedVideos.value)
    }
}
