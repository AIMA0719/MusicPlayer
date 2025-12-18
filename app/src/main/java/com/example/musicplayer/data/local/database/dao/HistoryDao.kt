package com.example.musicplayer.data.local.database.dao

import androidx.room.*
import com.example.musicplayer.data.local.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE songId = :songId")
    suspend fun getHistory(songId: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Delete
    suspend fun deleteHistory(history: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history")
    suspend fun deleteAllHistory()

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<HistoryEntity>>
}
