package com.example.musicplayer.database.dao

import androidx.room.*
import com.example.musicplayer.database.entity.Favorite
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<Favorite>>

    @Query("SELECT * FROM favorites WHERE songId = :songId")
    suspend fun getFavorite(songId: String): Favorite?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)

    @Delete
    suspend fun deleteFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun deleteFavoriteById(songId: String)
} 