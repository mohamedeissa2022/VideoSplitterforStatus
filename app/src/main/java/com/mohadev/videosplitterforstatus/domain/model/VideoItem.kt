package com.mohadev.videosplitterforstatus.domain.model

import android.graphics.Bitmap
import android.net.Uri

data class VideoItem(
    val uri: Uri,
    val name: String,
    val thumbnail: Bitmap? = null
)
