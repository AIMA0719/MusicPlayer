package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.database.dao.PlaylistDao
import com.example.musicplayer.data.local.database.entity.PlaylistEntity
import com.example.musicplayer.data.local.database.entity.PlaylistItemEntity
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.PlaylistItem
import com.example.musicplayer.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun getPlaylists(userId: String): Flow<List<Playlist>> {
        return playlistDao.getPlaylistsByUser(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getPlaylist(playlistId: Long): Playlist? {
        return playlistDao.getPlaylistById(playlistId)?.toDomain()
    }

    override suspend fun createPlaylist(playlist: Playlist): Long {
        return playlistDao.insertPlaylist(playlist.toEntity())
    }

    override suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist.toEntity())
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        val playlist = playlistDao.getPlaylistById(playlistId)
        if (playlist != null) {
            playlistDao.deletePlaylist(playlist)
        }
    }

    override suspend fun addItemToPlaylist(playlistId: Long, item: PlaylistItem): Long {
        return playlistDao.insertPlaylistItem(item.toEntity())
    }

    override suspend fun removeItemFromPlaylist(itemId: Long) {
        playlistDao.deletePlaylistItemById(itemId)
    }

    override suspend fun updateItemPosition(itemId: Long, newPosition: Int) {
        playlistDao.updateItemPosition(itemId, newPosition)
    }

    override fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItem>> {
        return playlistDao.getPlaylistItems(playlistId).map { list ->
            list.map { it.toDomain() }
        }
    }

    private fun PlaylistEntity.toDomain(): Playlist {
        return Playlist(
            playlistId = playlistId,
            userId = userId,
            name = name,
            description = description ?: "",
            coverImagePath = coverImageUrl,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Playlist.toEntity(): PlaylistEntity {
        return PlaylistEntity(
            playlistId = playlistId,
            userId = userId,
            name = name,
            description = description,
            coverImageUrl = coverImagePath,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun PlaylistItemEntity.toDomain(): PlaylistItem {
        return PlaylistItem(
            id = id,
            playlistId = playlistId,
            songId = musicUri,
            songName = musicTitle,
            artistName = musicArtist,
            position = position,
            addedAt = addedAt
        )
    }

    private fun PlaylistItem.toEntity(): PlaylistItemEntity {
        return PlaylistItemEntity(
            id = id,
            playlistId = playlistId,
            musicUri = songId,
            musicTitle = songName,
            musicArtist = artistName,
            position = position,
            addedAt = addedAt
        )
    }
}
