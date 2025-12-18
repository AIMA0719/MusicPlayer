package com.example.musicplayer.data.local.database.dao

import androidx.room.*
import com.example.musicplayer.data.local.database.entity.RecordingHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingHistoryDao {

    @Insert
    suspend fun insert(history: RecordingHistoryEntity): Long

    @Query("SELECT * FROM recording_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllByUser(userId: String): Flow<List<RecordingHistoryEntity>>

    @Query("SELECT * FROM recording_history WHERE userId = :userId AND songName = :songName ORDER BY timestamp DESC")
    fun getHistoryBySong(userId: String, songName: String): Flow<List<RecordingHistoryEntity>>

    @Query("SELECT * FROM recording_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(userId: String, limit: Int = 10): Flow<List<RecordingHistoryEntity>>

    @Query("SELECT * FROM recording_history WHERE id = :id")
    suspend fun getById(id: Long): RecordingHistoryEntity?

    @Query("DELETE FROM recording_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM recording_history WHERE userId = :userId")
    suspend fun getTotalRecordingCount(userId: String): Int

    @Query("SELECT AVG(totalScore) FROM recording_history WHERE userId = :userId")
    suspend fun getAverageScore(userId: String): Double

    @Query("SELECT MAX(totalScore) FROM recording_history WHERE userId = :userId")
    suspend fun getHighestScore(userId: String): Int

    @Query("""
        SELECT COUNT(DISTINCT DATE(timestamp / 1000, 'unixepoch'))
        FROM recording_history
        WHERE userId = :userId
        AND timestamp >= :startTime
    """)
    suspend fun getRecordingDaysInPeriod(userId: String, startTime: Long): Int

    @Query("""
        SELECT * FROM recording_history
        WHERE userId = :userId
        AND songName = :songName
        ORDER BY totalScore DESC
        LIMIT 1
    """)
    suspend fun getBestScoreBySong(userId: String, songName: String): RecordingHistoryEntity?

    @Query("""
        SELECT COUNT(*) FROM recording_history
        WHERE userId = :userId
        AND totalScore >= :minScore
    """)
    suspend fun getCountByMinScore(userId: String, minScore: Int): Int

    @Query("""
        SELECT COUNT(*) FROM recording_history
        WHERE userId = :userId
        AND totalScore >= :minScore
        AND totalScore < :maxScore
    """)
    suspend fun getCountByScoreRange(userId: String, minScore: Int, maxScore: Int): Int

    @Query("""
        SELECT * FROM recording_history
        WHERE userId = :userId
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getRecentRecordings(userId: String, limit: Int): List<RecordingHistoryEntity>

    @Query("""
        SELECT COUNT(DISTINCT songName) FROM recording_history
        WHERE userId = :userId
    """)
    suspend fun getUniqueSongCount(userId: String): Int

    @Query("""
        SELECT COUNT(*) FROM recording_history
        WHERE userId = :userId
        AND timestamp >= :startTime
        AND timestamp < :endTime
    """)
    suspend fun getRecordingCountInTimeRange(userId: String, startTime: Long, endTime: Long): Int

    @Query("""
        SELECT * FROM recording_history
        WHERE userId = :userId
        AND timestamp >= :startTime
        AND timestamp < :endTime
        ORDER BY timestamp
    """)
    suspend fun getRecordingsInTimeRange(userId: String, startTime: Long, endTime: Long): List<RecordingHistoryEntity>

    @Query("""
        SELECT COUNT(DISTINCT difficulty) FROM recording_history
        WHERE userId = :userId
        AND difficulty != 'NONE'
    """)
    suspend fun getTriedDifficultyCount(userId: String): Int
}
