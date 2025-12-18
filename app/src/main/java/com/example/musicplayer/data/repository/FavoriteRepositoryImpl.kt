package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.database.dao.FavoriteDao
import com.example.musicplayer.data.local.database.entity.FavoriteEntity
import com.example.musicplayer.domain.model.FavoriteSong
import com.example.musicplayer.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {

    override fun getAllFavorites(): Flow<List<FavoriteSong>> {
        return favoriteDao.getAllFavorites().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun isFavorite(songId: String): Boolean {
        return favoriteDao.getFavorite(songId) != null
    }

    override suspend fun addFavorite(song: FavoriteSong) {
        favoriteDao.insertFavorite(song.toEntity())
    }

    override suspend fun removeFavorite(songId: String) {
        favoriteDao.deleteFavoriteById(songId)
    }

    override suspend fun toggleFavorite(song: FavoriteSong): Boolean {
        val existing = favoriteDao.getFavorite(song.songId)
        return if (existing != null) {
            favoriteDao.deleteFavoriteById(song.songId)
            false
        } else {
            favoriteDao.insertFavorite(song.toEntity())
            true
        }
    }

    private fun FavoriteEntity.toDomain(): FavoriteSong {
        return FavoriteSong(
            id = 0,
            songId = songId,
            songName = title,
            artistName = artist,
            timestamp = timestamp
        )
    }

    private fun FavoriteSong.toEntity(): FavoriteEntity {
        return FavoriteEntity(
            songId = songId,
            title = songName,
            artist = artistName,
            timestamp = timestamp
        )
    }
}
