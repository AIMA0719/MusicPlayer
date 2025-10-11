package com.example.musicplayer.data

data class MusicDownloadItem(
    val id: String,
    val title: String,
    val artist: String,
    val url: String,
    val duration: String,
    val size: String,
    val genre: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val imageUrl: String? = null,
    val albumName: String? = null
)