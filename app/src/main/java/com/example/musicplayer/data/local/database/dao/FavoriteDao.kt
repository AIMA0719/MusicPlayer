package com.example.musicplayer.data.local.database.dao

import androidx.room.*
import com.example.musicplayer.data.local.database.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE songId = :songId")
    suspend fun getFavorite(songId: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun deleteFavoriteById(songId: String)
}
