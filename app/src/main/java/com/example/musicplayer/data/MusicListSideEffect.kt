package com.example.musicplayer.data

sealed interface MusicListSideEffect {
    // Side effects는 필요시 추가 (현재는 상태로 모든 것 처리 가능)
    // 예: data class ShowError(val message: String) : MusicListSideEffect
}
