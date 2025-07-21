package com.example.musicplayer.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.database.AppDatabase
import com.example.musicplayer.database.entity.Favorite
import com.example.musicplayer.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FavoriteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FavoriteRepository
    val allFavorites: Flow<List<Favorite>>

    init {
        val favoriteDao = AppDatabase.getDatabase(application).favoriteDao()
        repository = FavoriteRepository(favoriteDao)
        allFavorites = repository.allFavorites
    }

    fun insertFavorite(favorite: Favorite) = viewModelScope.launch {
        repository.insertFavorite(favorite)
    }

    fun deleteFavorite(favorite: Favorite) = viewModelScope.launch {
        repository.deleteFavorite(favorite)
    }

    fun deleteFavoriteById(songId: String) = viewModelScope.launch {
        repository.deleteFavoriteById(songId)
    }
} 