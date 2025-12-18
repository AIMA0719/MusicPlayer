package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.PlaylistItem
import kotlinx.coroutines.flow.Flow

/**
 * 재생목록 Repository 인터페이스
 */
interface PlaylistRepository {
    /**
     * 사용자의 모든 재생목록 조회
     */
    fun getPlaylists(userId: String): Flow<List<Playlist>>

    /**
     * 특정 재생목록 조회
     */
    suspend fun getPlaylist(playlistId: Long): Playlist?

    /**
     * 재생목록 생성
     */
    suspend fun createPlaylist(playlist: Playlist): Long

    /**
     * 재생목록 업데이트
     */
    suspend fun updatePlaylist(playlist: Playlist)

    /**
     * 재생목록 삭제
     */
    suspend fun deletePlaylist(playlistId: Long)

    /**
     * 재생목록에 곡 추가
     */
    suspend fun addItemToPlaylist(playlistId: Long, item: PlaylistItem): Long

    /**
     * 재생목록에서 곡 삭제
     */
    suspend fun removeItemFromPlaylist(itemId: Long)

    /**
     * 재생목록 아이템 순서 변경
     */
    suspend fun updateItemPosition(itemId: Long, newPosition: Int)

    /**
     * 재생목록의 아이템 조회
     */
    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItem>>
}
