package com.example.musicplayer.data

sealed interface MusicListIntent {
    data object LoadMusicFiles : MusicListIntent // 페이지 진입 시 음악 파일을 불러오는 Intent
    data class AnalyzeOriginalMusic(val music: MusicFile) : MusicListIntent // 원본 음악 파일의 피치를 분석하는 Intent
    data class AnalysisCompleted(val originalPitch: List<Float>, val music: MusicFile) : MusicListIntent // 피치 분석이 완료되었을 때 호출되는 Intent
}

