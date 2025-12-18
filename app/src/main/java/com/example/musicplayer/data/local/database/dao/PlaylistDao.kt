package com.example.musicplayer.data.local.database.dao

import androidx.room.*
import com.example.musicplayer.data.local.database.entity.PlaylistEntity
import com.example.musicplayer.data.local.database.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    // Playlist CRUD
    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getPlaylistsByUser(userId: String): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    // PlaylistItem CRUD
    @Insert
    suspend fun insertPlaylistItem(item: PlaylistItemEntity): Long

    @Delete
    suspend fun deletePlaylistItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE id = :itemId")
    suspend fun deletePlaylistItemById(itemId: Long)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position")
    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Query("UPDATE playlist_items SET position = :position WHERE id = :itemId")
    suspend fun updateItemPosition(itemId: Long, position: Int)

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getPlaylistItemCount(playlistId: Long): Int
}
