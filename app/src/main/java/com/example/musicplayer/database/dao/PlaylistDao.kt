package com.example.musicplayer.database.dao

import androidx.room.*
import com.example.musicplayer.database.entity.Playlist
import com.example.musicplayer.database.entity.PlaylistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    // Playlist CRUD
    @Query("SELECT * FROM playlists WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllPlaylists(userId: String): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    // PlaylistItem CRUD
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItem): Long

    @Delete
    suspend fun deletePlaylistItem(item: PlaylistItem)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteAllItemsInPlaylist(playlistId: Long)

    @Query("UPDATE playlist_items SET position = :newPosition WHERE id = :itemId")
    suspend fun updateItemPosition(itemId: Long, newPosition: Int)
}
