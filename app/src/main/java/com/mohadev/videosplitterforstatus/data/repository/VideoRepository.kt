package com.mohadev.videosplitterforstatus.data.repository

import android.content.Context
import com.mohadev.videosplitterforstatus.domain.model.VideoItem
import com.mohadev.videosplitterforstatus.data.service.VideoLoader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor() {
    fun getGroupedVideos(dirPath: String, context: Context): Map<String, List<VideoItem>> {
        return VideoLoader.loadVideosGrouped(dirPath, context)
    }
}
