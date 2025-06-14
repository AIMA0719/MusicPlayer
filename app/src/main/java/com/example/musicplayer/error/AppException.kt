package com.example.musicplayer.error

sealed class AppException : Exception() {
    data class AudioRecordingException(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppException()

    data class ScoreCalculationException(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppException()

    data class ResourceInitializationException(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppException()

    data class InvalidStateException(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppException()
} 