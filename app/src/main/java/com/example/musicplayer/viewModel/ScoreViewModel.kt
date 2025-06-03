package com.example.musicplayer.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.database.AppDatabase
import com.example.musicplayer.entity.ScoreEntity
import kotlinx.coroutines.launch

class ScoreViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val scoreDao = db.scoreDao()

    fun saveScore(songName: String, score: Int) {
        viewModelScope.launch {
            val scoreEntity = ScoreEntity(songName = songName, score = score)
            scoreDao.insertScore(scoreEntity)
        }
    }
}
