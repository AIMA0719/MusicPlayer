package com.example.musicplayer.database.dao

import androidx.room.*
import com.example.musicplayer.database.entity.History
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<History>>

    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun getHistory(id: Long): History?

    @Insert
    suspend fun insertHistory(history: History): Long

    @Delete
    suspend fun deleteHistory(history: History)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history")
    suspend fun deleteAllHistory()
} 