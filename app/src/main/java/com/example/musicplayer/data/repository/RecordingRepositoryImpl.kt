package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.database.dao.RecordingHistoryDao
import com.example.musicplayer.data.local.database.entity.RecordingHistoryEntity
import com.example.musicplayer.domain.model.RecordingHistory
import com.example.musicplayer.domain.model.ScoringDifficulty
import com.example.musicplayer.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val recordingHistoryDao: RecordingHistoryDao
) : RecordingRepository {

    override fun getAllRecordingHistory(userId: String): Flow<List<RecordingHistory>> {
        return recordingHistoryDao.getAllByUser(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRecentRecordingHistory(userId: String, limit: Int): Flow<List<RecordingHistory>> {
        return recordingHistoryDao.getRecentHistory(userId, limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRecordingHistoryBySong(userId: String, songName: String): Flow<List<RecordingHistory>> {
        return recordingHistoryDao.getHistoryBySong(userId, songName).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveRecordingHistory(history: RecordingHistory): Long {
        return recordingHistoryDao.insert(history.toEntity())
    }

    override suspend fun deleteRecordingHistory(id: Long) {
        recordingHistoryDao.deleteById(id)
    }

    override suspend fun getHighScore(userId: String, songName: String): Int? {
        return recordingHistoryDao.getBestScoreBySong(userId, songName)?.totalScore
    }

    override suspend fun getTotalRecordingCount(userId: String): Int {
        return recordingHistoryDao.getTotalRecordingCount(userId)
    }

    override suspend fun getAverageScore(userId: String): Double {
        return recordingHistoryDao.getAverageScore(userId)
    }

    override fun getRecordingsByDifficulty(
        userId: String,
        difficulty: ScoringDifficulty
    ): Flow<List<RecordingHistory>> {
        return recordingHistoryDao.getAllByUser(userId).map { list ->
            list.filter { it.difficulty == difficulty.name }.map { it.toDomain() }
        }
    }

    override suspend fun getScoreDistribution(userId: String): Map<String, Int> {
        return mapOf(
            "0-59" to recordingHistoryDao.getCountByScoreRange(userId, 0, 60),
            "60-69" to recordingHistoryDao.getCountByScoreRange(userId, 60, 70),
            "70-79" to recordingHistoryDao.getCountByScoreRange(userId, 70, 80),
            "80-89" to recordingHistoryDao.getCountByScoreRange(userId, 80, 90),
            "90-99" to recordingHistoryDao.getCountByScoreRange(userId, 90, 100),
            "100" to recordingHistoryDao.getCountByScoreRange(userId, 100, 101)
        )
    }

    private fun RecordingHistoryEntity.toDomain(): RecordingHistory {
        return RecordingHistory(
            id = id,
            userId = userId,
            songName = songName,
            songArtist = songArtist,
            songDuration = songDuration,
            totalScore = totalScore,
            pitchAccuracy = pitchAccuracy,
            rhythmScore = rhythmScore,
            volumeStability = volumeStability,
            durationMatch = durationMatch,
            hasVibrato = hasVibrato,
            vibratoScore = vibratoScore,
            difficulty = ScoringDifficulty.fromString(difficulty),
            recordingFilePath = recordingFilePath,
            timestamp = timestamp
        )
    }

    private fun RecordingHistory.toEntity(): RecordingHistoryEntity {
        return RecordingHistoryEntity(
            id = id,
            userId = userId,
            songName = songName,
            songArtist = songArtist,
            songDuration = songDuration,
            totalScore = totalScore,
            pitchAccuracy = pitchAccuracy,
            rhythmScore = rhythmScore,
            volumeStability = volumeStability,
            durationMatch = durationMatch,
            hasVibrato = hasVibrato,
            vibratoScore = vibratoScore,
            difficulty = difficulty.name,
            recordingFilePath = recordingFilePath,
            timestamp = timestamp
        )
    }
}
