package com.mohadev.videosplitterforstatus.data.repository

import android.content.Context
import com.mohadev.videosplitterforstatus.domain.model.VideoItem
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test

class VideoRepositoryTest {

    private val repository = VideoRepository()
    private val context: Context = mockk()

    @Test
    fun `getGroupedVideos should return a map`() {
        // Since loadVideosGrouped uses File and Uri.fromFile, it's hard to unit test without real files
        // but we can check if it returns a non-null map for a non-existent path
        val result = repository.getGroupedVideos("/non/existent/path", context)
        assertNotNull(result)
    }
}
