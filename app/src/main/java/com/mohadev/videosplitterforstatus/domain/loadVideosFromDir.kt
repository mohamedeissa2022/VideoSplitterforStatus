package com.mohadev.videosplitterforstatus.domain

import android.content.Context
import android.net.Uri
import com.mohadev.videosplitterforstatus.domain.model.VideoItem
import java.io.File

/**
 * Loads videos from a directory and groups them by original video prefix (v0_, v1_, etc.)
 */
fun loadVideosGrouped(dirPath: String, context: Context): Map<String, List<VideoItem>> {
    val dir = File(dirPath)
    if (!dir.exists()) return emptyMap()

    // File names are formatted as "v{index}_part_{partIndex}.mp4"
    val allFiles = dir.listFiles()
        ?.filter { it.extension == "mp4" }
        ?.sortedBy { it.name } ?: return emptyMap()

    // Grouping by "v{index}" prefix
    return allFiles.groupBy { file ->
        file.name.substringBefore("_part")
    }.mapValues { entry ->
        entry.value.map { file ->
            VideoItem(
                uri = Uri.fromFile(file),
                name = file.name
            )
        }
    }
}

fun loadVideosFromDir(dirPath: String, context: Context): List<VideoItem> {
    val dir = File(dirPath)
    if (!dir.exists()) return emptyList()

    return dir.listFiles()
        ?.filter { it.extension == "mp4" }
        ?.sortedBy { it.name }
        ?.map {
            VideoItem(
                uri = Uri.fromFile(it),
                name = it.name
            )
        } ?: emptyList()
}
