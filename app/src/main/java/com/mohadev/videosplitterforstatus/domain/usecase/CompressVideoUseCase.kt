package com.mohadev.videosplitterforstatus.domain.usecase

import android.content.Context
import com.mohadev.videosplitterforstatus.data.repository.VideoProcessorRepository
import java.io.File
import javax.inject.Inject

class CompressVideoUseCase @Inject constructor(
    private val repository: VideoProcessorRepository
) {
    suspend operator fun invoke(context: Context, uri: String, quality: String): File? {
        return repository.compress(context, uri, quality)
    }
}
