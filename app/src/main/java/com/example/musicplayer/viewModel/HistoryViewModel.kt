package com.example.musicplayer.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.database.AppDatabase
import com.example.musicplayer.database.entity.History
import com.example.musicplayer.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HistoryRepository
    val allHistory: Flow<List<History>>

    init {
        val historyDao = AppDatabase.getDatabase(application).historyDao()
        repository = HistoryRepository(historyDao)
        allHistory = repository.allHistory
    }

    fun insertHistory(history: History) = viewModelScope.launch {
        repository.insertHistory(history)
    }

    fun deleteHistory(history: History) = viewModelScope.launch {
        repository.deleteHistory(history)
    }

    fun deleteHistoryById(id: Long) = viewModelScope.launch {
        repository.deleteHistoryById(id)
    }

    fun deleteAllHistory() = viewModelScope.launch {
        repository.deleteAllHistory()
    }
} 