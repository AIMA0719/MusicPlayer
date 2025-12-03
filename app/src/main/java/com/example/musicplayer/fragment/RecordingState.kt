package com.example.musicplayer.fragment

data class RecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedTime: Long = 0,
    val currentPitch: Float = 0f,
    val pitchDifference: Float = 0f,
    val amplitude: Int = 0,
    val currentScore: Int = 0,
    val accuracy: Float = 0f,
    val score: Int? = null,
)