package com.mohadev.videosplitterforstatus.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.mohadev.videosplitterforstatus.domain.model.VideoItem
import com.mohadev.videosplitterforstatus.domain.usecase.GetGroupedVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val getGroupedVideosUseCase: GetGroupedVideosUseCase
) : ViewModel() {

    private val _groupedVideos = mutableStateOf<Map<String, List<VideoItem>>>(emptyMap())
    val groupedVideos: State<Map<String, List<VideoItem>>> = _groupedVideos

    fun load(dir: String, context: Context) {
        _groupedVideos.value = getGroupedVideosUseCase(dir, context)
    }
}
