package com.example.musicplayer.domain.model

import android.net.Uri

/**
 * 음악 트랙 도메인 모델
 */
data class MusicTrack(
    val id: String,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: Long,
    val albumArtUri: Uri? = null,
    val isDownloaded: Boolean = false,
    val downloadPath: String? = null
)

/**
 * Jamendo API 응답 트랙
 */
data class JamendoTrack(
    val id: String,
    val name: String,
    val artistName: String,
    val albumName: String,
    val duration: Int,
    val audioUrl: String,
    val imageUrl: String
)

/**
 * 즐겨찾기 곡
 */
data class FavoriteSong(
    val id: Long = 0,
    val songId: String,
    val songName: String,
    val artistName: String,
    val albumName: String = "",
    val imageUrl: String = "",
    val audioUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 재생 히스토리 곡
 */
data class PlayHistory(
    val id: Long = 0,
    val songId: String,
    val songName: String,
    val artistName: String,
    val imageUrl: String = "",
    val audioUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
