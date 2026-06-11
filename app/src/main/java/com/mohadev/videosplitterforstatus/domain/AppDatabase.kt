package com.mohadev.videosplitterforstatus.domain

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SplitHistory::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
