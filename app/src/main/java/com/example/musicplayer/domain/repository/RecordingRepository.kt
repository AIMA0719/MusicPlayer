package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.RecordingHistory
import com.example.musicplayer.domain.model.ScoringDifficulty
import kotlinx.coroutines.flow.Flow

/**
 * 녹음 및 점수 기록 Repository 인터페이스
 */
interface RecordingRepository {
    /**
     * 모든 녹음 히스토리 조회
     */
    fun getAllRecordingHistory(userId: String): Flow<List<RecordingHistory>>

    /**
     * 최근 녹음 히스토리 조회
     */
    fun getRecentRecordingHistory(userId: String, limit: Int): Flow<List<RecordingHistory>>

    /**
     * 특정 곡의 녹음 히스토리 조회
     */
    fun getRecordingHistoryBySong(userId: String, songName: String): Flow<List<RecordingHistory>>

    /**
     * 녹음 히스토리 저장
     */
    suspend fun saveRecordingHistory(history: RecordingHistory): Long

    /**
     * 녹음 히스토리 삭제
     */
    suspend fun deleteRecordingHistory(id: Long)

    /**
     * 특정 곡의 최고 점수 조회
     */
    suspend fun getHighScore(userId: String, songName: String): Int?

    /**
     * 전체 녹음 횟수 조회
     */
    suspend fun getTotalRecordingCount(userId: String): Int

    /**
     * 평균 점수 조회
     */
    suspend fun getAverageScore(userId: String): Double

    /**
     * 난이도별 기록 조회
     */
    fun getRecordingsByDifficulty(userId: String, difficulty: ScoringDifficulty): Flow<List<RecordingHistory>>

    /**
     * 점수대별 분포 조회
     */
    suspend fun getScoreDistribution(userId: String): Map<String, Int>
}
