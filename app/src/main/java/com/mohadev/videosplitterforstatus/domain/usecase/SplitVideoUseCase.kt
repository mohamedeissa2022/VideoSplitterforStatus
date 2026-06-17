package com.mohadev.videosplitterforstatus.domain.usecase

import android.content.Context
import android.net.Uri
import com.mohadev.videosplitterforstatus.data.repository.VideoProcessorRepository
import java.io.File
import javax.inject.Inject

class SplitVideoUseCase @Inject constructor(
    private val repository: VideoProcessorRepository
) {
    operator fun invoke(context: Context, uri: Uri, outputDir: File, duration: Int, prefix: String, onProgress: (Int) -> Unit) {
        repository.split(context, uri, outputDir, duration, prefix, onProgress)
    }
}
