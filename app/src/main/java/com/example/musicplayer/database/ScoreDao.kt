package com.example.musicplayer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.musicplayer.entity.ScoreEntity

@Dao
interface ScoreDao {
    @Insert
    suspend fun insertScore(score: ScoreEntity)

    @Query("SELECT * FROM scores ORDER BY timestamp DESC")
    suspend fun getAllScores(): List<ScoreEntity>
}
