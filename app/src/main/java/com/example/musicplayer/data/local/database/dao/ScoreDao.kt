package com.example.musicplayer.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.musicplayer.data.local.database.entity.ScoreEntity

@Dao
interface ScoreDao {
    @Insert
    suspend fun insertScore(score: ScoreEntity)

    @Query("SELECT * FROM scores ORDER BY timestamp DESC")
    suspend fun getAllScores(): List<ScoreEntity>

    @Query("SELECT * FROM scores WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getScoresByUserId(userId: String): List<ScoreEntity>

    @Query("SELECT AVG(score) FROM scores WHERE userId = :userId AND timestamp >= :startOfMonth AND timestamp < :endOfMonth")
    suspend fun getMonthlyAverageScore(userId: String, startOfMonth: Long, endOfMonth: Long): Double?

    @Query("SELECT * FROM scores WHERE userId = :userId AND timestamp >= :startOfMonth AND timestamp < :endOfMonth ORDER BY score DESC LIMIT 3")
    suspend fun getMonthlyTop3Scores(userId: String, startOfMonth: Long, endOfMonth: Long): List<ScoreEntity>

    @Query("SELECT * FROM scores WHERE userId = :userId ORDER BY score DESC LIMIT 3")
    suspend fun getTop3Scores(userId: String): List<ScoreEntity>
}
