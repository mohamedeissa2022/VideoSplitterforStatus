package com.mohadev.videosplitterforstatus.domain.usecase

import android.content.Context
import com.mohadev.videosplitterforstatus.data.repository.VideoRepository
import com.mohadev.videosplitterforstatus.domain.model.VideoItem
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class GetGroupedVideosUseCaseTest {

    private val repository: VideoRepository = mockk()
    private val getGroupedVideosUseCase = GetGroupedVideosUseCase(repository)
    private val context: Context = mockk()

    @Test
    fun `invoke should return grouped videos from repository`() {
        // Arrange
        val mockGroupedVideos = mapOf(
            "v0" to listOf(VideoItem(mockk(), "name1"))
        )
        every { repository.getGroupedVideos("path", context) } returns mockGroupedVideos

        // Act
        val result = getGroupedVideosUseCase("path", context)

        // Assert
        assertEquals(mockGroupedVideos, result)
    }
}
