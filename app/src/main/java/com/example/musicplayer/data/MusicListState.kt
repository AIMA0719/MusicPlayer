package com.example.musicplayer.data

data class MusicListState(
    val musicFiles: List<MusicFile> = emptyList(),
    val selectedMusic: MusicFile? = null,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val originalPitch: List<Float>? = null
)


