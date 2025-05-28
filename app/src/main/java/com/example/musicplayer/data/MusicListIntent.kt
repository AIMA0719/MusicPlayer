package com.example.musicplayer.data

sealed interface MusicListIntent {
    data object LoadMusicFiles : MusicListIntent
    data class AnalyzeOriginalMusic(val music: MusicFile) : MusicListIntent
    data class AnalysisCompleted(val originalPitch: List<Float>, val music: MusicFile) : MusicListIntent
}

