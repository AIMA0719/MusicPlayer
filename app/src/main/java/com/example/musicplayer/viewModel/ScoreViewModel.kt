package com.example.musicplayer.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.database.AppDatabase
import com.example.musicplayer.entity.ScoreEntity
import com.example.musicplayer.manager.AuthManager
import kotlinx.coroutines.launch

class ScoreViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val scoreDao = db.scoreDao()

    fun saveScore(songName: String, score: Int, songArtist: String = "") {
        viewModelScope.launch {
            val userId = AuthManager.getCurrentUserId() ?: "guest"
            val scoreEntity = ScoreEntity(
                userId = userId,
                songName = songName,
                songArtist = songArtist,
                score = score
            )
            scoreDao.insertScore(scoreEntity)
        }
    }
}
