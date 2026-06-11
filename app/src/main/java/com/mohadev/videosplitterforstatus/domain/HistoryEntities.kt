package com.mohadev.videosplitterforstatus.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "split_history")
data class SplitHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val originalVideoUri: String,
    val videoName: String,
    val timestamp: Long,
    val outputFolderPath: String,
    val segmentDuration: Int
)
