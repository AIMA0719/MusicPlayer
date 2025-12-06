package com.example.musicplayer.repository

import com.example.musicplayer.database.dao.RecordingHistoryDao
import com.example.musicplayer.entity.RecordingHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 녹음 기록 Repository
 * - 녹음 기록 데이터 접근 계층
 */
@Singleton
class RecordingHistoryRepository @Inject constructor(
    private val recordingHistoryDao: RecordingHistoryDao
) {

    /**
     * 녹음 기록 저장
     */
    suspend fun insert(history: RecordingHistoryEntity): Long {
        return recordingHistoryDao.insert(history)
    }

    /**
     * 사용자의 모든 녹음 기록 조회
     */
    fun getAllByUser(userId: String): Flow<List<RecordingHistoryEntity>> {
        return recordingHistoryDao.getAllByUser(userId)
    }

    /**
     * 곡별 녹음 기록 조회
     */
    fun getHistoryBySong(userId: String, songName: String): Flow<List<RecordingHistoryEntity>> {
        return recordingHistoryDao.getHistoryBySong(userId, songName)
    }

    /**
     * 최근 녹음 기록 조회
     */
    fun getRecentHistory(userId: String, limit: Int = 10): Flow<List<RecordingHistoryEntity>> {
        return recordingHistoryDao.getRecentHistory(userId, limit)
    }

    /**
     * 특정 녹음 기록 조회
     */
    suspend fun getById(id: Long): RecordingHistoryEntity? {
        return recordingHistoryDao.getById(id)
    }

    /**
     * 녹음 기록 삭제
     */
    suspend fun deleteById(id: Long) {
        recordingHistoryDao.deleteById(id)
    }

    /**
     * 총 녹음 횟수
     */
    suspend fun getTotalRecordingCount(userId: String): Int {
        return recordingHistoryDao.getTotalRecordingCount(userId)
    }

    /**
     * 평균 점수
     */
    suspend fun getAverageScore(userId: String): Double {
        return recordingHistoryDao.getAverageScore(userId)
    }

    /**
     * 최고 점수
     */
    suspend fun getHighestScore(userId: String): Int {
        return recordingHistoryDao.getHighestScore(userId)
    }

    /**
     * 특정 기간 동안의 녹음 일수
     */
    suspend fun getRecordingDaysInPeriod(userId: String, startTime: Long): Int {
        return recordingHistoryDao.getRecordingDaysInPeriod(userId, startTime)
    }

    /**
     * 곡별 최고 점수
     */
    suspend fun getBestScoreBySong(userId: String, songName: String): RecordingHistoryEntity? {
        return recordingHistoryDao.getBestScoreBySong(userId, songName)
    }

    /**
     * 최소 점수 이상 개수
     */
    suspend fun getCountByMinScore(userId: String, minScore: Int): Int {
        return recordingHistoryDao.getCountByMinScore(userId, minScore)
    }

    /**
     * 점수 범위별 개수
     */
    suspend fun getCountByScoreRange(userId: String, minScore: Int, maxScore: Int): Int {
        return recordingHistoryDao.getCountByScoreRange(userId, minScore, maxScore)
    }

    /**
     * 최근 N개 녹음 기록
     */
    suspend fun getRecentRecordings(userId: String, limit: Int): List<RecordingHistoryEntity> {
        return recordingHistoryDao.getRecentRecordings(userId, limit)
    }

    /**
     * 고유 곡 개수
     */
    suspend fun getUniqueSongCount(userId: String): Int {
        return recordingHistoryDao.getUniqueSongCount(userId)
    }

    /**
     * 시간 범위 내 녹음 개수
     */
    suspend fun getRecordingCountInTimeRange(userId: String, startTime: Long, endTime: Long): Int {
        return recordingHistoryDao.getRecordingCountInTimeRange(userId, startTime, endTime)
    }

    /**
     * 시간 범위 내 녹음 기록
     */
    suspend fun getRecordingsInTimeRange(userId: String, startTime: Long, endTime: Long): List<RecordingHistoryEntity> {
        return recordingHistoryDao.getRecordingsInTimeRange(userId, startTime, endTime)
    }

    /**
     * 시도한 난이도 개수
     */
    suspend fun getTriedDifficultyCount(userId: String): Int {
        return recordingHistoryDao.getTriedDifficultyCount(userId)
    }

    /**
     * 통계 정보 (한번에 조회)
     */
    data class Statistics(
        val totalCount: Int,
        val averageScore: Double,
        val highestScore: Int,
        val uniqueSongCount: Int
    )

    suspend fun getStatistics(userId: String): Statistics {
        return Statistics(
            totalCount = getTotalRecordingCount(userId),
            averageScore = getAverageScore(userId),
            highestScore = getHighestScore(userId),
            uniqueSongCount = getUniqueSongCount(userId)
        )
    }
}
