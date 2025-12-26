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

    @Query("SELECT * FROM scores WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getScoresByUserId(userId: String): List<ScoreEntity>

    /**
     * 현재 월의 평균 점수 조회
     * @param userId 사용자 ID
     * @param startOfMonth 월 시작 타임스탬프
     * @param endOfMonth 월 종료 타임스탬프
     */
    @Query("SELECT AVG(score) FROM scores WHERE userId = :userId AND timestamp >= :startOfMonth AND timestamp < :endOfMonth")
    suspend fun getMonthlyAverageScore(userId: String, startOfMonth: Long, endOfMonth: Long): Double?

    /**
     * 현재 월의 Top 3 점수 조회
     * @param userId 사용자 ID
     * @param startOfMonth 월 시작 타임스탬프
     * @param endOfMonth 월 종료 타임스탬프
     */
    @Query("SELECT * FROM scores WHERE userId = :userId AND timestamp >= :startOfMonth AND timestamp < :endOfMonth ORDER BY score DESC LIMIT 3")
    suspend fun getMonthlyTop3Scores(userId: String, startOfMonth: Long, endOfMonth: Long): List<ScoreEntity>

    /**
     * 전체 기간의 Top 3 점수 조회
     * @param userId 사용자 ID
     */
    @Query("SELECT * FROM scores WHERE userId = :userId ORDER BY score DESC LIMIT 3")
    suspend fun getTop3Scores(userId: String): List<ScoreEntity>

    /**
     * 기간 내 최고 점수 조회
     */
    @Query("SELECT MAX(score) FROM scores WHERE userId = :userId AND timestamp >= :startTime AND timestamp < :endTime")
    suspend fun getBestScoreByDateRange(userId: String, startTime: Long, endTime: Long): Int?

    /**
     * 기간 내 평균 점수 조회
     */
    @Query("SELECT AVG(score) FROM scores WHERE userId = :userId AND timestamp >= :startTime AND timestamp < :endTime")
    suspend fun getAverageScoreByDateRange(userId: String, startTime: Long, endTime: Long): Double?

    /**
     * 전체 기간 최고 점수 조회
     */
    @Query("SELECT MAX(score) FROM scores WHERE userId = :userId")
    suspend fun getHighestScore(userId: String): Int?
}
