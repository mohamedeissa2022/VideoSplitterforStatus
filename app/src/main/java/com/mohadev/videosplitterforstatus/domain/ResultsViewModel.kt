package com.mohadev.videosplitterforstatus.domain

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.mohadev.videosplitterforstatus.VideoItem
import kotlin.collections.emptyList
import androidx.compose.runtime.State

class ResultsViewModel : ViewModel() {

    private val _videos = mutableStateOf<List<VideoItem>>(emptyList())
    val videos: State<List<VideoItem>> = _videos

    fun load(dir: String, context: Context) {
        _videos.value = loadVideosFromDir(dir, context)
    }

}