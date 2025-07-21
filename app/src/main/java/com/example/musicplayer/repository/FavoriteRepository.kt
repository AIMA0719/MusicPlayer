package com.example.musicplayer.repository

import com.example.musicplayer.database.dao.FavoriteDao
import com.example.musicplayer.database.entity.Favorite
import kotlinx.coroutines.flow.Flow

class FavoriteRepository(private val favoriteDao: FavoriteDao) {
    val allFavorites: Flow<List<Favorite>> = favoriteDao.getAllFavorites()

    suspend fun getFavorite(songId: String): Favorite? {
        return favoriteDao.getFavorite(songId)
    }

    suspend fun insertFavorite(favorite: Favorite) {
        favoriteDao.insertFavorite(favorite)
    }

    suspend fun deleteFavorite(favorite: Favorite) {
        favoriteDao.deleteFavorite(favorite)
    }

    suspend fun deleteFavoriteById(songId: String) {
        favoriteDao.deleteFavoriteById(songId)
    }
} 