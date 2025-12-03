package com.example.musicplayer.fragment

sealed class RecordingSideEffect {
    data object ClearChart : RecordingSideEffect()
    data class ShowError(val message: String) : RecordingSideEffect()
}