package com.mohadev.videosplitterforstatus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SplitHistory::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
