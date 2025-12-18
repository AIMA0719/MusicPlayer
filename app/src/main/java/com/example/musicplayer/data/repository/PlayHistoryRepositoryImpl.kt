package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.database.dao.HistoryDao
import com.example.musicplayer.data.local.database.entity.HistoryEntity
import com.example.musicplayer.domain.model.PlayHistory
import com.example.musicplayer.domain.repository.PlayHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayHistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : PlayHistoryRepository {

    override fun getAllHistory(): Flow<List<PlayHistory>> {
        return historyDao.getAllHistory().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRecentHistory(limit: Int): Flow<List<PlayHistory>> {
        return historyDao.getRecentHistory(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addHistory(history: PlayHistory) {
        historyDao.insertHistory(history.toEntity())
    }

    override suspend fun deleteHistory(id: Long) {
        historyDao.deleteHistoryById(id)
    }

    override suspend fun clearAllHistory() {
        historyDao.deleteAllHistory()
    }

    private fun HistoryEntity.toDomain(): PlayHistory {
        return PlayHistory(
            id = id,
            songId = songId,
            songName = title,
            artistName = artist,
            timestamp = timestamp
        )
    }

    private fun PlayHistory.toEntity(): HistoryEntity {
        return HistoryEntity(
            id = id,
            songId = songId,
            title = songName,
            artist = artistName,
            score = 0,
            recordingPath = "",
            timestamp = timestamp
        )
    }
}
