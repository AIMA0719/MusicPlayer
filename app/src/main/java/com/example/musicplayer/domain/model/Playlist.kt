package com.example.musicplayer.domain.model

/**
 * 재생목록 도메인 모델
 */
data class Playlist(
    val playlistId: Long = 0,
    val userId: String,
    val name: String,
    val description: String = "",
    val coverImagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val items: List<PlaylistItem> = emptyList()
) {
    val itemCount: Int get() = items.size
}

/**
 * 재생목록 아이템
 */
data class PlaylistItem(
    val id: Long = 0,
    val playlistId: Long,
    val songId: String,
    val songName: String,
    val artistName: String,
    val imageUrl: String = "",
    val audioUrl: String = "",
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)
