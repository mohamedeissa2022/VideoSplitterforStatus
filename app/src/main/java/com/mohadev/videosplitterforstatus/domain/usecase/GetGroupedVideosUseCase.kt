package com.mohadev.videosplitterforstatus.domain.usecase

import android.content.Context
import com.mohadev.videosplitterforstatus.data.repository.VideoRepository
import com.mohadev.videosplitterforstatus.domain.model.VideoItem
import javax.inject.Inject

class GetGroupedVideosUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    operator fun invoke(dirPath: String, context: Context): Map<String, List<VideoItem>> {
        return repository.getGroupedVideos(dirPath, context)
    }
}
