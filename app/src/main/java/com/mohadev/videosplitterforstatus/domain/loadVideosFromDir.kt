package com.mohadev.videosplitterforstatus.domain

import android.content.Context
import android.net.Uri
import com.mohadev.videosplitterforstatus.VideoItem
import java.io.File

fun loadVideosFromDir(dirPath: String, context: Context): List<VideoItem> {

    val dir = File(dirPath)

    if (!dir.exists()) return emptyList()

    return dir.listFiles()
        ?.filter { it.extension == "mp4" }
        ?.sorted()
        ?.map {
            VideoItem(
                uri = Uri.fromFile(it),
                name = it.name
            )
        } ?: emptyList()
}