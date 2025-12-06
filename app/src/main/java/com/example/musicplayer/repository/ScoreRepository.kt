package com.example.musicplayer.repository

import com.example.musicplayer.database.ScoreDao
import com.example.musicplayer.entity.ScoreEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 점수 Repository
 */
@Singleton
class ScoreRepository @Inject constructor(
    private val scoreDao: ScoreDao
) {

    /**
     * 점수 저장
     */
    suspend fun insertScore(score: ScoreEntity) {
        scoreDao.insertScore(score)
    }

    /**
     * 모든 점수 조회
     */
    suspend fun getAllScores(): List<ScoreEntity> {
        return scoreDao.getAllScores()
    }

    /**
     * 사용자별 점수 조회
     */
    suspend fun getScoresByUserId(userId: String): List<ScoreEntity> {
        return scoreDao.getScoresByUserId(userId)
    }

    /**
     * 월간 평균 점수
     */
    suspend fun getMonthlyAverageScore(userId: String, startOfMonth: Long, endOfMonth: Long): Double? {
        return scoreDao.getMonthlyAverageScore(userId, startOfMonth, endOfMonth)
    }

    /**
     * 월간 Top 3 점수
     */
    suspend fun getMonthlyTop3Scores(userId: String, startOfMonth: Long, endOfMonth: Long): List<ScoreEntity> {
        return scoreDao.getMonthlyTop3Scores(userId, startOfMonth, endOfMonth)
    }

    /**
     * 전체 기간 Top 3 점수
     */
    suspend fun getTop3Scores(userId: String): List<ScoreEntity> {
        return scoreDao.getTop3Scores(userId)
    }
}
