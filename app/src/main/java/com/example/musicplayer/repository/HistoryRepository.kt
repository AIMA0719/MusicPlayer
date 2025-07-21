package com.example.musicplayer.repository

import com.example.musicplayer.database.dao.HistoryDao
import com.example.musicplayer.database.entity.History
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<History>> = historyDao.getAllHistory()

    suspend fun getHistory(id: Long): History? {
        return historyDao.getHistory(id)
    }

    suspend fun insertHistory(history: History): Long {
        return historyDao.insertHistory(history)
    }

    suspend fun deleteHistory(history: History) {
        historyDao.deleteHistory(history)
    }

    suspend fun deleteHistoryById(id: Long) {
        historyDao.deleteHistoryById(id)
    }

    suspend fun deleteAllHistory() {
        historyDao.deleteAllHistory()
    }
} 