package com.mohadev.videosplitterforstatus.domain

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM split_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<SplitHistory>>

    @Insert
    suspend fun insert(history: SplitHistory): Long

    @Query("DELETE FROM split_history WHERE id = :id")
    suspend fun deleteById(id: Int)
}
